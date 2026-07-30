package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配送任务视图对象
 */
@Data
public class DeliveryTaskVO {

    private Long id;

    private Long orderId;

    private String orderNo;

    private String deliveryNo;

    private Integer deliveryStatus;

    private String deliveryStatusText;

    private Long deliverymanId;

    private String deliverymanName;

    private String deliverymanPhone;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime assignTime;

    private LocalDateTime acceptTime;

    private LocalDateTime pickupTime;

    private LocalDateTime deliveredTime;

    private String remark;

    private Integer hasException;

    private LocalDateTime createTime;
}
