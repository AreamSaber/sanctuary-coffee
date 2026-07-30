package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 销售统计VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class SalesStatisticsVO {
    
    /**
     * 总订单数
     */
    private Long totalOrders;
    
    /**
     * 总销售额
     */
    private BigDecimal totalSales;
    
    /**
     * 今日订单数
     */
    private Long todayOrders;
    
    /**
     * 今日销售额
     */
    private BigDecimal todaySales;
    
    /**
     * 本月订单数
     */
    private Long monthOrders;
    
    /**
     * 本月销售额
     */
    private BigDecimal monthSales;
    
    /**
     * 待处理订单数
     */
    private Long pendingOrders;
    
    /**
     * 已完成订单数
     */
    private Long completedOrders;
}
