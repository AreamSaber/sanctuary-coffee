package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
