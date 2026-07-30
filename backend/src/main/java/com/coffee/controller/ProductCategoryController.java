package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.dto.CategoryDTO;
import com.coffee.entity.ProductCategory;
import com.coffee.service.ProductCategoryService;
import com.coffee.vo.CategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "商品分类管理", description = "商品分类管理接口")
@RestController
@RequestMapping("/product/category")
@RequiredArgsConstructor
public class ProductCategoryController {
    
    private final ProductCategoryService categoryService;
    
    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> getCategoryTree() {
        List<CategoryTreeVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }
    
    @Operation(summary = "获取分类列表")
    @GetMapping("/list")
    public Result<List<ProductCategory>> getCategoryList(@RequestParam(required = false) Long parentId) {
        List<ProductCategory> list = categoryService.getCategoryList(parentId);
        return Result.success(list);
    }
    
    @Operation(summary = "添加分类")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:category')")
    public Result<Void> addCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.addCategory(categoryDTO);
        return Result.success("添加成功", null);
    }
    
    @Operation(summary = "更新分类")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:category')")
    public Result<Void> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.updateCategory(categoryDTO);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:category')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功", null);
    }
}
