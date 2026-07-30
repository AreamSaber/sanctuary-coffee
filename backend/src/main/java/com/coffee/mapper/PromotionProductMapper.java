package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.PromotionProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 促销活动商品关联Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface PromotionProductMapper extends BaseMapper<PromotionProduct> {
}
