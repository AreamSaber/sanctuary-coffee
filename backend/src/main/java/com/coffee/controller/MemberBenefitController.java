package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.MemberBenefitDTO;
import com.coffee.dto.MemberLevelBenefitBindDTO;
import com.coffee.entity.BenefitGrantLog;
import com.coffee.service.MemberBenefitService;
import com.coffee.vo.MemberBenefitUsageVO;
import com.coffee.vo.MemberBenefitVO;
import com.coffee.vo.MemberLevelBenefitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员权益控制器
 */
@Tag(name = "会员权益管理")
@RestController
@RequestMapping("/member-benefit")
@RequiredArgsConstructor
public class MemberBenefitController {

    private final MemberBenefitService memberBenefitService;

    @Operation(summary = "分页查询会员权益")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<IPage<MemberBenefitVO>> getBenefitPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer benefitType,
            @RequestParam(required = false) Integer status) {
        return Result.success(memberBenefitService.getBenefitPage(pageNum, pageSize, keyword, benefitType, status));
    }

    @Operation(summary = "查询启用会员权益")
    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<List<MemberBenefitVO>> getActiveBenefits() {
        return Result.success(memberBenefitService.getActiveBenefits());
    }

    @Operation(summary = "创建会员权益")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<Void> createBenefit(@Valid @RequestBody MemberBenefitDTO dto) {
        memberBenefitService.createBenefit(dto);
        return Result.success("创建成功", null);
    }

    @Operation(summary = "更新会员权益")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<Void> updateBenefit(@PathVariable Long id, @Valid @RequestBody MemberBenefitDTO dto) {
        memberBenefitService.updateBenefit(id, dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除会员权益")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<Void> deleteBenefit(@PathVariable Long id) {
        memberBenefitService.deleteBenefit(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "更新会员权益状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberBenefitService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    @Operation(summary = "查询会员等级权益矩阵")
    @GetMapping("/level-matrix")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<List<MemberLevelBenefitVO>> getLevelBenefitMatrix() {
        return Result.success(memberBenefitService.getLevelBenefitMatrix());
    }

    @Operation(summary = "保存会员等级权益绑定")
    @PutMapping("/level/{levelId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<Void> saveLevelBenefits(
            @PathVariable Long levelId,
            @RequestBody MemberLevelBenefitBindDTO dto) {
        memberBenefitService.saveLevelBenefits(levelId, dto == null ? null : dto.getBenefitIds());
        return Result.success("保存成功", null);
    }

    @Operation(summary = "查询当前用户会员权益")
    @GetMapping("/my")
    public Result<List<MemberBenefitVO>> getMyBenefits() {
        Long userId = getCurrentUserId();
        return Result.success(memberBenefitService.getUserBenefits(userId));
    }

    @Operation(summary = "分页查询当前用户权益使用记录")
    @GetMapping("/usage/my")
    public Result<IPage<MemberBenefitUsageVO>> getMyUsagePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        return Result.success(memberBenefitService.getMyUsagePage(userId, pageNum, pageSize));
    }

    @Operation(summary = "分页查询会员权益使用记录")
    @GetMapping("/usage/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<IPage<MemberBenefitUsageVO>> getUsagePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer benefitType,
            @RequestParam(required = false) String businessType) {
        return Result.success(memberBenefitService.getAdminUsagePage(pageNum, pageSize, userId, benefitType, businessType));
    }

    @Operation(summary = "发放权益给用户")
    @PostMapping("/grant")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:benefit')")
    public Result<Void> grantBenefit(@RequestParam Long userId,
                                     @RequestParam Long benefitId,
                                     @RequestParam(required = false) BigDecimal value,
                                     @RequestParam(required = false) String reason) {
        Long operatorId = getCurrentUserId();
        memberBenefitService.grantBenefit(userId, benefitId, value, reason, operatorId);
        return Result.success("权益已发放", null);
    }

    @Operation(summary = "用户权益发放记录")
    @GetMapping("/grant-logs")
    public Result<IPage<BenefitGrantLog>> getGrantLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer benefitType) {
        Long userId = getCurrentUserId();
        return Result.success(memberBenefitService.getUserGrantLogs(userId, benefitType, pageNum, pageSize));
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
