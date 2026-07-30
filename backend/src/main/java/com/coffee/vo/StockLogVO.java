package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存变更记录VO
 */
@Data
public class StockLogVO {
    
    private Long id;
    
    private Long productId;

    private Long skuId;
    
    /**
     * 商品名称
     */
    private String productName;

    /**
     * SKU名称
     */
    private String skuName;

    /**
     * 规格信息
     */
    private String specInfo;
    
    /**
     * 变更类型 1入库 2出库 3退货 4调整
     */
    private Integer changeType;
    
    /**
     * 变更类型名称
     */
    public String getChangeTypeName() {
        if (changeType == null) return "";
        switch (changeType) {
            case 1: return "入库";
            case 2: return "出库";
            case 3: return "退货";
            case 4: return "调整";
            default: return "未知";
        }
    }
    
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
     * 操作人名称
     */
    private String operatorName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
