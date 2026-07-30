package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invoice entity aligned with current database schema.
 */
@Data
@TableName("invoice")
public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("invoice_no")
    private String invoiceNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("user_id")
    private Long userId;

    @TableField("invoice_type")
    private Integer invoiceType;

    @TableField("title_type")
    private Integer titleType;

    private String title;

    @TableField("tax_no")
    private String taxNo;

    private BigDecimal amount;

    private Integer status;

    @TableField("issue_time")
    private LocalDateTime issueTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String type;

    @TableField(exist = false)
    private String taxNumber;

    @TableField(exist = false)
    private String statusText;

    @TableField(exist = false)
    private BigDecimal taxAmount;

    @TableField(exist = false)
    private String content;

    @TableField(exist = false)
    private String email;

    @TableField(exist = false)
    private String fileUrl;

    @TableField(exist = false)
    private LocalDateTime sentTime;

    @TableField(exist = false)
    private String remark;
}
