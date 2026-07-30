package com.coffee.vo;

import lombok.Data;

/**
 * 商品规格视图对象。
 */
@Data
public class ProductSpecVO {

    private Long id;

    private String specName;

    private String specValues;
}
