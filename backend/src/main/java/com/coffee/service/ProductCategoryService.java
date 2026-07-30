package com.coffee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.dto.CategoryDTO;
import com.coffee.entity.ProductCategory;
import com.coffee.vo.CategoryTreeVO;

import java.util.List;

/**
 * 商品分类服务接口
 * 
 * @author Coffee Shop Team
 */
public interface ProductCategoryService extends IService<ProductCategory> {
    
    /**
     * 获取分类树
     */
    List<CategoryTreeVO> getCategoryTree();
    
    /**
     * 获取分类列表
     */
    List<ProductCategory> getCategoryList(Long parentId);
    
    /**
     * 添加分类
     */
    void addCategory(CategoryDTO categoryDTO);
    
    /**
     * 更新分类
     */
    void updateCategory(CategoryDTO categoryDTO);
    
    /**
     * 删除分类
     */
    void deleteCategory(Long id);
}
