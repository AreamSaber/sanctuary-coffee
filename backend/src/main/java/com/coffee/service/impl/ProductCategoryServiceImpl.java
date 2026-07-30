package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.CategoryDTO;
import com.coffee.entity.Product;
import com.coffee.entity.ProductCategory;
import com.coffee.mapper.ProductCategoryMapper;
import com.coffee.mapper.ProductMapper;
import com.coffee.service.ProductCategoryService;
import com.coffee.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
    
    private final ProductCategoryMapper categoryMapper;

    private final ProductMapper productMapper;
    
    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        // 查询所有分类
        List<ProductCategory> allCategories = categoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByAsc(ProductCategory::getCreateTime)
        );
        
        // 转换为VO
        List<CategoryTreeVO> allCategoryVOs = allCategories.stream()
                .map(category -> BeanUtil.copyProperties(category, CategoryTreeVO.class))
                .collect(Collectors.toList());
        
        // 构建树形结构
        return buildTree(allCategoryVOs, 0L);
    }
    
    @Override
    public List<ProductCategory> getCategoryList(Long parentId) {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategory>()
                .eq(parentId != null, ProductCategory::getParentId, parentId)
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByAsc(ProductCategory::getCreateTime)
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCategory(CategoryDTO categoryDTO) {
        // 检查分类编码是否已存在
        ProductCategory existCategory = categoryMapper.selectOne(
            new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getCategoryCode, categoryDTO.getCategoryCode())
        );
        if (existCategory != null) {
            throw new BusinessException("分类编码已存在");
        }
        
        ProductCategory category = BeanUtil.copyProperties(categoryDTO, ProductCategory.class);
        
        // 设置默认值
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        // 计算层级
        if (category.getParentId() == 0) {
            category.setLevel(1);
        } else {
            ProductCategory parent = categoryMapper.selectById(category.getParentId());
            if (parent != null) {
                category.setLevel(parent.getLevel() + 1);
            } else {
                category.setLevel(1);
            }
        }
        
        categoryMapper.insert(category);
        log.info("添加分类成功: {}", category.getCategoryName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategoryDTO categoryDTO) {
        ProductCategory category = categoryMapper.selectById(categoryDTO.getId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 如果修改了分类编码，检查是否重复
        if (!category.getCategoryCode().equals(categoryDTO.getCategoryCode())) {
            ProductCategory existCategory = categoryMapper.selectOne(
                new LambdaQueryWrapper<ProductCategory>()
                    .eq(ProductCategory::getCategoryCode, categoryDTO.getCategoryCode())
                    .ne(ProductCategory::getId, categoryDTO.getId())
            );
            if (existCategory != null) {
                throw new BusinessException("分类编码已存在");
            }
        }
        
        BeanUtil.copyProperties(categoryDTO, category, "id", "level", "createTime");
        categoryMapper.updateById(category);
        log.info("更新分类成功: {}", category.getCategoryName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        ProductCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 检查是否有子分类
        Long childCount = categoryMapper.selectCount(
            new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }
        
        // 检查是否已有商品引用该分类
        Long productCount = productMapper.selectCount(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id)
        );
        if (productCount > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }

        categoryMapper.deleteById(id);
        log.info("删除分类成功: {}", category.getCategoryName());
    }
    
    /**
     * 构建树形结构
     */
    private List<CategoryTreeVO> buildTree(List<CategoryTreeVO> allCategories, Long parentId) {
        List<CategoryTreeVO> tree = new ArrayList<>();
        
        for (CategoryTreeVO category : allCategories) {
            if (category.getParentId().equals(parentId)) {
                List<CategoryTreeVO> children = buildTree(allCategories, category.getId());
                category.setChildren(children);
                tree.add(category);
            }
        }
        
        return tree;
    }
}
