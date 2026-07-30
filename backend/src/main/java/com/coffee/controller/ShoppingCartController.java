package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.CartItemDTO;
import com.coffee.vo.CartItemVO;
import com.coffee.entity.ShoppingCart;
import com.coffee.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "购物车管理")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class ShoppingCartController {
    
    private final ShoppingCartService cartService;
    
    @Operation(summary = "获取购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemVO>> getCartList() {
        Long userId = getCurrentUserId();
        List<CartItemVO> list = cartService.getCartList(userId);
        return Result.success(list);
    }
    
    @Operation(summary = "添加到购物车")
    @PostMapping
    public Result<Void> addToCart(@Valid @RequestBody CartItemDTO cartItemDTO) {
        Long userId = getCurrentUserId();
        cartService.addToCart(userId, cartItemDTO);
        return Result.success("添加成功", null);
    }
    
    @Operation(summary = "更新数量")
    @PutMapping("/{id}/quantity")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        Long userId = getCurrentUserId();
        cartService.updateQuantity(userId, id, quantity);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "移除商品")
    @DeleteMapping("/{id}")
    public Result<Void> removeFromCart(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        cartService.removeFromCart(userId, id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clearCart() {
        Long userId = getCurrentUserId();
        cartService.clearCart(userId);
        return Result.success("清空成功", null);
    }
    
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
