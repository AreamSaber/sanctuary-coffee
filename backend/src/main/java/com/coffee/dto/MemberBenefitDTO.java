package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员权益保存参数
 */
@Data
public class MemberBenefitDTO {

    @NotBlank(message = "权益名称不能为空")
    private String benefitName;

    @NotBlank(message = "权益编码不能为空")
    private String benefitCode;

    @NotNull(message = "权益类型不能为空")
    private Integer benefitType;

    private BigDecimal benefitValue;

    private String description;

    private String icon;

    private Integer status;
}
