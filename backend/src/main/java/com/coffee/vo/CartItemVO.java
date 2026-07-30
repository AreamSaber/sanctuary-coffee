package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车项VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class CartItemVO {
    
    /**
     * 购物车ID
     */
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    private Long skuId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 商品图片
     */
    private String mainImage;
    
    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * SKU 名称
     */
    private String skuName;

    /**
     * SKU 规格信息
     */
    private String specInfo;
    
    /**
     * 库存
     */
    private Integer stock;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 是否选中
     */
    private Integer checked;
    
    /**
     * 小计金额
     */
    private BigDecimal subtotal;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
