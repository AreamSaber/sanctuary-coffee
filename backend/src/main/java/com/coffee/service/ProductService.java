package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.dto.ProductDTO;
import com.coffee.entity.Product;
import com.coffee.vo.ProductVO;

/**
 * 商品服务接口
 * 
 * @author Coffee Shop Team
 */
public interface ProductService extends IService<Product> {
    
    /**
     * 分页查询商品列表
     */
    IPage<ProductVO> getProductPage(Integer pageNum, Integer pageSize, String keyword, Long categoryId, Integer status);
    
    /**
     * 获取商品详情
     */
    ProductVO getProductDetail(Long id);
    
    /**
     * 添加商品
     */
    void addProduct(ProductDTO productDTO);
    
    /**
     * 更新商品
     */
    void updateProduct(ProductDTO productDTO);
    
    /**
     * 删除商品
     */
    void deleteProduct(Long id);
    
    /**
     * 上架商品
     */
    void onShelf(Long id);
    
    /**
     * 下架商品
     */
    void offShelf(Long id);
    
    /**
     * 更新商品库存
     */
    void updateStock(Long id, Long skuId, Integer stock, Long operatorId);
    
    /**
     * 补货（带记录）
     */
    void restock(Long id, Long skuId, Integer quantity, String remark, Long operatorId);
}
