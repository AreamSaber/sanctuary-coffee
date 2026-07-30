package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("promotion_activity")
public class Promotion implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 活动ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 活动名称
     */
    @TableField("activity_name")
    private String name;
    
    /**
     * 活动描述
     */
    private String description;
    
    /**
     * 活动类型：1-限时折扣，2-满减，3-秒杀
     */
    @TableField("activity_type")
    private Integer type;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 活动横幅
     */
    private String banner;
    
    /**
     * 状态：0-禁用，1-启用
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Long> productIds;

    @TableField(exist = false)
    private BigDecimal flashPrice;

    @TableField(exist = false)
    private Integer stock;
}
