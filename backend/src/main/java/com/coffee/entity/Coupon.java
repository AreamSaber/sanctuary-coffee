package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 优惠券ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 优惠券名称
     */
    @TableField("coupon_name")
    private String couponName;
    
    /**
     * 优惠券类型 1满减 2折扣 3免邮
     */
    @TableField("coupon_type")
    private Integer couponType;
    
    /**
     * 优惠类型 1金额 2折扣
     */
    @TableField("discount_type")
    private Integer discountType;
    
    /**
     * 优惠值
     */
    @TableField("discount_value")
    private BigDecimal discountValue;
    
    /**
     * 使用门槛金额
     */
    @TableField("min_amount")
    private BigDecimal minAmount;
    
    /**
     * 发放总量
     */
    @TableField("total_quantity")
    private Integer totalQuantity;
    
    /**
     * 已领取数量
     */
    @TableField("received_quantity")
    private Integer receivedQuantity;
    
    /**
     * 每人限领数量
     */
    @TableField("per_limit")
    private Integer perLimit;
    
    /**
     * 有效期开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;
    
    /**
     * 有效期结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;
    
    /**
     * 领取后有效天数
     */
    @TableField("valid_days")
    private Integer validDays;
    
    /**
     * 使用范围 1全场 2指定分类 3指定商品
     */
    @TableField("use_scope")
    private Integer useScope;
    
    /**
     * 范围ID(JSON)
     */
    @TableField("scope_ids")
    private String scopeIds;
    
    /**
     * 使用说明
     */
    private String description;
    
    /**
     * 状态 0禁用 1启用
     */
    private Integer status;
    
    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
