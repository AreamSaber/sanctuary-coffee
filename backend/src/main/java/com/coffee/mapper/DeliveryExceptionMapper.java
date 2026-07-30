package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.DeliveryException;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配送异常记录Mapper
 */
@Mapper
public interface DeliveryExceptionMapper extends BaseMapper<DeliveryException> {
}
