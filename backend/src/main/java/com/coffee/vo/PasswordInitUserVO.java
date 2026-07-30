package com.coffee.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待初始化密码的测试账号。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordInitUserVO {

    private Long id;

    private String username;

    private String nickname;
}
