package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.entity.UserBehavior;
import com.coffee.vo.UserAnalyticsVO;
import java.time.LocalDate;
import java.util.Map;

/**
 * 用户行为分析服务接口
 * 
 * @author Coffee Shop Team
 */
public interface AnalyticsService {
    
    /**
     * 记录用户行为
     */
    void recordUserBehavior(UserBehavior behavior);
    
    /**
     * 批量记录用户行为
     */
    void recordUserBehaviorBatch(java.util.List<UserBehavior> behaviors);
    
    /**
     * 获取用户行为分析报告
     */
    UserAnalyticsVO getUserAnalytics(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取实时统计数据
     */
    Map<String, Object> getRealTimeStats();
    
    /**
     * 获取用户画像
     */
    Map<String, Object> getUserProfile(Long userId);
    
    /**
     * 分页查询用户行为记录
     */
    IPage<UserBehavior> getUserBehaviors(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取热门商品
     */
    java.util.List<UserAnalyticsVO.ProductPreference> getHotProducts(Integer limit);
    
    /**
     * 获取用户活跃度趋势
     */
    Map<LocalDate, Integer> getUserActivityTrend(Integer days);
    
    /**
     * 清理过期的行为数据
     */
    void cleanExpiredBehaviorData(Integer daysToKeep);

    /**
     * 获取复购率统计
     */
    Map<String, Object> getRepurchaseRate(String startTime, String endTime);
}
