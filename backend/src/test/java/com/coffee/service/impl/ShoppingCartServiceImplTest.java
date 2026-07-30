package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.dto.CartItemDTO;
import com.coffee.entity.ShoppingCart;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.ShoppingCartMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.support.TradeInventoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceImplTest {

    @Mock
    private ShoppingCartMapper cartMapper;

    @Mock
    private TradeInventoryService tradeInventoryService;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private ShoppingCartServiceImpl shoppingCartService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(ShoppingCart.class);
        initTableInfo(UserBehavior.class);
    }

    @Test
    void addToCartRecordsAddCartBehaviorForNewItem() {
        CartItemDTO dto = cartItemDTO(20L, 200L, 2);
        when(cartMapper.selectOne(anyShoppingCartQueryWrapper())).thenReturn(null);

        shoppingCartService.addToCart(100L, dto);

        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());

        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals(100L, behavior.getUserId());
        assertEquals("ADD_CART", behavior.getActionType());
        assertEquals("PRODUCT", behavior.getTargetType());
        assertEquals(20L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"addQuantity\":2"));
        assertTrue(behavior.getActionData().contains("\"cartQuantity\":2"));
        assertTrue(behavior.getActionData().contains("\"mergedExistingItem\":false"));
    }

    @Test
    void addToCartRecordsAddCartBehaviorForMergedItem() {
        CartItemDTO dto = cartItemDTO(20L, 200L, 2);
        ShoppingCart existCart = new ShoppingCart();
        existCart.setId(10L);
        existCart.setUserId(100L);
        existCart.setProductId(20L);
        existCart.setSkuId(200L);
        existCart.setQuantity(3);
        when(cartMapper.selectOne(anyShoppingCartQueryWrapper())).thenReturn(existCart);

        shoppingCartService.addToCart(100L, dto);

        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());

        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals("ADD_CART", behavior.getActionType());
        assertEquals("PRODUCT", behavior.getTargetType());
        assertEquals(20L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"addQuantity\":2"));
        assertTrue(behavior.getActionData().contains("\"cartQuantity\":5"));
        assertTrue(behavior.getActionData().contains("\"mergedExistingItem\":true"));
    }

    private CartItemDTO cartItemDTO(Long productId, Long skuId, Integer quantity) {
        CartItemDTO dto = new CartItemDTO();
        dto.setProductId(productId);
        dto.setSkuId(skuId);
        dto.setQuantity(quantity);
        return dto;
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
