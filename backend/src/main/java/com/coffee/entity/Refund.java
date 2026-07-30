package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("refund")
public class Refund implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 退款ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 退款单号
     */
    private String refundNo;
    
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
     * 退款金额
     */
    private BigDecimal refundAmount;
    
    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审核备注
     */
    private String reviewRemark;

    /**
     * 退款状态 0申请中 1退款成功 2退款失败
     */
    private Integer refundStatus;
    
    /**
     * 第三方退款单号
     */
    private String tradeNo;
    
    /**
     * 退款时间
     */
    private LocalDateTime refundTime;
    
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
