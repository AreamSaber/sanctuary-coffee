package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.MemberLevelBenefit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级权益绑定 Mapper
 */
@Mapper
public interface MemberLevelBenefitMapper extends BaseMapper<MemberLevelBenefit> {
}
