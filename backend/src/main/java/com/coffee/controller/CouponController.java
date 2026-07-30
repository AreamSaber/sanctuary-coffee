package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.CouponDTO;
import com.coffee.service.CouponService;
import com.coffee.vo.CouponStatsVO;
import com.coffee.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "优惠券管理")
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;
    
    @Operation(summary = "分页查询所有优惠券（管理员）")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'coupon:manage')")
    public Result<IPage<CouponVO>> getCouponPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        IPage<CouponVO> page = couponService.getAllCoupons(pageNum, pageSize, name, type, status);
        return Result.success(page);
    }
    
    @Operation(summary = "分页查询可领取优惠券")
    @GetMapping("/available")
    public Result<IPage<CouponVO>> getAvailableCoupons(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        IPage<CouponVO> page = couponService.getCouponPage(userId, pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "领取优惠券")
    @PostMapping("/{id}/receive")
    public Result<Void> receiveCoupon(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        couponService.receiveCoupon(userId, id);
        return Result.success("领取成功", null);
    }
    
    @Operation(summary = "获取我的优惠券")
    @GetMapping("/my")
    public Result<List<CouponVO>> getMyCoupons(@RequestParam(required = false) Integer status) {
        Long userId = getCurrentUserId();
        List<CouponVO> list = couponService.getUserCoupons(userId, status);
        return Result.success(list);
    }
    
    @Operation(summary = "创建优惠券")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'coupon:manage')")
    public Result<Void> createCoupon(@Valid @RequestBody CouponDTO couponDTO) {
        couponService.createCoupon(couponDTO);
        return Result.success("创建成功", null);
    }
    
    @Operation(summary = "更新优惠券")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'coupon:manage')")
    public Result<Void> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponDTO couponDTO) {
        couponService.updateCoupon(id, couponDTO);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除优惠券")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'coupon:manage')")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "更新优惠券状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'coupon:manage')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "获取优惠券核销统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'coupon:manage')")
    public Result<List<CouponStatsVO>> getCouponStats(
            @RequestParam(required = false) Integer couponType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        List<CouponStatsVO> stats = couponService.getCouponStats(couponType, startTime, endTime);
        return Result.success(stats);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
