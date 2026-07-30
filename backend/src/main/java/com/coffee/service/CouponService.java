package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.dto.CouponDTO;
import com.coffee.vo.CouponStatsVO;
import com.coffee.vo.CouponVO;

import java.util.List;

/**
 * 优惠券服务接口
 * 
 * @author Coffee Shop Team
 */
public interface CouponService {
    
    /**
     * 分页查询所有优惠券（管理员）
     */
    IPage<CouponVO> getAllCoupons(Integer pageNum, Integer pageSize, String name, Integer type, Integer status);
    
    /**
     * 分页查询优惠券列表（用户端）
     */
    IPage<CouponVO> getCouponPage(Long userId, Integer pageNum, Integer pageSize);
    
    /**
     * 领取优惠券
     */
    void receiveCoupon(Long userId, Long couponId);
    
    /**
     * 获取用户的优惠券列表
     */
    List<CouponVO> getUserCoupons(Long userId, Integer status);
    
    /**
     * 创建优惠券（管理端）
     */
    void createCoupon(CouponDTO couponDTO);
    
    /**
     * 更新优惠券
     */
    void updateCoupon(Long id, CouponDTO couponDTO);
    
    /**
     * 删除优惠券
     */
    void deleteCoupon(Long id);
    
    /**
     * 更新优惠券状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取优惠券核销统计
     */
    List<CouponStatsVO> getCouponStats(Integer couponType, String startTime, String endTime);
}
