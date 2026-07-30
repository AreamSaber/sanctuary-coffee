package com.coffee.service;

import com.coffee.dto.CartItemDTO;
import com.coffee.vo.CartItemVO;
import java.util.List;

public interface ShoppingCartService {
    
    List<CartItemVO> getCartList(Long userId);
    
    void addToCart(Long userId, CartItemDTO cartItemDTO);
    
    void updateQuantity(Long userId, Long cartId, Integer quantity);
    
    void removeFromCart(Long userId, Long cartId);
    
    void clearCart(Long userId);
}
