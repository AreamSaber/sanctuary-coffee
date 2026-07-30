package com.coffee.vo;

import lombok.Data;

/**
 * 用户统计VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class UserStatisticsVO {
    
    /**
     * 总用户数
     */
    private Long totalUsers;
    
    /**
     * 今日新增用户
     */
    private Long todayNewUsers;
    
    /**
     * 本月新增用户
     */
    private Long monthNewUsers;
    
    /**
     * 活跃用户数（本月下单用户）
     */
    private Long activeUsers;
    
    /**
     * 会员用户数
     */
    private Long memberUsers;
}
