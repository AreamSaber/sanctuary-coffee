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
 * 售后处理日志。
 */
@Data
@TableName("order_after_sale_log")
public class OrderAfterSaleLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long afterSaleId;

    private Long orderId;

    private Long userId;

    private Long operatorId;

    private String operatorType;

    private String action;

    private Integer statusFrom;

    private Integer statusTo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
