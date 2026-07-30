package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.OrderCreateDTO;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryOrder;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.ShoppingCart;
import com.coffee.entity.UserAddress;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.DeliveryMethodMapper;
import com.coffee.mapper.DeliveryOrderMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.ShoppingCartMapper;
import com.coffee.mapper.UserAddressMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.PaymentService;
import com.coffee.service.support.TradeInventoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ShoppingCartMapper cartMapper;

    @Mock
    private UserAddressMapper addressMapper;

    @Mock
    private DeliveryMethodMapper deliveryMethodMapper;

    @Mock
    private DeliveryOrderMapper deliveryOrderMapper;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private TradeInventoryService tradeInventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(Order.class);
        initTableInfo(OrderItem.class);
        initTableInfo(ShoppingCart.class);
        initTableInfo(UserAddress.class);
        initTableInfo(DeliveryMethod.class);
        initTableInfo(DeliveryOrder.class);
        initTableInfo(UserBehavior.class);
    }

    @Test
    void createOrderStoresSelectedDeliveryMethodAndFreight() {
        Long userId = 100L;
        OrderCreateDTO dto = orderCreateDTO(1L, List.of(10L), 2L);
        UserAddress address = userAddress(1L, userId);
        ShoppingCart cartItem = shoppingCart(10L, userId, 20L, null, 2);
        DeliveryMethod method = deliveryMethod(2L, "快速配送", new BigDecimal("15.00"), new BigDecimal("100.00"));
        TradeInventoryService.ResolvedTradeItem resolvedItem = new TradeInventoryService.ResolvedTradeItem(
            20L,
            null,
            "测试咖啡",
            "/image.jpg",
            null,
            new BigDecimal("30.00"),
            20
        );

        when(addressMapper.selectById(1L)).thenReturn(address);
        when(cartMapper.selectList(anyShoppingCartQueryWrapper())).thenReturn(List.of(cartItem));
        when(deliveryMethodMapper.selectById(2L)).thenReturn(method);
        when(tradeInventoryService.resolveTradeItem(20L, null)).thenReturn(resolvedItem);

        orderService.createOrder(userId, dto);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order order = orderCaptor.getValue();
        assertEquals(new BigDecimal("60.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("15.00"), order.getFreightAmount());
        assertEquals(new BigDecimal("75.00"), order.getPayAmount());
        assertEquals(2L, order.getDeliveryMethodId());
        assertEquals("快速配送", order.getDeliveryMethodName());

        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals(userId, behavior.getUserId());
        assertEquals("ORDER", behavior.getActionType());
        assertEquals("ORDER", behavior.getTargetType());
        assertEquals("/order/confirm", behavior.getPageUrl());
        assertTrue(behavior.getActionData().contains("\"deliveryMethodName\":\"快速配送\""));
        assertTrue(behavior.getActionData().contains("\"itemCount\":1"));
    }

    @Test
    void createOrderWaivesFreightWhenAmountReachesFreeThreshold() {
        Long userId = 100L;
        OrderCreateDTO dto = orderCreateDTO(1L, List.of(10L), 2L);
        UserAddress address = userAddress(1L, userId);
        ShoppingCart cartItem = shoppingCart(10L, userId, 20L, null, 2);
        DeliveryMethod method = deliveryMethod(2L, "标准配送", new BigDecimal("8.00"), new BigDecimal("50.00"));
        TradeInventoryService.ResolvedTradeItem resolvedItem = new TradeInventoryService.ResolvedTradeItem(
            20L,
            null,
            "测试咖啡",
            "/image.jpg",
            null,
            new BigDecimal("30.00"),
            20
        );

        when(addressMapper.selectById(1L)).thenReturn(address);
        when(cartMapper.selectList(anyShoppingCartQueryWrapper())).thenReturn(List.of(cartItem));
        when(deliveryMethodMapper.selectById(2L)).thenReturn(method);
        when(tradeInventoryService.resolveTradeItem(20L, null)).thenReturn(resolvedItem);

        orderService.createOrder(userId, dto);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order order = orderCaptor.getValue();
        assertEquals(new BigDecimal("60.00"), order.getTotalAmount());
        assertEquals(BigDecimal.ZERO, order.getFreightAmount());
        assertEquals(new BigDecimal("60.00"), order.getPayAmount());
        assertEquals(2L, order.getDeliveryMethodId());
        assertEquals("标准配送", order.getDeliveryMethodName());
    }

    @Test
    void createOrderRejectsDisabledDeliveryMethod() {
        Long userId = 100L;
        OrderCreateDTO dto = orderCreateDTO(1L, List.of(10L), 2L);
        UserAddress address = userAddress(1L, userId);
        ShoppingCart cartItem = shoppingCart(10L, userId, 20L, null, 1);
        DeliveryMethod method = deliveryMethod(2L, "停用配送", new BigDecimal("8.00"), BigDecimal.ZERO);
        method.setStatus(0);

        when(addressMapper.selectById(1L)).thenReturn(address);
        when(cartMapper.selectList(anyShoppingCartQueryWrapper())).thenReturn(List.of(cartItem));
        when(deliveryMethodMapper.selectById(2L)).thenReturn(method);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> orderService.createOrder(userId, dto)
        );
        assertEquals("配送方式不可用", exception.getMessage());
    }

    private OrderCreateDTO orderCreateDTO(Long addressId, List<Long> cartIds, Long deliveryMethodId) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setAddressId(addressId);
        dto.setCartIds(cartIds);
        dto.setDeliveryMethodId(deliveryMethodId);
        dto.setRemark("测试备注");
        return dto;
    }

    private UserAddress userAddress(Long id, Long userId) {
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setUserId(userId);
        address.setReceiverName("测试用户");
        address.setReceiverPhone("13800000000");
        address.setProvince("广东省");
        address.setCity("广州市");
        address.setDistrict("天河区");
        address.setDetailAddress("测试路 1 号");
        return address;
    }

    private ShoppingCart shoppingCart(Long id, Long userId, Long productId, Long skuId, Integer quantity) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setSkuId(skuId);
        cart.setQuantity(quantity);
        return cart;
    }

    private DeliveryMethod deliveryMethod(Long id, String methodName, BigDecimal freight, BigDecimal freeThreshold) {
        DeliveryMethod method = new DeliveryMethod();
        method.setId(id);
        method.setMethodName(methodName);
        method.setFreight(freight);
        method.setFreeThreshold(freeThreshold);
        method.setStatus(1);
        return method;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<ShoppingCart> anyShoppingCartQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
