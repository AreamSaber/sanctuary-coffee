package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.dto.PermissionSaveDTO;
import com.coffee.dto.RolePermissionAssignDTO;
import com.coffee.dto.RoleSaveDTO;
import com.coffee.dto.UserRoleAssignDTO;
import com.coffee.vo.PermissionVO;
import com.coffee.vo.RbacUserVO;
import com.coffee.vo.RoleVO;

import java.util.List;

/**
 * RBAC 权限管理服务
 */
public interface RbacService {

    List<RoleVO> listRoles(String keyword, Integer status);

    RoleVO createRole(RoleSaveDTO dto);

    RoleVO updateRole(Long roleId, RoleSaveDTO dto);

    void deleteRole(Long roleId);

    List<PermissionVO> listPermissionTree();

    PermissionVO createPermission(PermissionSaveDTO dto);

    PermissionVO updatePermission(Long permissionId, PermissionSaveDTO dto);

    void deletePermission(Long permissionId);

    void assignRolePermissions(Long roleId, RolePermissionAssignDTO dto);

    IPage<RbacUserVO> listUsers(Integer pageNum, Integer pageSize, String keyword, Integer status);

    void assignUserRoles(Long userId, UserRoleAssignDTO dto);
}
