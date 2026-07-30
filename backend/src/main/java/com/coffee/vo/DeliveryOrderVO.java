package com.coffee.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 配送订单VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class DeliveryOrderVO {
    
    /**
     * 配送订单ID
     */
    private Long id;
    
    /**
     * 配送单号
     */
    private String deliveryNo;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 配送员ID
     */
    private Long deliverymanId;
    
    /**
     * 收货人
     */
    private String receiverName;
    
    /**
     * 收货电话
     */
    private String receiverPhone;
    
    /**
     * 收货地址
     */
    private String receiverAddress;
    
    /**
     * 配送状态
     */
    private Integer deliveryStatus;
    
    /**
     * 配送状态文本
     */
    private String deliveryStatusText;
    
    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;
    
    /**
     * 取货时间
     */
    private LocalDateTime pickupTime;
    
    /**
     * 送达时间
     */
    private LocalDateTime deliveredTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
