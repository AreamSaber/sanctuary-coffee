package com.coffee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付 DTO
 */
@Data
public class PaymentDTO {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 支付方式：1-支付宝，2-微信，3-余额/线下确认
     */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 使用积分
     */
    private Integer usePoints;
}
