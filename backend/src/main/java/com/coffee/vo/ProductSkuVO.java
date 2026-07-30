package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 视图对象。
 */
@Data
public class ProductSkuVO {

    private Long id;

    private String skuCode;

    private String skuName;

    private String specInfo;

    private BigDecimal price;

    private Integer stock;

    private String image;

    private Integer status;
}
