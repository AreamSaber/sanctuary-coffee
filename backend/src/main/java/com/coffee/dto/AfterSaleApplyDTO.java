package com.coffee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户售后申请请求。
 */
@Data
public class AfterSaleApplyDTO {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 售后类型：1-仅退款，2-配送问题，3-退货退款
     */
    @NotNull(message = "售后类型不能为空")
    private Integer type;

    /**
     * 售后原因
     */
    @NotBlank(message = "售后原因不能为空")
    @Size(max = 100, message = "售后原因不能超过100个字符")
    private String reason;

    /**
     * 详细说明
     */
    @Size(max = 500, message = "售后说明不能超过500个字符")
    private String description;

    /**
     * 凭证图片，JSON数组字符串
     */
    @Size(max = 1000, message = "凭证图片内容不能超过1000个字符")
    private String images;

    /**
     * 申请退款金额，配送问题可为空
     */
    @DecimalMin(value = "0.00", message = "退款金额不能小于0")
    private BigDecimal refundAmount;
}
