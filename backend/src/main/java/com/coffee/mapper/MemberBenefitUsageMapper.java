package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.MemberBenefitUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员权益使用流水 Mapper
 */
@Mapper
public interface MemberBenefitUsageMapper extends BaseMapper<MemberBenefitUsage> {
}
