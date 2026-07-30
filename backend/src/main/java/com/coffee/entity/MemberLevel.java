package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("member_level")
public class MemberLevel implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 等级ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 等级名称
     */
    private String levelName;
    
    /**
     * 等级序号
     */
    @TableField("level_no")
    private Integer levelCode;
    
    /**
     * 所需积分
     */
    @TableField("required_points")
    private Integer requiredPoints;
    
    /**
     * 折扣率（0.95表示95折）
     */
    private BigDecimal discountRate;
    
    /**
     * 等级图标
     */
    private String icon;
    
    /**
     * 等级描述
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
