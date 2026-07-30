package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon view object.
 */
@Data
public class CouponVO {

    /**
     * Primary identifier for the current endpoint.
     * For available/admin coupon endpoints this is coupon.id.
     * For my-coupons endpoint this is user_coupon.id.
     */
    private Long id;

    /**
     * Coupon template id.
     */
    private Long couponId;

    private String couponCode;

    private Integer status;

    private String couponName;

    private Integer couponType;

    private String couponTypeText;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    private Integer remainCount;

    private Integer limitPerUser;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime expireTime;

    private Integer validDays;

    private Boolean received;

    private Integer receivedNum;
}
