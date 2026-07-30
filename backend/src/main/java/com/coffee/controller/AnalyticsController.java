package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.entity.UserBehavior;
import com.coffee.service.AnalyticsService;
import com.coffee.vo.UserAnalyticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户行为分析控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "数据分析", description = "用户行为分析与数据统计接口")
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @Operation(summary = "记录用户行为")
    @PostMapping("/behavior")
    public Result<Void> recordBehavior(@RequestBody UserBehavior behavior) {
        analyticsService.recordUserBehavior(behavior);
        return Result.success("记录成功", null);
    }
    
    @Operation(summary = "批量记录用户行为")
    @PostMapping("/behavior/batch")
    public Result<Void> recordBehaviorBatch(@RequestBody List<UserBehavior> behaviors) {
        analyticsService.recordUserBehaviorBatch(behaviors);
        return Result.success("记录成功", null);
    }
    
    @Operation(summary = "获取用户行为分析报告")
    @GetMapping("/report")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<UserAnalyticsVO> getAnalyticsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UserAnalyticsVO analytics = analyticsService.getUserAnalytics(startDate, endDate);
        return Result.success(analytics);
    }
    
    @Operation(summary = "获取实时统计数据")
    @GetMapping("/realtime")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<Map<String, Object>> getRealTimeStats() {
        Map<String, Object> stats = analyticsService.getRealTimeStats();
        return Result.success(stats);
    }
    
    @Operation(summary = "获取用户画像")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        Map<String, Object> profile = analyticsService.getUserProfile(userId);
        return Result.success(profile);
    }
    
    @Operation(summary = "分页查询用户行为记录")
    @GetMapping("/behavior/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<IPage<UserBehavior>> getUserBehaviors(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        IPage<UserBehavior> page = analyticsService.getUserBehaviors(userId, pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "获取热门商品")
    @GetMapping("/hot-products")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<List<UserAnalyticsVO.ProductPreference>> getHotProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<UserAnalyticsVO.ProductPreference> products = analyticsService.getHotProducts(limit);
        return Result.success(products);
    }
    
    @Operation(summary = "获取用户活跃度趋势")
    @GetMapping("/activity-trend")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<Map<LocalDate, Integer>> getUserActivityTrend(
            @RequestParam(defaultValue = "30") Integer days) {
        Map<LocalDate, Integer> trend = analyticsService.getUserActivityTrend(days);
        return Result.success(trend);
    }

    @Operation(summary = "获取复购率统计")
    @GetMapping("/repurchase-rate")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'operation:analytics')")
    public Result<Map<String, Object>> getRepurchaseRate(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String, Object> result = analyticsService.getRepurchaseRate(startTime, endTime);
        return Result.success(result);
    }
}
