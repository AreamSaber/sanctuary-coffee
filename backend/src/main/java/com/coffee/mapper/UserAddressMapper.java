package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户地址Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
