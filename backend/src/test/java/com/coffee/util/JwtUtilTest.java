package com.coffee.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @Test
    void generateTokenUsesConfiguredSecretWithEnoughEntropy() {
        JwtUtil jwtUtil = jwtUtil("coffee-shop-demo-jwt-secret-2026-change-before-production");

        String token = jwtUtil.generateToken(1L, "admin");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals(1L, jwtUtil.getUserIdFromToken(token));
        assertEquals("admin", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void initSecretKeyRejectsShortJwtSecret() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "change-me-in-production");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, jwtUtil::initSecretKey);

        assertTrue(exception.getMessage().contains("至少需要 32 字节"));
    }

    private JwtUtil jwtUtil(String secret) {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        jwtUtil.initSecretKey();
        return jwtUtil;
    }
}
