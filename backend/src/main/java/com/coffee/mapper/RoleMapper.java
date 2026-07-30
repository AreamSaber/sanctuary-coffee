package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
