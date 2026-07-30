package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.UserAddress;
import com.coffee.mapper.UserAddressMapper;
import com.coffee.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务实现类
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
    
    private final UserAddressMapper userAddressMapper;
    
    @Override
    public List<UserAddress> getUserAddressList(Long userId) {
        return userAddressMapper.selectList(
            new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime)
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAddress(Long userId, UserAddress address) {
        address.setUserId(userId);
        
        // 如果是默认地址，先取消其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            cancelOtherDefaultAddress(userId);
        } else {
            // 如果是第一个地址，自动设为默认
            long count = userAddressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
            );
            if (count == 0) {
                address.setIsDefault(1);
            }
        }
        
        userAddressMapper.insert(address);
        log.info("添加用户地址成功: userId={}", userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, UserAddress address) {
        UserAddress existAddress = userAddressMapper.selectById(address.getId());
        if (existAddress == null || !existAddress.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_EXIST);
        }
        
        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            cancelOtherDefaultAddress(userId);
        }
        
        address.setUserId(userId);
        userAddressMapper.updateById(address);
        log.info("更新用户地址成功: userId={}, addressId={}", userId, address.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_EXIST);
        }
        
        userAddressMapper.deleteById(addressId);
        log.info("删除用户地址成功: userId={}, addressId={}", userId, addressId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_EXIST);
        }
        
        // 取消其他默认地址
        cancelOtherDefaultAddress(userId);
        
        // 设置当前地址为默认
        address.setIsDefault(1);
        userAddressMapper.updateById(address);
        log.info("设置默认地址成功: userId={}, addressId={}", userId, addressId);
    }
    
    @Override
    public UserAddress getDefaultAddress(Long userId) {
        return userAddressMapper.selectOne(
            new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
        );
    }
    
    /**
     * 取消其他默认地址
     */
    private void cancelOtherDefaultAddress(Long userId) {
        userAddressMapper.update(null,
            new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0)
        );
    }
}
