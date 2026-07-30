package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.entity.MemberLevel;
import com.coffee.service.MemberLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员等级管理控制器
 * 
 * @author Coffee Shop Team
 */
@Tag(name = "会员等级管理", description = "会员等级权益管理接口")
@RestController
@RequestMapping("/member-level")
@RequiredArgsConstructor
public class MemberLevelController {
    
    private final MemberLevelService memberLevelService;
    
    @Operation(summary = "分页查询会员等级")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<IPage<MemberLevel>> getMemberLevelPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<MemberLevel> page = memberLevelService.getMemberLevelPage(pageNum, pageSize);
        return Result.success(page);
    }
    
    @Operation(summary = "获取所有启用的会员等级")
    @GetMapping("/list")
    public Result<List<MemberLevel>> getActiveLevels() {
        List<MemberLevel> levels = memberLevelService.getActiveLevels();
        return Result.success(levels);
    }
    
    @Operation(summary = "创建会员等级")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<Void> createLevel(@Valid @RequestBody MemberLevel memberLevel) {
        memberLevelService.createLevel(memberLevel);
        return Result.success("创建成功", null);
    }
    
    @Operation(summary = "更新会员等级")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<Void> updateLevel(@Valid @RequestBody MemberLevel memberLevel) {
        memberLevelService.updateLevel(memberLevel);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除会员等级")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<Void> deleteLevel(@PathVariable Long id) {
        memberLevelService.deleteLevel(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "更新会员等级状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'member:manage')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberLevelService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }
}
