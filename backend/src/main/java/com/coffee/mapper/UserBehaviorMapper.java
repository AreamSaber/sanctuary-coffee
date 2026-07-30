package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
}
