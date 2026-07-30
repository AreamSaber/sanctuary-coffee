package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.ProductStockLogMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.ProductService;
import com.coffee.vo.ProductVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductStockLogMapper stockLogMapper;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private ProductController productController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProductPageRecordsSearchBehaviorWhenKeywordProvided() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails(100L), null, List.of())
        );
        Page<ProductVO> page = new Page<>(1, 10, 3);
        when(productService.getProductPage(1, 10, "拿铁", 2L, 1)).thenReturn(page);

        productController.getProductPage(1, 10, "拿铁", 2L, 1);

        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals(100L, behavior.getUserId());
        assertEquals("SEARCH", behavior.getActionType());
        assertEquals("CATEGORY", behavior.getTargetType());
        assertEquals(2L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"keyword\":\"拿铁\""));
        assertTrue(behavior.getActionData().contains("\"resultCount\":3"));
    }

    @Test
    void getProductDetailRecordsProductViewBehavior() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails(100L), null, List.of())
        );
        ProductVO product = new ProductVO();
        product.setId(20L);
        product.setProductName("测试咖啡");
        product.setCategoryId(2L);
        when(productService.getProductDetail(20L)).thenReturn(product);

        productController.getProductDetail(20L);

        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals(100L, behavior.getUserId());
        assertEquals("VIEW", behavior.getActionType());
        assertEquals("PRODUCT", behavior.getTargetType());
        assertEquals(20L, behavior.getTargetId());
        assertEquals("/shop/product/20", behavior.getPageUrl());
        assertTrue(behavior.getActionData().contains("\"productName\":\"测试咖啡\""));
    }

    private com.coffee.security.JwtUserDetails userDetails(Long userId) {
        return new com.coffee.security.JwtUserDetails(
                userId,
                "test_user",
                "password",
                "测试用户",
                "test@example.com",
                List.of()
        );
    }
}
