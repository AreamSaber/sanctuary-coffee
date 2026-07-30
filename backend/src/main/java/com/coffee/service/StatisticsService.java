package com.coffee.service;

import com.coffee.vo.ProductStatisticsVO;
import com.coffee.vo.SalesStatisticsVO;
import com.coffee.vo.UserStatisticsVO;

import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 * 
 * @author Coffee Shop Team
 */
public interface StatisticsService {
    
    /**
     * 获取销售统计数据
     */
    SalesStatisticsVO getSalesStatistics();
    
    /**
     * 获取商品销售排行
     */
    List<ProductStatisticsVO> getTopSellingProducts(Integer limit);
    
    /**
     * 获取低库存商品
     */
    List<ProductStatisticsVO> getLowStockProducts(Integer threshold);
    
    /**
     * 获取用户统计数据
     */
    UserStatisticsVO getUserStatistics();
    
    /**
     * 获取每日销售趋势（最近N天）
     */
    List<Map<String, Object>> getDailySalesTrend(Integer days);
    
    /**
     * 获取商品分类销售占比
     */
    List<Map<String, Object>> getCategorySalesDistribution();

    /**
     * 获取商品毛利分析
     */
    List<ProductStatisticsVO> getProductProfitAnalysis();
}
