package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员等级权益矩阵视图
 */
@Data
public class MemberLevelBenefitVO {

    private Long levelId;

    private String levelName;

    private Integer levelCode;

    private Integer requiredPoints;

    private BigDecimal discountRate;

    private Integer status;

    private List<MemberBenefitVO> benefits;
}
