package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("orders")
public class Order implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 实付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;
    
    /**
     * 运费
     */
    private BigDecimal freightAmount;

    /**
     * 配送方式ID
     */
    private Long deliveryMethodId;

    /**
     * 配送方式名称
     */
    private String deliveryMethodName;

    /**
     * 订单状态 1待付款 2待发货 3待收货 4已完成 5已取消 6退款中 7已退款
     */
    private Integer orderStatus;
    
    /**
     * 支付状态 0未支付 1已支付
     */
    private Integer payStatus;
    
    /**
     * 支付方式 1支付宝 2微信 3余额
     */
    private Integer payType;
    
    /**
     * 订单状态（字符串形式）：PENDING-待付款，PAID-已支付，SHIPPED-已发货，DELIVERED-已送达，COMPLETED-已完成，CANCELLED-已取消，REFUNDED-已退款
     */
    private String status;
    
    /**
     * 支付方式（字符串形式）：WECHAT-微信支付，ALIPAY-支付宝，CASH-现金，CARD-银行卡
     */
    private String paymentMethod;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;
    
    /**
     * 收货时间
     */
    private LocalDateTime receiveTime;
    
    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;
    
    /**
     * 取消原因
     */
    private String cancelReason;
    
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
     * 订单备注
     */
    private String remark;
    
    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
