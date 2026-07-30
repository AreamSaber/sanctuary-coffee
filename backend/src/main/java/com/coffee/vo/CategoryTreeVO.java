package com.coffee.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class CategoryTreeVO {
    
    /**
     * 分类ID
     */
    private Long id;
    
    /**
     * 父分类ID
     */
    private Long parentId;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 分类编码
     */
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
     * 层级
     */
    private Integer level;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 子分类列表
     */
    private List<CategoryTreeVO> children = new ArrayList<>();
}
