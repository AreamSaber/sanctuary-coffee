package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.coffee.vo.ReviewVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ProductReviewMapper reviewMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ReviewReplyMapper reviewReplyMapper;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(ProductReview.class);
        initTableInfo(ReviewReply.class);
        initTableInfo(Order.class);
        initTableInfo(OrderItem.class);
        initTableInfo(UserBehavior.class);
    }

    @Test
    void addReviewRecordsReviewBehavior() {
        Long userId = 100L;
        ReviewDTO dto = reviewDTO(1L, 20L, 5, "味道很好，包装也不错", "[\"/uploads/review.jpg\"]");
        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setStatus("COMPLETED");
        OrderItem item = new OrderItem();
        item.setOrderId(1L);
        item.setProductId(20L);

        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectOne(anyOrderItemQueryWrapper())).thenReturn(item);
        when(reviewMapper.selectCount(anyReviewQueryWrapper())).thenReturn(0L);

        reviewService.addReview(userId, dto);

        ArgumentCaptor<ProductReview> reviewCaptor = ArgumentCaptor.forClass(ProductReview.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(20L, reviewCaptor.getValue().getProductId());
        assertEquals(userId, reviewCaptor.getValue().getUserId());
        assertEquals(1, reviewCaptor.getValue().getStatus());

        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals(userId, behavior.getUserId());
        assertEquals("REVIEW", behavior.getActionType());
        assertEquals("PRODUCT", behavior.getTargetType());
        assertEquals(20L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"orderId\":1"));
        assertTrue(behavior.getActionData().contains("\"rating\":5"));
        assertTrue(behavior.getActionData().contains("\"hasImages\":true"));
    }

    @Test
    void hideReviewSetsStatusToHidden() {
        ProductReview review = productReview(10L, 1);
        when(reviewMapper.selectById(10L)).thenReturn(review);

        reviewService.hideReview(10L);

        ArgumentCaptor<ProductReview> reviewCaptor = ArgumentCaptor.forClass(ProductReview.class);
        verify(reviewMapper).updateById(reviewCaptor.capture());
        assertEquals(10L, reviewCaptor.getValue().getId());
        assertEquals(0, reviewCaptor.getValue().getStatus());
    }

    @Test
    void restoreReviewSetsStatusToVisible() {
        ProductReview review = productReview(10L, 0);
        when(reviewMapper.selectById(10L)).thenReturn(review);

        reviewService.restoreReview(10L);

        ArgumentCaptor<ProductReview> reviewCaptor = ArgumentCaptor.forClass(ProductReview.class);
        verify(reviewMapper).updateById(reviewCaptor.capture());
        assertEquals(10L, reviewCaptor.getValue().getId());
        assertEquals(1, reviewCaptor.getValue().getStatus());
    }

    @Test
    void replyReviewCreatesReplyWhenMissing() {
        ProductReview review = productReview(10L, 1);
        ReviewReplyDTO dto = replyDTO("  感谢反馈，欢迎下次体验  ");

        when(reviewMapper.selectById(10L)).thenReturn(review);
        when(reviewReplyMapper.selectOne(anyReplyQueryWrapper())).thenReturn(null);

        reviewService.replyReview(20L, 10L, dto);

        ArgumentCaptor<ReviewReply> replyCaptor = ArgumentCaptor.forClass(ReviewReply.class);
        verify(reviewReplyMapper).insert(replyCaptor.capture());
        ReviewReply reply = replyCaptor.getValue();
        assertEquals(10L, reply.getReviewId());
        assertEquals(20L, reply.getUserId());
        assertEquals("感谢反馈，欢迎下次体验", reply.getContent());
    }

    @Test
    void replyReviewUpdatesExistingReply() {
        ProductReview review = productReview(10L, 1);
        ReviewReply existingReply = reviewReply(30L, 10L, 18L, "旧回复", LocalDateTime.of(2026, 5, 1, 10, 0));

        when(reviewMapper.selectById(10L)).thenReturn(review);
        when(reviewReplyMapper.selectOne(anyReplyQueryWrapper())).thenReturn(existingReply);

        reviewService.replyReview(20L, 10L, replyDTO("  已更新回复  "));

        ArgumentCaptor<ReviewReply> replyCaptor = ArgumentCaptor.forClass(ReviewReply.class);
        verify(reviewReplyMapper).updateById(replyCaptor.capture());
        ReviewReply reply = replyCaptor.getValue();
        assertEquals(30L, reply.getId());
        assertEquals(10L, reply.getReviewId());
        assertEquals(20L, reply.getUserId());
        assertEquals("已更新回复", reply.getContent());
    }

    @Test
    void replyReviewRejectsBlankContent() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reviewService.replyReview(20L, 10L, replyDTO("   "))
        );

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(reviewMapper, never()).selectById(10L);
        verify(reviewReplyMapper, never()).insert(any(ReviewReply.class));
    }

    @Test
    void getAllReviewsIncludesLatestReply() {
        ProductReview review = productReview(10L, 1);
        review.setCreateTime(LocalDateTime.of(2026, 5, 2, 12, 0));
        Page<ProductReview> reviewPage = new Page<>(1, 10, 1);
        reviewPage.setRecords(List.of(review));

        User user = new User();
        user.setId(100L);
        user.setNickname("测试用户");
        user.setAvatar("/avatar.png");

        Product product = new Product();
        product.setId(20L);
        product.setProductName("拿铁");
        product.setMainImage("/latte.png");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(1L);
        orderItem.setProductId(20L);
        orderItem.setSkuId(200L);
        orderItem.setSpecInfo("中杯/少糖");
        orderItem.setProductName("拿铁快照");
        orderItem.setProductImage("/latte-order.png");

        ReviewReply reply = reviewReply(30L, 10L, 20L, "感谢反馈", LocalDateTime.of(2026, 5, 3, 9, 30));

        when(reviewMapper.selectPage(anyReviewPage(), anyReviewQueryWrapper())).thenReturn(reviewPage);
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(product));
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(orderItem));
        when(reviewReplyMapper.selectList(anyReplyQueryWrapper())).thenReturn(List.of(reply));

        IPage<ReviewVO> result = reviewService.getAllReviews(1, 10, null, null, null);

        assertEquals(1, result.getRecords().size());
        ReviewVO vo = result.getRecords().get(0);
        assertEquals(10L, vo.getId());
        assertEquals("测试用户", vo.getNickname());
        assertEquals("拿铁快照", vo.getProductName());
        assertEquals(200L, vo.getSkuId());
        assertEquals(Boolean.TRUE, vo.getReplied());
        assertEquals(30L, vo.getReplyId());
        assertEquals(20L, vo.getReplyUserId());
        assertEquals("感谢反馈", vo.getReplyContent());
        assertEquals(LocalDateTime.of(2026, 5, 3, 9, 30), vo.getReplyTime());
        assertFalse(vo.getIsAnonymous());
    }

    @Test
    void hideReviewThrowsWhenReviewMissing() {
        when(reviewMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reviewService.hideReview(10L)
        );

        assertEquals(ResultCode.REVIEW_NOT_EXIST.getCode(), exception.getCode());
        verify(reviewMapper, never()).updateById(any(ProductReview.class));
    }

    @Test
    void hideReviewSkipsUpdateWhenAlreadyHidden() {
        ProductReview review = productReview(10L, 0);
        when(reviewMapper.selectById(10L)).thenReturn(review);

        reviewService.hideReview(10L);

        verify(reviewMapper, never()).updateById(any(ProductReview.class));
    }

    private ReviewDTO reviewDTO(Long orderId, Long productId, Integer rating, String content, String images) {
        ReviewDTO dto = new ReviewDTO();
        dto.setOrderId(orderId);
        dto.setProductId(productId);
        dto.setRating(rating);
        dto.setContent(content);
        dto.setImages(images);
        dto.setIsAnonymous(false);
        return dto;
    }

    private ReviewReplyDTO replyDTO(String content) {
        ReviewReplyDTO dto = new ReviewReplyDTO();
        dto.setContent(content);
        return dto;
    }

    private ProductReview productReview(Long reviewId, Integer status) {
        ProductReview review = new ProductReview();
        review.setId(reviewId);
        review.setProductId(20L);
        review.setUserId(100L);
        review.setOrderId(1L);
        review.setRating(5);
        review.setContent("测试评价");
        review.setIsAnonymous(0);
        review.setStatus(status);
        return review;
    }

    private ReviewReply reviewReply(Long replyId, Long reviewId, Long userId, String content, LocalDateTime createTime) {
        ReviewReply reply = new ReviewReply();
        reply.setId(replyId);
        reply.setReviewId(reviewId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setCreateTime(createTime);
        return reply;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<OrderItem> anyOrderItemQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<ProductReview> anyReviewQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<ReviewReply> anyReplyQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private Page<ProductReview> anyReviewPage() {
        return any(Page.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
