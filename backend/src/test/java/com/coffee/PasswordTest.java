package com.coffee;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    
    @Test
    public void generatePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String encodedPassword = encoder.encode(password);
        System.out.println("原密码: " + password);
        System.out.println("加密后: " + encodedPassword);
        System.out.println("长度: " + encodedPassword.length());
        
        // 验证
        boolean matches = encoder.matches(password, encodedPassword);
        System.out.println("验证结果: " + matches);
    }
}
