package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order view object.
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal discountAmount;

    private BigDecimal freightAmount;

    private Long deliveryMethodId;

    private String deliveryMethodName;

    private Integer orderStatus;

    private String orderStatusText;

    private Integer payStatus;

    private Integer payType;

    private String status;

    private String paymentMethod;

    private java.time.LocalDateTime payTime;

    private java.time.LocalDateTime deliveryTime;

    private java.time.LocalDateTime receiveTime;

    private java.time.LocalDateTime cancelTime;

    private String cancelReason;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;
}
