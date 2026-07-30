package com.coffee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员编辑会员资料 DTO
 */
@Data
public class MemberUpdateDTO {

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
     * 会员等级 ID
     */
    @NotNull(message = "会员等级不能为空")
    private Long levelId;
}
