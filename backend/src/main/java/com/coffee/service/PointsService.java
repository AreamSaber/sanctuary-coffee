package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.entity.PointsRecord;

/**
 * 积分服务接口
 * 
 * @author Coffee Shop Team
 */
public interface PointsService {
    
    /**
     * 增加积分
     */
    void addPoints(Long userId, Integer points, Integer bizType, Long bizId, String description);
    
    /**
     * 扣减积分
     */
    void deductPoints(Long userId, Integer points, Integer bizType, Long bizId, String description);
    
    /**
     * 分页查询积分记录
     */
    IPage<PointsRecord> getPointsRecordPage(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 获取积分余额
     */
    Integer getPointsBalance(Long userId);
}
