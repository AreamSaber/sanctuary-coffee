package com.coffee.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis容错服务
 * 当Redis不可用时，使用本地缓存作为后备方案
 */
@Slf4j
@Service
public class RedisFallbackService {
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 本地缓存作为后备方案
    private final ConcurrentHashMap<String, Object> localCache = new ConcurrentHashMap<>();
    
    /**
     * 设置缓存值
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
            } else {
                // Redis不可用，使用本地缓存
                localCache.put(key, value);
                log.warn("Redis不可用，使用本地缓存存储: {}", key);
            }
        } catch (Exception e) {
            // Redis操作失败，降级到本地缓存
            localCache.put(key, value);
            log.error("Redis操作失败，降级到本地缓存: {}", e.getMessage());
        }
    }
    
    /**
     * 获取缓存值
     */
    public Object get(String key) {
        try {
            if (redisTemplate != null) {
                return redisTemplate.opsForValue().get(key);
            } else {
                // Redis不可用，从本地缓存获取
                return localCache.get(key);
            }
        } catch (Exception e) {
            // Redis操作失败，从本地缓存获取
            log.error("Redis操作失败，从本地缓存获取: {}", e.getMessage());
            return localCache.get(key);
        }
    }
    
    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        try {
            if (redisTemplate != null) {
                return redisTemplate.delete(key);
            } else {
                // Redis不可用，从本地缓存删除
                localCache.remove(key);
                return true;
            }
        } catch (Exception e) {
            // Redis操作失败，从本地缓存删除
            log.error("Redis操作失败，从本地缓存删除: {}", e.getMessage());
            localCache.remove(key);
            return true;
        }
    }
    
    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        try {
            if (redisTemplate != null) {
                return redisTemplate.hasKey(key);
            } else {
                return localCache.containsKey(key);
            }
        } catch (Exception e) {
            log.error("Redis操作失败，检查本地缓存: {}", e.getMessage());
            return localCache.containsKey(key);
        }
    }
    
    /**
     * 检查Redis是否可用
     */
    public boolean isRedisAvailable() {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().get("ping");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
