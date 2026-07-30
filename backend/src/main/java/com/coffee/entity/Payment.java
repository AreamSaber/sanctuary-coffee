package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("payment")
public class Payment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 支付ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 支付单号
     */
    private String paymentNo;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 支付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 支付方式 1支付宝 2微信 3余额/线下确认
     */
    private Integer payType;
    
    /**
     * 支付状态 0待支付 1支付成功 2支付失败 3已退款
     */
    private Integer payStatus;
    
    /**
     * 第三方交易号
     */
    private String tradeNo;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
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
