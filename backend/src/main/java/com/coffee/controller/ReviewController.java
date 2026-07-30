package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.ReviewDTO;
import com.coffee.dto.ReviewReplyDTO;
import com.coffee.service.ReviewService;
import com.coffee.vo.ReviewStatsVO;
import com.coffee.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 商品评价控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "商品评价", description = "商品评价管理接口")
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @Operation(summary = "获取商品评价列表")
    @GetMapping("/product/{productId}")
    public Result<IPage<ReviewVO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Boolean hasImages) {
        IPage<ReviewVO> page = reviewService.getProductReviews(productId, pageNum, pageSize, minRating, maxRating, hasImages);
        return Result.success(page);
    }
    
    @Operation(summary = "获取我的评价列表")
    @GetMapping("/my")
    public Result<IPage<ReviewVO>> getMyReviews(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer status) {
        Long userId = getCurrentUserId();
        IPage<ReviewVO> page = reviewService.getUserReviews(userId, pageNum, pageSize, productId, rating, status);
        return Result.success(page);
    }
    
    @Operation(summary = "获取所有评价列表（管理员）")
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'review:manage')")
    public Result<IPage<ReviewVO>> getAllReviews(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer status) {
        IPage<ReviewVO> page = reviewService.getAllReviews(pageNum, pageSize, productId, rating, status);
        return Result.success(page);
    }
    
    @Operation(summary = "添加商品评价")
    @PostMapping
    public Result<Void> addReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        Long userId = getCurrentUserId();
        reviewService.addReview(userId, reviewDTO);
        return Result.success("评价成功", null);
    }
    
    @Operation(summary = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        reviewService.deleteReview(userId, id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "隐藏评价（管理员）")
    @PutMapping("/admin/{id}/hide")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'review:manage')")
    public Result<Void> hideReview(@PathVariable Long id) {
        reviewService.hideReview(id);
        return Result.success("评价已隐藏", null);
    }

    @Operation(summary = "恢复评价展示（管理员）")
    @PutMapping("/admin/{id}/restore")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'review:manage')")
    public Result<Void> restoreReview(@PathVariable Long id) {
        reviewService.restoreReview(id);
        return Result.success("评价已恢复", null);
    }

    @Operation(summary = "回复评价（管理员）")
    @PostMapping("/admin/{id}/reply")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'review:manage')")
    public Result<Void> replyReview(@PathVariable Long id, @Valid @RequestBody ReviewReplyDTO replyDTO) {
        Long userId = getCurrentUserId();
        reviewService.replyReview(userId, id, replyDTO);
        return Result.success("回复成功", null);
    }

    @Operation(summary = "获取商品评价统计")
    @GetMapping("/stats/{productId}")
    public Result<ReviewStatsVO> getProductReviewStats(@PathVariable Long productId) {
        ReviewStatsVO stats = reviewService.getProductReviewStats(productId);
        return Result.success(stats);
    }
    
    @Operation(summary = "检查是否可以评价")
    @GetMapping("/check")
    public Result<Boolean> checkCanReview(
            @RequestParam Long orderId,
            @RequestParam Long productId) {
        Long userId = getCurrentUserId();
        boolean canReview = reviewService.canReview(userId, orderId, productId);
        return Result.success(canReview);
    }
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
