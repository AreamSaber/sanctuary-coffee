package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送区域实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("delivery_region")
public class DeliveryRegion implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 区域ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 区域名称
     */
    private String regionName;
    
    /**
     * 区域编码
     */
    private String regionCode;
    
    /**
     * 上级区域ID
     */
    private Long parentId;
    
    /**
     * 区域级别：1-省/直辖市，2-市，3-区/县，4-街道/镇
     */
    private Integer level;
    
    /**
     * 配送费
     */
    private BigDecimal deliveryFee;
    
    /**
     * 最低起送金额
     */
    private BigDecimal minOrderAmount;
    
    /**
     * 预计配送时间（分钟）
     */
    private Integer estimatedTime;
    
    /**
     * 经度
     */
    private BigDecimal longitude;
    
    /**
     * 纬度
     */
    private BigDecimal latitude;
    
    /**
     * 配送范围（公里）
     */
    private BigDecimal deliveryRange;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
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
