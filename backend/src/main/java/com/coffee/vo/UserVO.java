package com.coffee.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class UserVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 性别
     */
    private Integer gender;
    
    /**
     * 生日
     */
    private LocalDate birthday;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限码列表
     */
    private List<String> permissionCodes;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
