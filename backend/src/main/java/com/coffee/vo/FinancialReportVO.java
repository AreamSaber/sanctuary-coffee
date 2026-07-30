package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 财务报表VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class FinancialReportVO {
    
    /**
     * 报表日期范围
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * 总收入
     */
    private BigDecimal totalRevenue;
    
    /**
     * 订单总数
     */
    private Integer totalOrders;
    
    /**
     * 平均订单金额
     */
    private BigDecimal averageOrderAmount;
    
    /**
     * 退款总额
     */
    private BigDecimal totalRefund;
    
    /**
     * 净收入
     */
    private BigDecimal netRevenue;
    
    /**
     * 日收入趋势
     */
    private List<DailyRevenueVO> dailyRevenueTrend;
    
    /**
     * 商品销售排行
     */
    private List<ProductSalesRankVO> productSalesRank;
    
    /**
     * 支付方式分布
     */
    private Map<String, BigDecimal> paymentMethodDistribution;
    
    /**
     * 时段销售分布（0-23小时）
     */
    private Map<Integer, BigDecimal> hourlyDistribution;
    
    /**
     * 会员消费占比
     */
    private BigDecimal memberRevenueRatio;
    
    /**
     * 非促销优惠金额，当前包含优惠券、积分、会员折扣、免配送费等非促销抵扣。
     */
    private BigDecimal couponDeductAmount;
    
    /**
     * 促销活动优惠金额
     */
    private BigDecimal promotionDeductAmount;
    
    @Data
    public static class DailyRevenueVO {
        private LocalDate date;
        private BigDecimal revenue;
        private Integer orderCount;
    }
    
    @Data
    public static class ProductSalesRankVO {
        private Long productId;
        private String productName;
        private Integer salesCount;
        private BigDecimal salesAmount;
        private BigDecimal percentage;
    }
}
