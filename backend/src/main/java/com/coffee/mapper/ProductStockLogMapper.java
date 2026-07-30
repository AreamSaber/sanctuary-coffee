package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.entity.ProductStockLog;
import com.coffee.vo.StockLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存变更记录Mapper
 */
@Mapper
public interface ProductStockLogMapper extends BaseMapper<ProductStockLog> {
    
    /**
     * 分页查询库存变更记录（带商品名称）
     */
    @Select("<script>" +
            "SELECT l.*, p.product_name, s.sku_name, s.spec_info, u.nickname as operator_name " +
            "FROM product_stock_log l " +
            "LEFT JOIN product p ON l.product_id = p.id " +
            "LEFT JOIN product_sku s ON l.sku_id = s.id " +
            "LEFT JOIN sys_user u ON l.operator_id = u.id " +
            "<where>" +
            "  <if test='productId != null'> AND l.product_id = #{productId} </if>" +
            "  <if test='skuId != null'> AND l.sku_id = #{skuId} </if>" +
            "  <if test='changeType != null'> AND l.change_type = #{changeType} </if>" +
            "</where>" +
            "ORDER BY l.create_time DESC" +
            "</script>")
    IPage<StockLogVO> selectStockLogPage(Page<StockLogVO> page, 
                                          @Param("productId") Long productId,
                                          @Param("skuId") Long skuId,
                                          @Param("changeType") Integer changeType);
}
