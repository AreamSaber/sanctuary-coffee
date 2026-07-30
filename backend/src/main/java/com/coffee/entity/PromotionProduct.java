package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 促销活动商品关联实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("promotion_product")
public class PromotionProduct implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 活动ID
     */
    @TableField("activity_id")
    private Long promotionId;
    
    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;
    
    /**
     * 活动价格
     */
    @TableField("promotion_price")
    private java.math.BigDecimal promotionPrice;
    
    /**
     * 限购数量
     */
    @TableField("stock_limit")
    private Integer stockLimit;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
