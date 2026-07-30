package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员权益使用流水视图
 */
@Data
public class MemberBenefitUsageVO {

    private Long id;

    private Long userId;

    private Long benefitId;

    private String benefitName;

    private Integer benefitType;

    private String benefitTypeText;

    private BigDecimal benefitValue;

    private String businessType;

    private String businessTypeText;

    private Long businessId;

    private BigDecimal effectAmount;

    private Integer effectPoints;

    private Integer status;

    private String statusText;

    private String remark;

    private LocalDateTime createTime;
}
