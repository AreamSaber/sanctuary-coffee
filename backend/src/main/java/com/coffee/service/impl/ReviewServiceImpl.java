package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.ReviewDTO;
import com.coffee.dto.ReviewReplyDTO;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.Product;
import com.coffee.entity.ProductReview;
import com.coffee.entity.ReviewReply;
import com.coffee.entity.User;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.ProductReviewMapper;
import com.coffee.mapper.ReviewReplyMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.ReviewService;
import com.coffee.vo.ReviewStatsVO;
import com.coffee.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview> implements ReviewService {

    private final ProductReviewMapper reviewMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final AnalyticsService analyticsService;

    @Override
    public IPage<ReviewVO> getProductReviews(
            Long productId,
            Integer pageNum,
            Integer pageSize,
            Integer minRating,
            Integer maxRating,
            Boolean hasImages
    ) {
        Page<ProductReview> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, productId)
                .eq(ProductReview::getStatus, 1)
                .ge(minRating != null, ProductReview::getRating, minRating)
                .le(maxRating != null, ProductReview::getRating, maxRating)
                .orderByDesc(ProductReview::getCreateTime);

        if (Boolean.TRUE.equals(hasImages)) {
            List<ProductReview> filteredReviews = reviewMapper.selectList(wrapper).stream()
                    .filter(review -> hasReviewImages(review.getImages()))
                    .collect(Collectors.toList());
            return buildReviewPage(pageNum, pageSize, filteredReviews);
        }

        IPage<ProductReview> reviewPage = reviewMapper.selectPage(page, wrapper);
        return buildReviewPage(pageNum, pageSize, reviewPage.getTotal(), reviewPage.getRecords());
    }

    @Override
    public IPage<ReviewVO> getUserReviews(Long userId, Integer pageNum, Integer pageSize, Long productId, Integer rating, Integer status) {
        Page<ProductReview> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getUserId, userId)
                .eq(productId != null, ProductReview::getProductId, productId)
                .eq(rating != null && rating > 0, ProductReview::getRating, rating)
                .eq(status != null, ProductReview::getStatus, status)
                .orderByDesc(ProductReview::getCreateTime);

        IPage<ProductReview> reviewPage = reviewMapper.selectPage(page, wrapper);
        return buildReviewPage(pageNum, pageSize, reviewPage.getTotal(), reviewPage.getRecords());
    }

    @Override
    public IPage<ReviewVO> getAllReviews(Integer pageNum, Integer pageSize, Long productId, Integer rating, Integer status) {
        Page<ProductReview> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();

        if (productId != null) {
            wrapper.eq(ProductReview::getProductId, productId);
        }

        if (rating != null && rating > 0) {
            wrapper.eq(ProductReview::getRating, rating);
        }

        if (status != null) {
            wrapper.eq(ProductReview::getStatus, status);
        }

        wrapper.orderByDesc(ProductReview::getCreateTime);

        IPage<ProductReview> reviewPage = reviewMapper.selectPage(page, wrapper);
        return buildReviewPage(pageNum, pageSize, reviewPage.getTotal(), reviewPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addReview(Long userId, ReviewDTO reviewDTO) {
        if (!canReview(userId, reviewDTO.getOrderId(), reviewDTO.getProductId())) {
            throw new BusinessException(ResultCode.REVIEW_NOT_ALLOWED);
        }

        ProductReview review = new ProductReview();
        BeanUtil.copyProperties(reviewDTO, review);
        review.setUserId(userId);
        review.setIsAnonymous(reviewDTO.getIsAnonymous() ? 1 : 0);
        review.setStatus(1);

        reviewMapper.insert(review);
        recordReviewBehavior(userId, reviewDTO, review);
        log.info("User {} added review for product {}", userId, reviewDTO.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long userId, Long reviewId) {
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.REVIEW_NOT_EXIST);
        }

        if (!review.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        reviewMapper.deleteById(reviewId);
        log.info("User {} deleted review {}", userId, reviewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hideReview(Long reviewId) {
        updateReviewDisplayStatus(reviewId, 0, "hidden");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreReview(Long reviewId) {
        updateReviewDisplayStatus(reviewId, 1, "restored");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyReview(Long adminUserId, Long reviewId, ReviewReplyDTO replyDTO) {
        String content = replyDTO == null || replyDTO.getContent() == null
                ? ""
                : replyDTO.getContent().trim();
        if (content.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "回复内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "回复内容不能超过500个字符");
        }

        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.REVIEW_NOT_EXIST);
        }

        ReviewReply existingReply = reviewReplyMapper.selectOne(
                new LambdaQueryWrapper<ReviewReply>()
                        .eq(ReviewReply::getReviewId, reviewId)
                        .orderByDesc(ReviewReply::getCreateTime)
                        .orderByDesc(ReviewReply::getId)
                        .last("LIMIT 1")
        );

        if (existingReply == null) {
            ReviewReply reply = new ReviewReply();
            reply.setReviewId(reviewId);
            reply.setUserId(adminUserId);
            reply.setContent(content);
            reviewReplyMapper.insert(reply);
            log.info("Admin {} replied review {}", adminUserId, reviewId);
            return;
        }

        existingReply.setUserId(adminUserId);
        existingReply.setContent(content);
        reviewReplyMapper.updateById(existingReply);
        log.info("Admin {} updated reply {} for review {}", adminUserId, existingReply.getId(), reviewId);
    }

    @Override
    public ReviewStatsVO getProductReviewStats(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }

        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, productId)
                .eq(ProductReview::getStatus, 1);
        List<ProductReview> reviews = reviewMapper.selectList(wrapper);

        ReviewStatsVO stats = new ReviewStatsVO();
        stats.setProductId(productId);
        stats.setProductName(product.getProductName());
        stats.setTotalReviews(reviews.size());

        if (reviews.isEmpty()) {
            stats.setAverageRating(BigDecimal.ZERO);
            stats.setRatingDistribution(new HashMap<>());
            stats.setRatingPercentage(new HashMap<>());
            stats.setPositiveRate(BigDecimal.ZERO);
            stats.setWithImagesCount(0);
            return stats;
        }

        double avgRating = reviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);
        stats.setAverageRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));

        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }
        reviews.forEach(r -> distribution.merge(r.getRating(), 1, Integer::sum));
        stats.setRatingDistribution(distribution);

        Map<Integer, BigDecimal> percentage = new HashMap<>();
        int total = reviews.size();
        for (int i = 1; i <= 5; i++) {
            BigDecimal pct = BigDecimal.valueOf(distribution.get(i) * 100.0 / total)
                    .setScale(1, RoundingMode.HALF_UP);
            percentage.put(i, pct);
        }
        stats.setRatingPercentage(percentage);

        long positiveCount = reviews.stream()
                .filter(r -> r.getRating() >= 4)
                .count();
        BigDecimal positiveRate = BigDecimal.valueOf(positiveCount * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP);
        stats.setPositiveRate(positiveRate);

        long withImages = reviews.stream()
                .filter(r -> hasReviewImages(r.getImages()))
                .count();
        stats.setWithImagesCount((int) withImages);

        return stats;
    }

    @Override
    public boolean canReview(Long userId, Long orderId, Long productId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return false;
        }

        if (!"COMPLETED".equals(order.getStatus())) {
            return false;
        }

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getProductId, productId);
        OrderItem orderItem = orderItemMapper.selectOne(itemWrapper);
        if (orderItem == null) {
            return false;
        }

        LambdaQueryWrapper<ProductReview> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(ProductReview::getUserId, userId)
                .eq(ProductReview::getOrderId, orderId)
                .eq(ProductReview::getProductId, productId);
        Long count = reviewMapper.selectCount(reviewWrapper);

        return count == 0;
    }

    private List<ReviewVO> convertToVOList(List<ProductReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> userIds = reviews.stream()
                .map(ProductReview::getUserId)
                .collect(Collectors.toSet());
        Set<Long> productIds = reviews.stream()
                .map(ProductReview::getProductId)
                .collect(Collectors.toSet());
        Set<Long> orderIds = reviews.stream()
                .map(ProductReview::getOrderId)
                .collect(Collectors.toSet());
        Set<Long> reviewIds = reviews.stream()
                .map(ProductReview::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
        }

        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(productIds);
            productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
        }

        Map<String, OrderItem> orderItemMap = new HashMap<>();
        if (!orderIds.isEmpty() && !productIds.isEmpty()) {
            List<OrderItem> orderItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getOrderId, orderIds)
                            .in(OrderItem::getProductId, productIds)
                            .orderByAsc(OrderItem::getId)
            );
            orderItems.forEach(item -> orderItemMap.putIfAbsent(
                    buildReviewItemKey(item.getOrderId(), item.getProductId()),
                    item
            ));
        }

        Map<Long, ReviewReply> replyMap = new HashMap<>();
        if (!reviewIds.isEmpty()) {
            List<ReviewReply> replies = reviewReplyMapper.selectList(
                    new LambdaQueryWrapper<ReviewReply>()
                            .in(ReviewReply::getReviewId, reviewIds)
                            .orderByDesc(ReviewReply::getCreateTime)
                            .orderByDesc(ReviewReply::getId)
            );
            replies.forEach(reply -> replyMap.putIfAbsent(reply.getReviewId(), reply));
        }

        List<ReviewVO> voList = new ArrayList<>();
        for (ProductReview review : reviews) {
            ReviewVO vo = new ReviewVO();
            BeanUtil.copyProperties(review, vo);

            User user = userMap.get(review.getUserId());
            if (user != null) {
                if (review.getIsAnonymous() == 1) {
                    vo.setNickname("匿名用户");
                    vo.setAvatar(null);
                } else {
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                }
            }

            OrderItem orderItem = orderItemMap.get(buildReviewItemKey(review.getOrderId(), review.getProductId()));
            if (orderItem != null) {
                vo.setSkuId(orderItem.getSkuId());
                vo.setSpecInfo(orderItem.getSpecInfo());
                vo.setProductName(orderItem.getProductName());
                vo.setProductImage(orderItem.getProductImage());
            }

            Product product = productMap.get(review.getProductId());
            if (product != null) {
                if (vo.getProductName() == null || vo.getProductName().isBlank()) {
                    vo.setProductName(product.getProductName());
                }
                if (vo.getProductImage() == null || vo.getProductImage().isBlank()) {
                    vo.setProductImage(product.getMainImage());
                }
            }

            if (review.getImages() != null && !review.getImages().isEmpty()) {
                try {
                    List<String> images = JSONUtil.toList(review.getImages(), String.class);
                    vo.setImageList(images);
                } catch (Exception e) {
                    vo.setImageList(new ArrayList<>());
                }
            } else {
                vo.setImageList(new ArrayList<>());
            }

            vo.setIsAnonymous(review.getIsAnonymous() == 1);
            ReviewReply reply = replyMap.get(review.getId());
            if (reply != null) {
                vo.setReplied(true);
                vo.setReplyId(reply.getId());
                vo.setReplyUserId(reply.getUserId());
                vo.setReplyContent(reply.getContent());
                vo.setReplyTime(reply.getCreateTime());
            } else {
                vo.setReplied(false);
            }
            voList.add(vo);
        }

        return voList;
    }

    private String buildReviewItemKey(Long orderId, Long productId) {
        return orderId + ":" + productId;
    }

    private Page<ReviewVO> buildReviewPage(Integer pageNum, Integer pageSize, List<ProductReview> reviews) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.max((safePageNum - 1) * safePageSize, 0);
        int toIndex = Math.min(fromIndex + safePageSize, reviews.size());
        List<ProductReview> currentPageRecords = fromIndex >= reviews.size()
                ? Collections.emptyList()
                : reviews.subList(fromIndex, toIndex);
        return buildReviewPage(safePageNum, safePageSize, reviews.size(), currentPageRecords);
    }

    private Page<ReviewVO> buildReviewPage(long pageNum, long pageSize, long total, List<ProductReview> reviews) {
        Page<ReviewVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(total);
        voPage.setRecords(convertToVOList(reviews));
        return voPage;
    }

    private void updateReviewDisplayStatus(Long reviewId, Integer status, String actionName) {
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.REVIEW_NOT_EXIST);
        }

        if (status.equals(review.getStatus())) {
            log.info("Review {} already {}", reviewId, actionName);
            return;
        }

        ProductReview update = new ProductReview();
        update.setId(reviewId);
        update.setStatus(status);
        reviewMapper.updateById(update);
        log.info("Admin {} review {}", actionName, reviewId);
    }

    private boolean hasReviewImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return false;
        }

        try {
            return !JSONUtil.toList(imagesJson.trim(), String.class).isEmpty();
        } catch (Exception ex) {
            log.warn("Invalid review image payload, treat as no image: {}", imagesJson);
            return false;
        }
    }

    private void recordReviewBehavior(Long userId, ReviewDTO reviewDTO, ProductReview review) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType("REVIEW");
        behavior.setTargetType("PRODUCT");
        behavior.setTargetId(reviewDTO.getProductId());
        behavior.setPageUrl("/orders");
        behavior.setActionData(JSONUtil.createObj()
                .set("orderId", reviewDTO.getOrderId())
                .set("reviewId", review.getId())
                .set("rating", reviewDTO.getRating())
                .set("hasImages", hasReviewImages(reviewDTO.getImages()))
                .toString());
        analyticsService.recordUserBehavior(behavior);
    }
}
