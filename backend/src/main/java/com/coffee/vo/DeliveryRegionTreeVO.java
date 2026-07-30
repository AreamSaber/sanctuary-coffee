package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 配送区域树形结构VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class DeliveryRegionTreeVO {
    
    /**
     * 区域ID
     */
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
     * 区域级别
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
     * 预计配送时间
     */
    private Integer estimatedTime;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 配送员数量
     */
    private Integer staffCount;
    
    /**
     * 子区域
     */
    private List<DeliveryRegionTreeVO> children;
}
