package com.coffee.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.Result;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.ProductDTO;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.ProductStockLogMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.ProductService;
import com.coffee.vo.ProductVO;
import com.coffee.vo.StockLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 商品控制器
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Tag(name = "商品管理", description = "商品信息管理接口")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    private final ProductStockLogMapper stockLogMapper;
    private final AnalyticsService analyticsService;
    
    @Operation(summary = "分页查询商品列表")
    @GetMapping("/page")
    public Result<IPage<ProductVO>> getProductPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        IPage<ProductVO> page = productService.getProductPage(pageNum, pageSize, keyword, categoryId, status);
        recordProductListBehavior(keyword, categoryId, page.getTotal());
        return Result.success(page);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO productVO = productService.getProductDetail(id);
        recordProductViewBehavior(id, productVO);
        return Result.success(productVO);
    }
    
    @Operation(summary = "添加商品")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> addProduct(@Valid @RequestBody ProductDTO productDTO) {
        productService.addProduct(productDTO);
        return Result.success("添加成功", null);
    }
    
    @Operation(summary = "更新商品")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> updateProduct(@Valid @RequestBody ProductDTO productDTO) {
        productService.updateProduct(productDTO);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "上架商品")
    @PutMapping("/{id}/on-shelf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> onShelf(@PathVariable Long id) {
        productService.onShelf(id);
        return Result.success("上架成功", null);
    }
    
    @Operation(summary = "下架商品")
    @PutMapping("/{id}/off-shelf")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.offShelf(id);
        return Result.success("下架成功", null);
    }
    
    @Operation(summary = "更新商品库存")
    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> updateStock(@PathVariable Long id, @RequestBody Map<String, Integer> stockData) {
        Integer stock = stockData.get("stock");
        if (stock == null || stock < 0) {
            return Result.error("库存数量无效");
        }
        Long skuId = parseLong(stockData.get("skuId"));
        productService.updateStock(id, skuId, stock, getCurrentOperatorId());
        return Result.success("库存更新成功", null);
    }
    
    @Operation(summary = "快速补货")
    @PostMapping("/{id}/restock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<Void> restock(@PathVariable Long id, @RequestBody Map<String, Object> restockData) {
        log.info("收到补货请求: productId={}, data={}", id, restockData);
        
        // 安全地获取quantity，处理Integer和Long类型
        Integer quantity = null;
        Object quantityObj = restockData.get("quantity");
        if (quantityObj instanceof Integer) {
            quantity = (Integer) quantityObj;
        } else if (quantityObj instanceof Long) {
            quantity = ((Long) quantityObj).intValue();
        } else if (quantityObj instanceof Number) {
            quantity = ((Number) quantityObj).intValue();
        }
        String remark = (String) restockData.get("remark");
        
        log.info("解析后参数: quantity={}, remark={}", quantity, remark);
        
        if (quantity == null || quantity <= 0) {
            return Result.error("补货数量无效");
        }
        
        // 获取当前用户ID
        Long operatorId = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof com.coffee.security.JwtUserDetails) {
                operatorId = ((com.coffee.security.JwtUserDetails) principal).getUserId();
            }
        } catch (Exception e) {
            // 忽略，使用null
        }
        
        Long skuId = parseLong(restockData.get("skuId"));
        productService.restock(id, skuId, quantity, remark, operatorId);
        return Result.success("补货成功", null);
    }
    
    @Operation(summary = "获取库存变更记录")
    @GetMapping("/stock-log")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'product:list')")
    public Result<IPage<StockLogVO>> getStockLog(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Integer changeType) {
        log.info("查询库存记录: productId={}, skuId={}, changeType={}, pageNum={}, pageSize={}", productId, skuId, changeType, pageNum, pageSize);
        Page<StockLogVO> page = new Page<>(pageNum, pageSize);
        IPage<StockLogVO> result = stockLogMapper.selectStockLogPage(page, productId, skuId, changeType);
        log.info("查询结果: total={}, records={}", result.getTotal(), result.getRecords().size());
        return Result.success(result);
    }

    private Long getCurrentOperatorId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            return currentUserId;
        }
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof com.coffee.security.JwtUserDetails) {
                return ((com.coffee.security.JwtUserDetails) principal).getUserId();
            }
        } catch (Exception e) {
            log.debug("获取当前操作人失败: {}", e.getMessage());
        }
        return null;
    }

    private void recordProductListBehavior(String keyword, Long categoryId, Long resultCount) {
        Long userId = getCurrentOperatorId();
        if (userId == null) {
            return;
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType(isNotBlank(keyword) ? "SEARCH" : "VIEW");
        behavior.setTargetType(categoryId != null ? "CATEGORY" : "PRODUCT_LIST");
        behavior.setTargetId(categoryId);
        behavior.setPageUrl("/shop");
        behavior.setActionData(JSONUtil.createObj()
                .set("keyword", keyword)
                .set("categoryId", categoryId)
                .set("resultCount", resultCount == null ? 0 : resultCount)
                .toString());
        analyticsService.recordUserBehavior(behavior);
    }

    private void recordProductViewBehavior(Long productId, ProductVO productVO) {
        Long userId = getCurrentOperatorId();
        if (userId == null) {
            return;
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setActionType("VIEW");
        behavior.setTargetType("PRODUCT");
        behavior.setTargetId(productId);
        behavior.setPageUrl("/shop/product/" + productId);
        behavior.setActionData(JSONUtil.createObj()
                .set("productName", productVO == null ? null : productVO.getProductName())
                .set("categoryId", productVO == null ? null : productVO.getCategoryId())
                .toString());
        analyticsService.recordUserBehavior(behavior);
    }

    private boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}
