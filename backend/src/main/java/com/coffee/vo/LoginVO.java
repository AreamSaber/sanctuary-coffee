package com.coffee.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO
 * 
 * @author Coffee Shop Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    
    /**
     * Token
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserVO userInfo;
}
