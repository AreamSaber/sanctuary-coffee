package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品SKUMapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Update("UPDATE product_sku SET stock = stock - #{quantity} " +
            "WHERE id = #{skuId} AND stock >= #{quantity} AND deleted = 0 AND status = 1")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("UPDATE product_sku SET stock = stock + #{quantity} " +
            "WHERE id = #{skuId} AND deleted = 0")
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("UPDATE product_sku SET stock = stock - #{quantity}, locked_stock = locked_stock + #{quantity} " +
            "WHERE id = #{skuId} AND stock >= #{quantity} AND deleted = 0 AND status = 1")
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("UPDATE product_sku SET stock = stock + #{quantity}, locked_stock = locked_stock - #{quantity} " +
            "WHERE id = #{skuId} AND locked_stock >= #{quantity} AND deleted = 0")
    int releaseLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("UPDATE product_sku SET locked_stock = locked_stock - #{quantity} " +
            "WHERE id = #{skuId} AND locked_stock >= #{quantity} AND deleted = 0")
    int confirmLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
