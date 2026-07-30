package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类DTO
 * 
 * @author Coffee Shop Team
 */
@Data
public class CategoryDTO {
    
    /**
     * 分类ID（更新时需要）
     */
    private Long id;
    
    /**
     * 父分类ID
     */
    private Long parentId;
    
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    
    /**
     * 分类编码
     */
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;
    
    /**
     * 分类图标
     */
    private String icon;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态 0禁用 1启用
     */
    private Integer status;
}
