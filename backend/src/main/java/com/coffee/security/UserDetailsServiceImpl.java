package com.coffee.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.entity.User;
import com.coffee.mapper.PermissionMapper;
import com.coffee.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsService实现类
 * 
 * @author Coffee Shop Team
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 查询用户角色
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissionCodes = permissionMapper.selectPermissionCodesByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = java.util.stream.Stream.concat(roleCodes.stream(), permissionCodes.stream())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        // 构建自定义UserDetails，包含用户ID
        return new JwtUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus() == 1,  // enabled
                true,                    // accountNonExpired
                true,                    // credentialsNonExpired
                user.getStatus() == 1,   // accountNonLocked
                authorities
        );
    }
}
