package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.entity.UserAddress;
import com.coffee.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户地址控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "用户地址管理", description = "用户收货地址管理")
@RestController
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class UserAddressController {
    
    private final UserAddressService userAddressService;
    
    @Operation(summary = "获取地址列表")
    @GetMapping("/list")
    public Result<List<UserAddress>> getAddressList() {
        Long userId = getCurrentUserId();
        List<UserAddress> addressList = userAddressService.getUserAddressList(userId);
        return Result.success(addressList);
    }
    
    @Operation(summary = "添加地址")
    @PostMapping
    public Result<Void> addAddress(@Valid @RequestBody UserAddress address) {
        Long userId = getCurrentUserId();
        userAddressService.addAddress(userId, address);
        return Result.success("添加成功", null);
    }
    
    @Operation(summary = "更新地址")
    @PutMapping
    public Result<Void> updateAddress(@Valid @RequestBody UserAddress address) {
        Long userId = getCurrentUserId();
        userAddressService.updateAddress(userId, address);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        userAddressService.deleteAddress(userId, id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "设置默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        userAddressService.setDefaultAddress(userId, id);
        return Result.success("设置成功", null);
    }
    
    @Operation(summary = "获取默认地址")
    @GetMapping("/default")
    public Result<UserAddress> getDefaultAddress() {
        Long userId = getCurrentUserId();
        UserAddress address = userAddressService.getDefaultAddress(userId);
        return Result.success(address);
    }
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
