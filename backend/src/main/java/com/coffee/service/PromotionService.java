package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.dto.PromotionDTO;
import com.coffee.entity.Promotion;
import java.util.List;

/**
 * 促销活动服务接口
 * 
 * @author Coffee Shop Team
 */
public interface PromotionService extends IService<Promotion> {
    
    /**
     * 分页查询促销活动
     */
    IPage<Promotion> getPromotionPage(Integer pageNum, Integer pageSize, String keyword, String type, Integer status);
    
    /**
     * 创建促销活动
     */
    void createPromotion(PromotionDTO promotionDTO);
    
    /**
     * 更新促销活动
     */
    void updatePromotion(PromotionDTO promotionDTO);
    
    /**
     * 删除促销活动
     */
    void deletePromotion(Long id);
    
    /**
     * 启用/禁用促销活动
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 获取当前有效的促销活动
     */
    List<Promotion> getActivePromotions();
    
    /**
     * 获取商品的促销活动
     */
    Promotion getProductPromotion(Long productId);
}
