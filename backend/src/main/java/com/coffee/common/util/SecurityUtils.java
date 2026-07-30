package com.coffee.common.util;

import com.coffee.security.JwtUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 工具类
 * 用于获取当前登录用户信息
 */
public class SecurityUtils {
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            // 如果是JwtUserDetails对象，从中获取用户ID
            if (principal instanceof JwtUserDetails) {
                JwtUserDetails userDetails = (JwtUserDetails) principal;
                return userDetails.getUserId();
            }
            
            // 如果是用户名字符串（通常是JWT解析后的username）
            if (principal instanceof String) {
                // 这里可以根据需要从数据库查询用户ID
                // 暂时返回null，需要在JwtAuthenticationFilter中完善
                return null;
            }
        }
        
        // 未登录或无法获取用户信息时返回null
        return null;
    }
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof JwtUserDetails) {
                JwtUserDetails userDetails = (JwtUserDetails) principal;
                return userDetails.getUsername();
            }
            
            if (principal instanceof String) {
                return (String) principal;
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * 检查当前用户是否拥有指定权限
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }

    /**
     * 检查当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }
}
