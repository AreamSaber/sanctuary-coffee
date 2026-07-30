package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.Product;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.UserBehaviorMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.vo.UserAnalyticsVO.ProductPreference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private UserBehaviorMapper behaviorMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProductMapper productMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    private AnalyticsServiceImpl analyticsService;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisConfiguration cfg = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
        initTableInfo(assistant, UserBehavior.class);
        initTableInfo(assistant, Order.class);
        initTableInfo(assistant, OrderItem.class);
    }

    private static void initTableInfo(MapperBuilderAssistant assistant, Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(
            behaviorMapper,
            userMapper,
            productMapper,
            orderMapper,
            orderItemMapper
        );
    }

    @Test
    void productPreferencesUsePaidOrderItemsForPurchaseCount() {
        Long latteId = 20L;
        Long mochaId = 21L;

        when(behaviorMapper.selectList(any())).thenReturn(List.of(
            behavior(1L, "VIEW", latteId),
            behavior(2L, "VIEW", latteId),
            behavior(1L, "ADD_CART", latteId)
        ));

        Order paidOrder = new Order();
        paidOrder.setId(100L);
        paidOrder.setPayStatus(1);
        paidOrder.setPayTime(LocalDateTime.now().minusDays(1));
        when(orderMapper.selectList(any())).thenReturn(List.of(paidOrder));

        when(orderItemMapper.selectList(any())).thenReturn(List.of(
            orderItem(100L, latteId, 3, "45.00"),
            orderItem(100L, mochaId, 1, "18.00")
        ));

        when(productMapper.selectById(latteId)).thenReturn(product(latteId, "拿铁"));
        when(productMapper.selectById(mochaId)).thenReturn(product(mochaId, "摩卡"));

        List<ProductPreference> preferences = analyticsService.getHotProducts(10);

        assertEquals(2, preferences.size());

        ProductPreference latte = preferences.get(0);
        assertEquals(latteId, latte.getProductId());
        assertEquals(2, latte.getViewCount());
        assertEquals(1, latte.getAddToCartCount());
        assertEquals(3, latte.getPurchaseCount());
        assertEquals(new BigDecimal("45.00"), latte.getRevenue());
        assertEquals(new BigDecimal("150.00"), latte.getConversionRate());

        ProductPreference mocha = preferences.get(1);
        assertEquals(mochaId, mocha.getProductId());
        assertEquals(0, mocha.getViewCount());
        assertEquals(0, mocha.getAddToCartCount());
        assertEquals(1, mocha.getPurchaseCount());
        assertEquals(new BigDecimal("18.00"), mocha.getRevenue());
        assertEquals(BigDecimal.ZERO, mocha.getConversionRate());
    }

    private UserBehavior behavior(Long userId, String actionType, Long productId) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType(actionType);
        behavior.setTargetType("PRODUCT");
        behavior.setTargetId(productId);
        behavior.setCreateTime(LocalDateTime.now().minusHours(1));
        return behavior;
    }

    private OrderItem orderItem(Long orderId, Long productId, Integer quantity, String totalAmount) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setTotalAmount(new BigDecimal(totalAmount));
        return item;
    }

    private Product product(Long id, String productName) {
        Product product = new Product();
        product.setId(id);
        product.setProductName(productName);
        return product;
    }
}
