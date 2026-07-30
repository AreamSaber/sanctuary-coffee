package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.entity.Product;
import com.coffee.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商品Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    
    /**
     * 分页查询商品列表（带分类名称）
     */
    @Select("SELECT p.*, c.category_name FROM product p " +
            "LEFT JOIN product_category c ON p.category_id = c.id " +
            "WHERE p.deleted = 0 " +
            "AND (#{keyword} IS NULL OR p.product_name LIKE CONCAT('%', #{keyword}, '%') OR p.product_code LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{categoryId} IS NULL OR p.category_id = #{categoryId}) " +
            "AND (#{status} IS NULL OR p.status = #{status}) " +
            "ORDER BY p.create_time DESC")
    IPage<ProductVO> selectProductPage(Page<ProductVO> page, 
                                       @Param("keyword") String keyword,
                                       @Param("categoryId") Long categoryId,
                                       @Param("status") Integer status);
    
    /**
     * 根据ID查询商品详情（带分类名称）
     */
    @Select("SELECT p.*, c.category_name FROM product p " +
            "LEFT JOIN product_category c ON p.category_id = c.id " +
            "WHERE p.id = #{id} AND p.deleted = 0")
    ProductVO selectProductVOById(@Param("id") Long id);
    
    /**
     * 原子扣减库存（防止超卖）
     * 使用数据库级别的原子操作，确保并发安全
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 影响行数，0表示库存不足或商品不存在
     */
    @Update("UPDATE product SET stock = stock - #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND deleted = 0 AND status = 1")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    /**
     * 原子恢复库存
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 影响行数
     */
    @Update("UPDATE product SET stock = stock + #{quantity} " +
            "WHERE id = #{productId} AND deleted = 0")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    /**
     * 原子增加销量
     * @param productId 商品ID
     * @param quantity 销量增加数
     * @return 影响行数
     */
    @Update("UPDATE product SET sales = sales + #{quantity} " +
            "WHERE id = #{productId} AND deleted = 0")
    int increaseSales(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    /**
     * 原子减少销量（用于退款）
     * @param productId 商品ID
     * @param quantity 销量减少数
     * @return 影响行数
     */
    @Update("UPDATE product SET sales = CASE WHEN sales >= #{quantity} THEN sales - #{quantity} ELSE 0 END " +
            "WHERE id = #{productId} AND deleted = 0")
    int decreaseSales(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock - #{quantity}, locked_stock = locked_stock + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND deleted = 0 AND status = 1")
    int lockStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock + #{quantity}, locked_stock = locked_stock - #{quantity} " +
            "WHERE id = #{productId} AND locked_stock >= #{quantity} AND deleted = 0")
    int releaseLockedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET locked_stock = locked_stock - #{quantity} " +
            "WHERE id = #{productId} AND locked_stock >= #{quantity} AND deleted = 0")
    int confirmLockedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
