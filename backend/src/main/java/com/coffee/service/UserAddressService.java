package com.coffee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.entity.UserAddress;

import java.util.List;

/**
 * 用户地址服务接口
 * 
 * @author Coffee Shop Team
 */
public interface UserAddressService extends IService<UserAddress> {
    
    /**
     * 获取用户地址列表
     */
    List<UserAddress> getUserAddressList(Long userId);
    
    /**
     * 添加地址
     */
    void addAddress(Long userId, UserAddress address);
    
    /**
     * 更新地址
     */
    void updateAddress(Long userId, UserAddress address);
    
    /**
     * 删除地址
     */
    void deleteAddress(Long userId, Long addressId);
    
    /**
     * 设置默认地址
     */
    void setDefaultAddress(Long userId, Long addressId);
    
    /**
     * 获取默认地址
     */
    UserAddress getDefaultAddress(Long userId);
}
