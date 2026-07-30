package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class ProductVO {
    
    /**
     * 商品ID
     */
    private Long id;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 商品编码
     */
    private String productCode;
    
    /**
     * 商品描述
     */
    private String description;
    
    /**
     * 主图
     */
    private String mainImage;
    
    /**
     * 价格
     */
    private BigDecimal price;
    
    /**
     * 原价
     */
    private BigDecimal originalPrice;
    
    /**
     * 库存
     */
    private Integer stock;
    
    /**
     * 销量
     */
    private Integer sales;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 状态
     */
    private Integer status;
    
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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否存在 SKU
     */
    private Boolean hasSku;

    /**
     * 规格定义
     */
    private List<ProductSpecVO> specList;

    /**
     * SKU 列表
     */
    private List<ProductSkuVO> skuList;
}
