package com.coffee.service;

import com.coffee.dto.LoginDTO;
import com.coffee.dto.PasswordInitDTO;
import com.coffee.dto.RegisterDTO;
import com.coffee.vo.LoginVO;
import com.coffee.vo.PasswordInitResultVO;
import com.coffee.vo.PasswordInitStatusVO;

/**
 * 认证服务接口
 * 
 * @author Coffee Shop Team
 */
public interface AuthService {
    
    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);
    
    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 查询是否存在待初始化密码的测试账号。
     */
    PasswordInitStatusVO getPasswordInitStatus();

    /**
     * 为所有空密码测试账号统一初始化密码。
     */
    PasswordInitResultVO initializeBlankPasswords(PasswordInitDTO passwordInitDTO);
    
    /**
     * 退出登录
     */
    void logout();
}
