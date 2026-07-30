package com.coffee.common.controller;

import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;

/**
 * 基础控制器，提供公共方法
 */
public abstract class BaseController {
    
    /**
     * 获取当前登录用户ID
     */
    protected Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
    
    /**
     * 获取当前登录用户名
     */
    protected String getCurrentUsername() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return username;
    }
}
