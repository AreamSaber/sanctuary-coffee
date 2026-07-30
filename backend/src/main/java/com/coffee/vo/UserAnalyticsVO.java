package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户行为分析VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class UserAnalyticsVO {
    
    /**
     * 分析时间范围
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * 活跃用户统计
     */
    private ActiveUsersStats activeUsers;
    
    /**
     * 用户行为分布
     */
    private Map<String, Integer> behaviorDistribution;
    
    /**
     * 页面访问统计
     */
    private List<PageViewStats> pageViews;
    
    /**
     * 商品偏好分析
     */
    private List<ProductPreference> productPreferences;
    
    /**
     * 用户路径分析
     */
    private List<UserPathAnalysis> userPaths;
    
    /**
     * 设备分布
     */
    private Map<String, BigDecimal> deviceDistribution;
    
    /**
     * 时段活跃度
     */
    private Map<Integer, Integer> hourlyActivity;
    
    /**
     * 留存率分析
     */
    private RetentionAnalysis retention;
    
    /**
     * 转化漏斗
     */
    private ConversionFunnel conversionFunnel;
    
    @Data
    public static class ActiveUsersStats {
        private Integer dailyActiveUsers;  // DAU
        private Integer weeklyActiveUsers;  // WAU
        private Integer monthlyActiveUsers; // MAU
        private Integer newUsers;           // 新增用户
        private BigDecimal avgSessionDuration; // 平均会话时长
        private BigDecimal bounceRate;      // 跳出率
    }
    
    @Data
    public static class PageViewStats {
        private String pageUrl;
        private String pageName;
        private Integer views;
        private Integer uniqueVisitors;
        private BigDecimal avgDuration;
        private BigDecimal exitRate;
    }
    
    @Data
    public static class ProductPreference {
        private Long productId;
        private String productName;
        private String categoryName;
        private Integer viewCount;
        private Integer addToCartCount;
        private Integer purchaseCount;
        private BigDecimal conversionRate;
        private BigDecimal revenue;
    }
    
    @Data
    public static class UserPathAnalysis {
        private String pathPattern;
        private Integer frequency;
        private BigDecimal conversionRate;
        private List<String> steps;
    }
    
    @Data
    public static class RetentionAnalysis {
        private BigDecimal day1Retention;  // 次日留存率
        private BigDecimal day7Retention;  // 7日留存率
        private BigDecimal day30Retention; // 30日留存率
        private List<DailyRetention> dailyRetentions;
    }
    
    @Data
    public static class DailyRetention {
        private LocalDate cohortDate;
        private Integer cohortSize;
        private Map<Integer, BigDecimal> retentionByDay;
    }
    
    @Data
    public static class ConversionFunnel {
        private Integer totalVisitors;
        private Integer viewProduct;
        private Integer addToCart;
        private Integer checkout;
        private Integer purchase;
        private BigDecimal viewToCartRate;
        private BigDecimal cartToCheckoutRate;
        private BigDecimal checkoutToPurchaseRate;
        private BigDecimal overallConversionRate;
    }
}
