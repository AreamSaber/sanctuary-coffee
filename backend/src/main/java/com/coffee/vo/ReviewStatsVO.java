package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品评价统计VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class ReviewStatsVO {
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 评价总数
     */
    private Integer totalReviews;
    
    /**
     * 平均评分
     */
    private BigDecimal averageRating;
    
    /**
     * 各星级评价数量分布
     * key: 星级(1-5), value: 数量
     */
    private Map<Integer, Integer> ratingDistribution;
    
    /**
     * 各星级评价百分比
     * key: 星级(1-5), value: 百分比
     */
    private Map<Integer, BigDecimal> ratingPercentage;
    
    /**
     * 好评率（4-5星）
     */
    private BigDecimal positiveRate;
    
    /**
     * 有图评价数
     */
    private Integer withImagesCount;
}
