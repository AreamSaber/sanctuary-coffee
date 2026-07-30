package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("product")
public class Product implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
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
     * 成本价
     */
    private BigDecimal costPrice;
    
    /**
     * 库存（可用库存）
     */
    private Integer stock;

    /**
     * 锁定库存（已下单未支付）
     */
    private Integer lockedStock;
    
    /**
     * 销量
     */
    private Integer sales;
    
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
