package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品规格 DTO。
 */
@Data
public class ProductSpecDTO {

    private Long id;

    @NotBlank(message = "规格名称不能为空")
    private String specName;

    @NotBlank(message = "规格值不能为空")
    private String specValues;
}
