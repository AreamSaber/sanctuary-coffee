package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.UserUpdateDTO;
import com.coffee.entity.User;
import com.coffee.mapper.PermissionMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.service.UserService;
import com.coffee.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        
        // 查询用户角色
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        userVO.setRoles(roles);
        List<String> permissionCodes = permissionMapper.selectPermissionCodesByUserId(userId);
        userVO.setPermissionCodes(shouldUseRoleFallback(roles, permissionCodes) ? null : permissionCodes);
        
        return userVO;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateDTO updateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 更新用户信息
        BeanUtil.copyProperties(updateDTO, user, "id", "username", "password");
        userMapper.updateById(user);
        
        log.info("用户信息更新成功: userId={}", userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        
        log.info("用户密码修改成功: userId={}", userId);
    }

    private boolean shouldUseRoleFallback(List<String> roles, List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return true;
        }
        if (roles.contains("ROLE_ADMIN")) {
            return !permissionCodes.contains("admin:workbench:view");
        }
        if (roles.contains("ROLE_USER")) {
            return !permissionCodes.contains("order:member");
        }
        if (roles.contains("ROLE_DELIVERY")) {
            return !permissionCodes.contains("delivery:tracking");
        }
        return false;
    }
}
