package com.coffee.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String encodedPassword = encoder.encode(password);
        System.out.println("原密码: " + password);
        System.out.println("加密后: " + encodedPassword);
        
        // 验证
        boolean matches = encoder.matches(password, encodedPassword);
        System.out.println("验证结果: " + matches);
    }
}
