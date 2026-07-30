package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权益发放记录实体
 */
@Data
@TableName("benefit_grant_log")
public class BenefitGrantLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long benefitId;

    /**
     * 权益类型(冗余)
     */
    private Integer benefitType;

    /**
     * 发放值(金额/积分)
     */
    private BigDecimal grantValue;

    /**
     * 发放原因
     */
    private String grantReason;

    /**
     * 关联订单ID(可选)
     */
    private Long orderId;

    /**
     * 操作人ID(系统=0)
     */
    private Long operatorId;

    /**
     * 1已发放 2已撤销
     */
    private Integer status;

    /**
     * 发放时间
     */
    private LocalDateTime grantTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
