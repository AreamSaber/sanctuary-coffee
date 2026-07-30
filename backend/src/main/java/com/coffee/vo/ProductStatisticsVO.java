package com.coffee.vo;

import lombok.Data;

/**
 * 商品统计VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class ProductStatisticsVO {
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 销售数量
     */
    private Integer salesCount;
    
    /**
     * 库存数量
     */
    private Integer stock;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 是否热门
     */
    private Integer isHot;
    
    /**
     * 是否新品
     */
    private Integer isNew;
    
    /**
     * 是否推荐
     */
    private Integer isRecommend;

    /**
     * 售价
     */
    private java.math.BigDecimal price;

    /**
     * 成本价
     */
    private java.math.BigDecimal costPrice;

    /**
     * 单品毛利
     */
    private java.math.BigDecimal unitProfit;

    /**
     * 总毛利
     */
    private java.math.BigDecimal totalProfit;
}
