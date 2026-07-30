package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 批量初始化测试账号密码请求。
 */
@Data
public class PasswordInitDTO {

    @NotBlank(message = "初始化密码不能为空")
    @Size(min = 6, max = 50, message = "初始化密码长度需在6到50位之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
