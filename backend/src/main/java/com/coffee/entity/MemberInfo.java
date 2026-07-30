package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员信息实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("member_info")
public class MemberInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 会员ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会员等级ID
     */
    private Long levelId;
    
    /**
     * 成长值
     */
    private Integer growthValue;
    
    /**
     * 积分余额
     */
    private Integer points;
    
    /**
     * 累计消费金额
     */
    @TableField("total_consume")
    private BigDecimal totalConsumption;
    
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
