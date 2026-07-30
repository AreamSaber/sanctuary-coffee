package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.dto.PermissionSaveDTO;
import com.coffee.dto.RolePermissionAssignDTO;
import com.coffee.dto.RoleSaveDTO;
import com.coffee.dto.UserRoleAssignDTO;
import com.coffee.service.RbacService;
import com.coffee.vo.PermissionVO;
import com.coffee.vo.RbacUserVO;
import com.coffee.vo.RoleVO;
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

import java.util.List;

/**
 * RBAC 权限管理控制器
 */
@Tag(name = "RBAC 权限管理")
@RestController
@RequestMapping("/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'rbac:view')")
public class RbacController {

    private final RbacService rbacService;

    @Operation(summary = "查询角色列表")
    @GetMapping("/roles")
    public Result<List<RoleVO>> listRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(rbacService.listRoles(keyword, status));
    }

    @Operation(summary = "新增角色")
    @PostMapping("/roles")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.success("角色创建成功", rbacService.createRole(dto));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/roles/{roleId}")
    public Result<RoleVO> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleSaveDTO dto) {
        return Result.success("角色更新成功", rbacService.updateRole(roleId, dto));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        rbacService.deleteRole(roleId);
        return Result.success("角色删除成功", null);
    }

    @Operation(summary = "分配角色权限")
    @PutMapping("/roles/{roleId}/permissions")
    public Result<Void> assignRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody RolePermissionAssignDTO dto) {
        rbacService.assignRolePermissions(roleId, dto);
        return Result.success("角色权限已更新", null);
    }

    @Operation(summary = "查询权限树")
    @GetMapping("/permissions/tree")
    public Result<List<PermissionVO>> listPermissionTree() {
        return Result.success(rbacService.listPermissionTree());
    }

    @Operation(summary = "新增权限")
    @PostMapping("/permissions")
    public Result<PermissionVO> createPermission(@Valid @RequestBody PermissionSaveDTO dto) {
        return Result.success("权限创建成功", rbacService.createPermission(dto));
    }

    @Operation(summary = "更新权限")
    @PutMapping("/permissions/{permissionId}")
    public Result<PermissionVO> updatePermission(
            @PathVariable Long permissionId,
            @Valid @RequestBody PermissionSaveDTO dto) {
        return Result.success("权限更新成功", rbacService.updatePermission(permissionId, dto));
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/permissions/{permissionId}")
    public Result<Void> deletePermission(@PathVariable Long permissionId) {
        rbacService.deletePermission(permissionId);
        return Result.success("权限删除成功", null);
    }

    @Operation(summary = "查询用户角色分页")
    @GetMapping("/users")
    public Result<IPage<RbacUserVO>> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(rbacService.listUsers(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/users/{userId}/roles")
    public Result<Void> assignUserRoles(@PathVariable Long userId, @Valid @RequestBody UserRoleAssignDTO dto) {
        rbacService.assignUserRoles(userId, dto);
        return Result.success("用户角色已更新", null);
    }
}
