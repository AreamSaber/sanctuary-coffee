package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("product_category")
public class ProductCategory implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 分类ID
     */
    @TableId(type = IdType.AUTO)
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
     * 状态 0禁用 1启用
     */
    private Integer status;
    
    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
