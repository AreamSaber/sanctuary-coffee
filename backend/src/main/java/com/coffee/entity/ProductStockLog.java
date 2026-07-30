package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品库存变更记录实体
 */
@Data
@TableName("product_stock_log")
public class ProductStockLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * SKU ID
     */
    private Long skuId;
    
    /**
     * 变更类型 1入库 2出库 3退货 4调整
     */
    private Integer changeType;
    
    /**
     * 变更数量
     */
    private Integer changeQuantity;
    
    /**
     * 变更前库存
     */
    private Integer beforeStock;
    
    /**
     * 变更后库存
     */
    private Integer afterStock;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
