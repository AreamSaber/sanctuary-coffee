package com.coffee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU DTO。
 */
@Data
public class ProductSkuDTO {

    private Long id;

    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    @NotBlank(message = "SKU名称不能为空")
    private String skuName;

    private String specInfo;

    @NotNull(message = "SKU价格不能为空")
    @DecimalMin(value = "0.01", message = "SKU价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "SKU库存不能为空")
    @Min(value = 0, message = "SKU库存不能小于0")
    private Integer stock;

    private String image;

    private Integer status;
}
