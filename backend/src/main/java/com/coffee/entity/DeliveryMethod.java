package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Delivery method entity.
 */
@Data
@TableName("delivery_method")
public class DeliveryMethod implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("method_name")
    private String methodName;

    @TableField("description")
    private String description;

    @TableField("base_freight")
    private BigDecimal freight;

    @TableField("free_freight_amount")
    private BigDecimal freeThreshold;

    @TableField(exist = false)
    private String methodCode;

    @TableField(exist = false)
    private Integer estimatedTime;

    @TableField(exist = false)
    private Integer sortOrder;

    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
