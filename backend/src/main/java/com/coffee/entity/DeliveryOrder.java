package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配送订单实体
 */
@Data
@TableName("delivery_order")
public class DeliveryOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("delivery_no")
    private String deliveryNo;

    @TableField("staff_id")
    private Long deliverymanId;

    @TableField("delivery_status")
    private Integer deliveryStatus;

    @TableField("assign_time")
    private LocalDateTime acceptTime;

    @TableField("pickup_time")
    private LocalDateTime pickupTime;

    @TableField("delivery_time")
    private LocalDateTime deliveredTime;

    private String remark;

    private Integer hasException;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
