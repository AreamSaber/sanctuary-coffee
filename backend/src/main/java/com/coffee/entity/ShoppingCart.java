package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("shopping_cart")
public class ShoppingCart implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 购物车ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * SKU ID
     */
    private Long skuId;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 是否选中
     */
    private Integer checked;
    
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
