package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员权益视图
 */
@Data
public class MemberBenefitVO {

    private Long id;

    private String benefitName;

    private String benefitCode;

    private Integer benefitType;

    private String benefitTypeText;

    private BigDecimal benefitValue;

    private String valueText;

    private String description;

    private String icon;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
