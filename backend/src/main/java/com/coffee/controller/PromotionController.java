package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.dto.PromotionDTO;
import com.coffee.entity.Promotion;
import com.coffee.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 促销活动控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "促销活动", description = "促销活动管理接口")
@RestController
@RequestMapping("/promotion")
@RequiredArgsConstructor
public class PromotionController {
    
    private final PromotionService promotionService;
    
    @Operation(summary = "分页查询促销活动")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'promotion:manage')")
    public Result<IPage<Promotion>> getPromotionPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        IPage<Promotion> page = promotionService.getPromotionPage(pageNum, pageSize, keyword, type, status);
        return Result.success(page);
    }
    
    @Operation(summary = "创建促销活动")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'promotion:manage')")
    public Result<Void> createPromotion(@Valid @RequestBody PromotionDTO promotionDTO) {
        promotionService.createPromotion(promotionDTO);
        return Result.success("创建成功", null);
    }
    
    @Operation(summary = "更新促销活动")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'promotion:manage')")
    public Result<Void> updatePromotion(@Valid @RequestBody PromotionDTO promotionDTO) {
        promotionService.updatePromotion(promotionDTO);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除促销活动")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'promotion:manage')")
    public Result<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "启用/禁用促销活动")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'promotion:manage')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        promotionService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }
    
    @Operation(summary = "获取当前有效的促销活动")
    @GetMapping("/active")
    public Result<List<Promotion>> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();
        return Result.success(promotions);
    }
    
    @Operation(summary = "获取商品的促销活动")
    @GetMapping("/product/{productId}")
    public Result<Promotion> getProductPromotion(@PathVariable Long productId) {
        Promotion promotion = promotionService.getProductPromotion(productId);
        return Result.success(promotion);
    }
}
