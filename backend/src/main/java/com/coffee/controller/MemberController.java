package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.MemberUpdateDTO;
import com.coffee.entity.PointsRecord;
import com.coffee.service.MemberService;
import com.coffee.service.PointsService;
import com.coffee.vo.MemberInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员控制器
 */
@Tag(name = "会员管理")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PointsService pointsService;

    @Operation(summary = "获取会员信息")
    @GetMapping("/info")
    public Result<MemberInfoVO> getMemberInfo() {
        Long userId = getCurrentUserId();
        MemberInfoVO memberInfo = memberService.getMemberInfo(userId);
        return Result.success(memberInfo);
    }

    @Operation(summary = "获取积分余额")
    @GetMapping("/points/balance")
    public Result<Integer> getPointsBalance() {
        Long userId = getCurrentUserId();
        Integer balance = pointsService.getPointsBalance(userId);
        return Result.success(balance);
    }

    @Operation(summary = "分页查询积分记录")
    @GetMapping("/points/records")
    public Result<IPage<PointsRecord>> getPointsRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        IPage<PointsRecord> page = pointsService.getPointsRecordPage(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取会员列表（管理员）")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<IPage<MemberInfoVO>> getMemberList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) String username) {
        IPage<MemberInfoVO> page = memberService.getMemberList(pageNum, pageSize, levelId, username);
        return Result.success(page);
    }

    @Operation(summary = "编辑会员资料（管理员）")
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<Void> updateMember(
            @PathVariable Long userId,
            @Valid @RequestBody MemberUpdateDTO updateDTO) {
        memberService.updateMember(userId, updateDTO);
        return Result.success("会员资料更新成功", null);
    }

    @Operation(summary = "调整会员积分（管理员）")
    @PostMapping("/points/adjust")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<Void> adjustPoints(
            @RequestParam Long userId,
            @RequestParam Integer type,
            @RequestParam Integer points,
            @RequestParam(required = false) String remark) {
        memberService.adjustPoints(userId, type, points, remark);
        return Result.success("积分调整成功", null);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
