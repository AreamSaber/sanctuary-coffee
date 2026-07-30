package com.coffee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

/**
 * 用户更新DTO
 * 
 * @author Coffee Shop Team
 */
@Data
public class UserUpdateDTO {
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 性别 0未知 1男 2女
     */
    private Integer gender;
    
    /**
     * 生日
     */
    private LocalDate birthday;
}
