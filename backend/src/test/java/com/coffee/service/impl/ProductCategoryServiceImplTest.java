package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.Product;
import com.coffee.entity.ProductCategory;
import com.coffee.mapper.ProductCategoryMapper;
import com.coffee.mapper.ProductMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceImplTest {

    @Mock
    private ProductCategoryMapper categoryMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductCategoryServiceImpl categoryService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(ProductCategory.class);
        initTableInfo(Product.class);
    }

    @Test
    void deleteCategoryRejectsCategoryWithProducts() {
        ProductCategory category = category(10L, "咖啡饮品");
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(categoryMapper.selectCount(anyCategoryQueryWrapper())).thenReturn(0L);
        when(productMapper.selectCount(anyProductQueryWrapper())).thenReturn(2L);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> categoryService.deleteCategory(10L)
        );

        assertEquals(500, exception.getCode());
        assertEquals("该分类下存在商品，无法删除", exception.getMessage());
        verify(categoryMapper, never()).deleteById(10L);
    }

    @Test
    void deleteCategoryAllowsCategoryWithoutChildrenOrProducts() {
        ProductCategory category = category(10L, "咖啡饮品");
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(categoryMapper.selectCount(anyCategoryQueryWrapper())).thenReturn(0L);
        when(productMapper.selectCount(anyProductQueryWrapper())).thenReturn(0L);

        categoryService.deleteCategory(10L);

        verify(categoryMapper).deleteById(10L);
    }

    private ProductCategory category(Long id, String name) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setCategoryName(name);
        category.setCategoryCode("CAT" + id);
        category.setParentId(0L);
        category.setLevel(1);
        return category;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<ProductCategory> anyCategoryQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Product> anyProductQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
