package com.coffee.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.CartItemDTO;
import com.coffee.entity.ShoppingCart;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.ShoppingCartMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.ShoppingCartService;
import com.coffee.service.support.TradeInventoryService;
import com.coffee.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartMapper cartMapper;
    private final TradeInventoryService tradeInventoryService;
    private final AnalyticsService analyticsService;

    @Override
    public List<CartItemVO> getCartList(Long userId) {
        List<CartItemVO> list = cartMapper.selectCartItemsByUserId(userId);
        list.forEach(item -> {
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setSubtotal(subtotal);
        });
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long userId, CartItemDTO cartItemDTO) {
        tradeInventoryService.validateSaleQuantity(cartItemDTO.getProductId(), cartItemDTO.getSkuId(), cartItemDTO.getQuantity());

        ShoppingCart existCart = cartMapper.selectOne(
            new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId)
                .eq(ShoppingCart::getProductId, cartItemDTO.getProductId())
                .eq(cartItemDTO.getSkuId() != null, ShoppingCart::getSkuId, cartItemDTO.getSkuId())
                .isNull(cartItemDTO.getSkuId() == null, ShoppingCart::getSkuId)
        );

        if (existCart != null) {
            int newQuantity = existCart.getQuantity() + cartItemDTO.getQuantity();
            tradeInventoryService.validateSaleQuantity(cartItemDTO.getProductId(), cartItemDTO.getSkuId(), newQuantity);
            existCart.setQuantity(newQuantity);
            cartMapper.updateById(existCart);
            recordAddCartBehavior(userId, cartItemDTO, newQuantity, true);
            return;
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setProductId(cartItemDTO.getProductId());
        cart.setSkuId(cartItemDTO.getSkuId());
        cart.setQuantity(cartItemDTO.getQuantity());
        cart.setChecked(1);
        cartMapper.insert(cart);
        recordAddCartBehavior(userId, cartItemDTO, cartItemDTO.getQuantity(), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        tradeInventoryService.validateSaleQuantity(cart.getProductId(), cart.getSkuId(), quantity);
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromCart(Long userId, Long cartId) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart != null && cart.getUserId().equals(userId)) {
            cartMapper.deleteById(cartId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        cartMapper.delete(
            new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId)
        );
    }

    private void recordAddCartBehavior(Long userId, CartItemDTO cartItemDTO, Integer cartQuantity, boolean mergedExistingItem) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType("ADD_CART");
        behavior.setTargetType("PRODUCT");
        behavior.setTargetId(cartItemDTO.getProductId());
        behavior.setPageUrl("/shop");
        behavior.setActionData(JSONUtil.createObj()
                .set("productId", cartItemDTO.getProductId())
                .set("skuId", cartItemDTO.getSkuId())
                .set("addQuantity", cartItemDTO.getQuantity())
                .set("cartQuantity", cartQuantity)
                .set("mergedExistingItem", mergedExistingItem)
                .toString());
        analyticsService.recordUserBehavior(behavior);
    }
}
