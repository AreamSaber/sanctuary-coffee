package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评价Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {
}
