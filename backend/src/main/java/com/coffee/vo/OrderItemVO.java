package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单明细VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class OrderItemVO {
    
    /**
     * 明细ID
     */
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKU ID
     */
    private Long skuId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 商品图片
     */
    private String productImage;
    
    /**
     * 规格信息
     */
    private String specInfo;
    
    /**
     * 商品单价
     */
    private BigDecimal price;
    
    /**
     * 购买数量
     */
    private Integer quantity;
    
    /**
     * 小计金额
     */
    private BigDecimal totalAmount;
}
