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
 * 会员权益使用流水实体
 */
@Data
@TableName("member_benefit_usage")
public class MemberBenefitUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long benefitId;

    private String benefitName;

    /**
     * 权益类型：1专属折扣 2积分倍率 3免配送费 4生日礼券 5专属服务
     */
    private Integer benefitType;

    private BigDecimal benefitValue;

    private String businessType;

    private Long businessId;

    private BigDecimal effectAmount;

    private Integer effectPoints;

    /**
     * 状态：1使用 2回滚/扣回
     */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
