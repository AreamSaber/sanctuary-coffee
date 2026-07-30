package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.service.StatisticsService;
import com.coffee.vo.ProductStatisticsVO;
import com.coffee.vo.SalesStatisticsVO;
import com.coffee.vo.UserStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "数据统计")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'dashboard:view')")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @Operation(summary = "获取销售统计")
    @GetMapping("/sales")
    public Result<SalesStatisticsVO> getSalesStatistics() {
        SalesStatisticsVO statistics = statisticsService.getSalesStatistics();
        return Result.success(statistics);
    }
    
    @Operation(summary = "获取商品销售排行")
    @GetMapping("/products/top")
    public Result<List<ProductStatisticsVO>> getTopSellingProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<ProductStatisticsVO> products = statisticsService.getTopSellingProducts(limit);
        return Result.success(products);
    }
    
    @Operation(summary = "获取低库存商品")
    @GetMapping("/products/low-stock")
    public Result<List<ProductStatisticsVO>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<ProductStatisticsVO> products = statisticsService.getLowStockProducts(threshold);
        return Result.success(products);
    }
    
    @Operation(summary = "获取用户统计")
    @GetMapping("/users")
    public Result<UserStatisticsVO> getUserStatistics() {
        UserStatisticsVO statistics = statisticsService.getUserStatistics();
        return Result.success(statistics);
    }
    
    @Operation(summary = "获取每日销售趋势")
    @GetMapping("/sales/trend")
    public Result<List<Map<String, Object>>> getDailySalesTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        List<Map<String, Object>> trend = statisticsService.getDailySalesTrend(days);
        return Result.success(trend);
    }
    
    @Operation(summary = "获取商品分类销售占比")
    @GetMapping("/categories/distribution")
    public Result<List<Map<String, Object>>> getCategorySalesDistribution() {
        List<Map<String, Object>> distribution = statisticsService.getCategorySalesDistribution();
        return Result.success(distribution);
    }

    @Operation(summary = "获取商品毛利分析")
    @GetMapping("/products/profit")
    public Result<List<ProductStatisticsVO>> getProductProfitAnalysis() {
        return Result.success(statisticsService.getProductProfitAnalysis());
    }
}
