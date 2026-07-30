package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券核销统计视图对象
 */
@Data
public class CouponStatsVO {

    private Long couponId;

    private String couponName;

    private Integer couponType;

    private String couponTypeText;

    /**
     * 总发放数
     */
    private Integer totalReceived;

    /**
     * 已使用数
     */
    private Integer totalUsed;

    /**
     * 核销率（百分比）
     */
    private BigDecimal redemptionRate;
}
