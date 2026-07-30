package com.coffee.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 自定义UserDetails实现，包含用户ID信息
 */
@Getter
public class JwtUserDetails extends User {
    
    private final Long userId;
    private final String nickname;
    private final String email;
    
    public JwtUserDetails(Long userId, String username, String password,
                          String nickname, String email,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
    }
    
    public JwtUserDetails(Long userId, String username, String password, 
                          String nickname, String email,
                          boolean enabled, boolean accountNonExpired,
                          boolean credentialsNonExpired, boolean accountNonLocked,
                          Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
    }
}
