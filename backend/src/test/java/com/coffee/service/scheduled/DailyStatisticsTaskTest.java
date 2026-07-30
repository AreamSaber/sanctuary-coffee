package com.coffee.service.scheduled;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.entity.OperationSummary;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.Product;
import com.coffee.entity.ProductCategory;
import com.coffee.entity.ProductSalesStatistics;
import com.coffee.entity.Refund;
import com.coffee.entity.SalesStatisticsDaily;
import com.coffee.entity.User;
import com.coffee.entity.UserActiveStatistics;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.OperationSummaryMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.PaymentMapper;
import com.coffee.mapper.ProductCategoryMapper;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.ProductSalesStatisticsMapper;
import com.coffee.mapper.RefundMapper;
import com.coffee.mapper.SalesStatisticsDailyMapper;
import com.coffee.mapper.UserActiveStatisticsMapper;
import com.coffee.mapper.UserBehaviorMapper;
import com.coffee.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyStatisticsTaskTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private RefundMapper refundMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductCategoryMapper categoryMapper;
    @Mock private UserBehaviorMapper behaviorMapper;
    @Mock private SalesStatisticsDailyMapper salesStatsMapper;
    @Mock private ProductSalesStatisticsMapper productStatsMapper;
    @Mock private UserActiveStatisticsMapper userStatsMapper;
    @Mock private OperationSummaryMapper operationSummaryMapper;

    private DailyStatisticsTask task;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisConfiguration cfg = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
        initTableInfo(assistant, Order.class);
        initTableInfo(assistant, OrderItem.class);
        initTableInfo(assistant, Refund.class);
        initTableInfo(assistant, User.class);
        initTableInfo(assistant, Product.class);
        initTableInfo(assistant, ProductCategory.class);
        initTableInfo(assistant, UserBehavior.class);
        initTableInfo(assistant, SalesStatisticsDaily.class);
        initTableInfo(assistant, ProductSalesStatistics.class);
        initTableInfo(assistant, UserActiveStatistics.class);
        initTableInfo(assistant, OperationSummary.class);
    }

    private static void initTableInfo(MapperBuilderAssistant assistant, Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }

    @BeforeEach
    void setUp() {
        task = new DailyStatisticsTask(
            orderMapper,
            orderItemMapper,
            refundMapper,
            paymentMapper,
            userMapper,
            productMapper,
            categoryMapper,
            behaviorMapper,
            salesStatsMapper,
            productStatsMapper,
            userStatsMapper,
            operationSummaryMapper
        );
    }

    @Test
    void generateStatisticsForDateUpsertsRowsAndUsesBehaviorMetrics() {
        LocalDate date = LocalDate.of(2026, 5, 2);
        Order paidOrder = order(101L, 1L, "30.00", "25.00", date.atTime(10, 0), 4);
        List<Order> dayOrders = List.of(paidOrder);
        List<Order> completedOrders = List.of(
            order(101L, 1L, "30.00", "25.00", date.atTime(10, 0), 4),
            order(102L, 1L, "12.00", "12.00", date.atTime(11, 0), 4),
            order(103L, 2L, "9.00", "9.00", date.atTime(12, 0), 4)
        );
        OrderItem orderItem = orderItem(101L, 11L, 2, "20.00");
        List<UserBehavior> behaviors = List.of(
            behavior(1L, "VIEW", "PRODUCT", 11L, 5),
            behavior(1L, "ADD_CART", "PRODUCT", 11L, 2),
            behavior(2L, "VIEW", "PRODUCT", 12L, 3),
            behavior(1L, "PAY", "ORDER", 101L, 1)
        );
        Refund refund = new Refund();
        refund.setRefundAmount(new BigDecimal("5.00"));

        SalesStatisticsDaily existingSales = new SalesStatisticsDaily();
        existingSales.setId(501L);
        ProductSalesStatistics existingProduct = new ProductSalesStatistics();
        existingProduct.setId(601L);
        UserActiveStatistics existingUser = new UserActiveStatistics();
        existingUser.setId(701L);
        OperationSummary existingSummary = new OperationSummary();
        existingSummary.setId(801L);

        when(orderMapper.selectList(any())).thenReturn(dayOrders, completedOrders);
        when(behaviorMapper.selectList(any())).thenReturn(behaviors);
        when(refundMapper.selectList(any())).thenReturn(List.of(refund));
        when(userMapper.selectCount(any())).thenReturn(1L, 3L, 1L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem));
        when(salesStatsMapper.selectOne(any())).thenReturn(existingSales);
        when(productStatsMapper.selectOne(any())).thenReturn(existingProduct, null);
        when(userStatsMapper.selectOne(any())).thenReturn(existingUser, null);
        when(operationSummaryMapper.selectOne(any())).thenReturn(existingSummary);
        when(productMapper.selectCount(any())).thenReturn(4L);
        when(categoryMapper.selectCount(any())).thenReturn(2L);

        task.generateStatisticsForDate(date);

        ArgumentCaptor<SalesStatisticsDaily> salesCaptor = ArgumentCaptor.forClass(SalesStatisticsDaily.class);
        verify(salesStatsMapper).updateById(salesCaptor.capture());
        SalesStatisticsDaily sales = salesCaptor.getValue();
        assertEquals(501L, sales.getId());
        assertEquals(1, sales.getOrderCount());
        assertEquals(new BigDecimal("30.00"), sales.getOrderAmount());
        assertEquals(1, sales.getPayCount());
        assertEquals(new BigDecimal("25.00"), sales.getPayAmount());
        assertEquals(1, sales.getRefundCount());
        assertEquals(new BigDecimal("5.00"), sales.getRefundAmount());
        assertEquals(1, sales.getNewUserCount());
        assertEquals(2, sales.getActiveUserCount());

        ArgumentCaptor<ProductSalesStatistics> productUpdateCaptor = ArgumentCaptor.forClass(ProductSalesStatistics.class);
        verify(productStatsMapper).updateById(productUpdateCaptor.capture());
        ProductSalesStatistics updatedProduct = productUpdateCaptor.getValue();
        assertEquals(601L, updatedProduct.getId());
        assertEquals(11L, updatedProduct.getProductId());
        assertEquals(2, updatedProduct.getSalesCount());
        assertEquals(new BigDecimal("20.00"), updatedProduct.getSalesAmount());
        assertEquals(1, updatedProduct.getViewCount());
        assertEquals(1, updatedProduct.getCartCount());

        ArgumentCaptor<ProductSalesStatistics> productInsertCaptor = ArgumentCaptor.forClass(ProductSalesStatistics.class);
        verify(productStatsMapper).insert(productInsertCaptor.capture());
        ProductSalesStatistics insertedProduct = productInsertCaptor.getValue();
        assertEquals(12L, insertedProduct.getProductId());
        assertEquals(0, insertedProduct.getSalesCount());
        assertEquals(BigDecimal.ZERO, insertedProduct.getSalesAmount());
        assertEquals(1, insertedProduct.getViewCount());
        assertEquals(0, insertedProduct.getCartCount());

        ArgumentCaptor<UserActiveStatistics> userUpdateCaptor = ArgumentCaptor.forClass(UserActiveStatistics.class);
        verify(userStatsMapper).updateById(userUpdateCaptor.capture());
        UserActiveStatistics updatedUser = userUpdateCaptor.getValue();
        assertEquals(701L, updatedUser.getId());
        assertEquals(1L, updatedUser.getUserId());
        assertEquals(1, updatedUser.getBrowseCount());
        assertEquals(1, updatedUser.getOrderCount());
        assertEquals(1, updatedUser.getPayCount());
        assertEquals(8, updatedUser.getOnlineTime());

        ArgumentCaptor<UserActiveStatistics> userInsertCaptor = ArgumentCaptor.forClass(UserActiveStatistics.class);
        verify(userStatsMapper).insert(userInsertCaptor.capture());
        UserActiveStatistics insertedUser = userInsertCaptor.getValue();
        assertEquals(2L, insertedUser.getUserId());
        assertEquals(1, insertedUser.getBrowseCount());
        assertEquals(0, insertedUser.getOrderCount());
        assertEquals(0, insertedUser.getPayCount());
        assertEquals(3, insertedUser.getOnlineTime());

        ArgumentCaptor<OperationSummary> summaryCaptor = ArgumentCaptor.forClass(OperationSummary.class);
        verify(operationSummaryMapper).updateById(summaryCaptor.capture());
        OperationSummary summary = summaryCaptor.getValue();
        assertEquals(801L, summary.getId());
        assertEquals(3, summary.getTotalUser());
        assertEquals(1, summary.getNewUser());
        assertEquals(2, summary.getActiveUser());
        assertEquals(1, summary.getTotalOrder());
        assertEquals(new BigDecimal("25.00"), summary.getTotalAmount());
        assertEquals(new BigDecimal("25.00"), summary.getAvgOrderAmount());
        assertEquals(new BigDecimal("0.5000"), summary.getConversionRate());
        assertEquals(new BigDecimal("0.5000"), summary.getRepurchaseRate());
        assertEquals(4, summary.getProductCount());
        assertEquals(2, summary.getCategoryCount());
    }

    private Order order(Long id, Long userId, String totalAmount, String payAmount, LocalDateTime payTime, Integer status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal(totalAmount));
        order.setPayAmount(new BigDecimal(payAmount));
        order.setPayTime(payTime);
        order.setOrderStatus(status);
        return order;
    }

    private OrderItem orderItem(Long orderId, Long productId, Integer quantity, String totalAmount) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setTotalAmount(new BigDecimal(totalAmount));
        return item;
    }

    private UserBehavior behavior(Long userId, String actionType, String targetType, Long targetId, Integer duration) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType(actionType);
        behavior.setTargetType(targetType);
        behavior.setTargetId(targetId);
        behavior.setDuration(duration);
        return behavior;
    }
}
