package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("product_sku")
public class ProductSku implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * SKU ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * SKU编码
     */
    private String skuCode;
    
    /**
     * SKU名称
     */
    private String skuName;
    
    /**
     * 规格信息
     */
    private String specInfo;
    
    /**
     * 价格
     */
    private BigDecimal price;
    
    /**
     * 库存（可用库存）
     */
    private Integer stock;

    /**
     * 锁定库存（已下单未支付）
     */
    private Integer lockedStock;
    
    /**
     * 图片
     */
    private String image;
    
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
