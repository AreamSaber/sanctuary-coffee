package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon DTO.
 */
@Data
public class CouponDTO {

    @NotBlank(message = "Coupon name is required")
    private String couponName;

    @NotNull(message = "Coupon type is required")
    private Integer couponType;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal minAmount;

    @NotNull(message = "Total count is required")
    private Integer totalCount;

    private Integer limitPerUser;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private Integer validDays;

    private Integer useScope;

    private String scopeIds;

    private String description;
}
