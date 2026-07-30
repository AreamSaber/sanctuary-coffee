package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.config.RedisFallbackService;
import com.coffee.dto.LoginDTO;
import com.coffee.dto.PasswordInitDTO;
import com.coffee.dto.RegisterDTO;
import com.coffee.entity.User;
import com.coffee.mapper.UserMapper;
import com.coffee.security.JwtUserDetails;
import com.coffee.service.AuthService;
import com.coffee.service.MemberService;
import com.coffee.service.UserService;
import com.coffee.util.JwtUtil;
import com.coffee.vo.LoginVO;
import com.coffee.vo.PasswordInitResultVO;
import com.coffee.vo.PasswordInitStatusVO;
import com.coffee.vo.PasswordInitUserVO;
import com.coffee.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_PREFIX = "token:";
    private static final String DEFAULT_ROLE_CODE = "ROLE_USER";

    private final UserMapper userMapper;
    private final UserService userService;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisFallbackService redisFallbackService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        String email = normalizeOptionalValue(registerDTO.getEmail());
        String phone = normalizeOptionalValue(registerDTO.getPhone());
        String nickname = normalizeOptionalValue(registerDTO.getNickname());

        User existUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername())
        );
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
        }

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }

        if (email != null) {
            User emailUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)
            );
            if (emailUser != null) {
                throw new BusinessException("该邮箱已被使用");
            }
        }

        if (phone != null) {
            User phoneUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, phone)
            );
            if (phoneUser != null) {
                throw new BusinessException("该手机号已被使用");
            }
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(nickname != null ? nickname : registerDTO.getUsername());
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);

        userMapper.insert(user);
        assignDefaultRole(user.getId());
        memberService.initMemberInfo(user.getId());
        log.info("用户注册成功: {}", user.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
        );

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("当前账号尚未初始化密码，请先完成初始密码设置");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDTO.getUsername(),
                    loginDTO.getPassword()
                )
            );
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String key = TOKEN_PREFIX + user.getId();
        redisFallbackService.set(key, token, 24, TimeUnit.HOURS);

        UserVO userVO = userService.getUserInfo(user.getId());
        log.info("用户登录成功: {}", user.getUsername());
        return new LoginVO(token, userVO);
    }

    @Override
    public PasswordInitStatusVO getPasswordInitStatus() {
        List<PasswordInitUserVO> users = userMapper.selectUsersWithBlankPassword().stream()
            .map(user -> new PasswordInitUserVO(user.getId(), user.getUsername(), user.getNickname()))
            .toList();

        return new PasswordInitStatusVO(!users.isEmpty(), users.size(), users);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordInitResultVO initializeBlankPasswords(PasswordInitDTO passwordInitDTO) {
        if (!passwordInitDTO.getPassword().equals(passwordInitDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的初始化密码不一致");
        }

        List<User> pendingUsers = userMapper.selectUsersWithBlankPassword();
        if (pendingUsers.isEmpty()) {
            return new PasswordInitResultVO(0, List.of());
        }

        int updatedCount = userMapper.initializeBlankPasswords(passwordEncoder.encode(passwordInitDTO.getPassword()));
        List<String> usernames = pendingUsers.stream()
            .map(User::getUsername)
            .toList();

        log.info("初始化测试账号密码完成: updatedCount={}, usernames={}", updatedCount, usernames);
        return new PasswordInitResultVO(updatedCount, usernames);
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = null;
            Long userId = null;

            if (authentication.getPrincipal() instanceof JwtUserDetails userDetails) {
                username = userDetails.getUsername();
                userId = userDetails.getUserId();
            } else if (authentication.getPrincipal() instanceof String principal) {
                username = principal;
            }

            if (userId != null) {
                try {
                    String key = TOKEN_PREFIX + userId;
                    redisFallbackService.delete(key);
                    log.info("用户 {} (ID: {}) 退出登录", username, userId);
                } catch (Exception e) {
                    log.error("清理Token失败: {}", e.getMessage());
                }
            }

            SecurityContextHolder.clearContext();
        }
    }

    private void assignDefaultRole(Long userId) {
        Long roleId = userMapper.selectRoleIdByCode(DEFAULT_ROLE_CODE);
        if (roleId == null) {
            throw new BusinessException("默认用户角色不存在");
        }
        userMapper.insertUserRole(userId, roleId);
    }

    private String normalizeOptionalValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
