package com.coffee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品DTO
 * 
 * @author Coffee Shop Team
 */
@Data
public class ProductDTO {
    
    /**
     * 商品ID（更新时需要）
     */
    private Long id;
    
    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    
    /**
     * 商品编码
     */
    @NotBlank(message = "商品编码不能为空")
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
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    /**
     * 原价
     */
    private BigDecimal originalPrice;
    
    /**
     * 成本价
     */
    private BigDecimal costPrice;
    
    /**
     * 库存
     */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 状态 0下架 1上架
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
     * 规格定义
     */
    private List<ProductSpecDTO> specList;

    /**
     * SKU 列表
     */
    private List<ProductSkuDTO> skuList;
}
