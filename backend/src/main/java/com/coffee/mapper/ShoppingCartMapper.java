package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.ShoppingCart;
import com.coffee.vo.CartItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
    
    @Select("SELECT c.*, p.product_name, " +
            "COALESCE(s.image, p.main_image) AS main_image, " +
            "COALESCE(s.price, p.price) AS price, " +
            "COALESCE(s.stock, p.stock) AS stock, " +
            "s.sku_name, s.spec_info " +
            "FROM shopping_cart c " +
            "LEFT JOIN product p ON c.product_id = p.id " +
            "LEFT JOIN product_sku s ON c.sku_id = s.id AND s.deleted = 0 " +
            "WHERE c.user_id = #{userId} " +
            "ORDER BY c.update_time DESC")
    List<CartItemVO> selectCartItemsByUserId(@Param("userId") Long userId);
}
