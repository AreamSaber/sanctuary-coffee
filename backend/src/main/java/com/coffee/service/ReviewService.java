package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.dto.ReviewDTO;
import com.coffee.dto.ReviewReplyDTO;
import com.coffee.entity.ProductReview;
import com.coffee.vo.ReviewVO;
import com.coffee.vo.ReviewStatsVO;

import java.util.List;

/**
 * 商品评价服务接口
 * 
 * @author Coffee Shop Team
 */
public interface ReviewService extends IService<ProductReview> {
    
    /**
     * 分页获取商品评价列表
     */
    IPage<ReviewVO> getProductReviews(
            Long productId,
            Integer pageNum,
            Integer pageSize,
            Integer minRating,
            Integer maxRating,
            Boolean hasImages
    );
    
    /**
     * 获取用户的评价列表
     */
    IPage<ReviewVO> getUserReviews(Long userId, Integer pageNum, Integer pageSize, Long productId, Integer rating, Integer status);
    
    /**
     * 获取所有评价列表（管理员）
     */
    IPage<ReviewVO> getAllReviews(Integer pageNum, Integer pageSize, Long productId, Integer rating, Integer status);
    
    /**
     * 添加评价
     */
    void addReview(Long userId, ReviewDTO reviewDTO);
    
    /**
     * 删除评价
     */
    void deleteReview(Long userId, Long reviewId);

    /**
     * 管理员隐藏评价
     */
    void hideReview(Long reviewId);

    /**
     * 管理员恢复评价展示
     */
    void restoreReview(Long reviewId);

    /**
     * 管理员回复或更新评价回复
     */
    void replyReview(Long adminUserId, Long reviewId, ReviewReplyDTO replyDTO);

    /**
     * 获取商品评价统计
     */
    ReviewStatsVO getProductReviewStats(Long productId);
    
    /**
     * 检查是否可以评价（是否已购买且未评价）
     */
    boolean canReview(Long userId, Long orderId, Long productId);
}
