package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.PermissionSaveDTO;
import com.coffee.dto.RolePermissionAssignDTO;
import com.coffee.dto.RoleSaveDTO;
import com.coffee.dto.UserRoleAssignDTO;
import com.coffee.entity.Permission;
import com.coffee.entity.Role;
import com.coffee.entity.User;
import com.coffee.mapper.PermissionMapper;
import com.coffee.mapper.RoleMapper;
import com.coffee.mapper.RolePermissionMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.mapper.UserRoleMapper;
import com.coffee.service.RbacService;
import com.coffee.vo.PermissionVO;
import com.coffee.vo.RbacUserVO;
import com.coffee.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 权限管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private static final int ENABLED = 1;
    private static final Set<String> BUILTIN_ROLE_CODES = Set.of("ROLE_ADMIN", "ROLE_USER", "ROLE_DELIVERY");

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<RoleVO> listRoles(String keyword, Integer status) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), query -> query
                .like(Role::getRoleName, keyword)
                .or()
                .like(Role::getRoleCode, keyword))
            .eq(status != null, Role::getStatus, status)
            .orderByAsc(Role::getId);

        return roleMapper.selectList(wrapper).stream()
            .map(this::toRoleVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleSaveDTO dto) {
        validateRoleCodeUnique(null, dto.getRoleCode());
        Role role = new Role();
        applyRole(role, dto);
        roleMapper.insert(role);
        return toRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long roleId, RoleSaveDTO dto) {
        Role role = requireRole(roleId);
        validateRoleCodeUnique(roleId, dto.getRoleCode());
        applyRole(role, dto);
        roleMapper.updateById(role);
        return toRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Role role = requireRole(roleId);
        if (BUILTIN_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BusinessException("内置角色不允许删除");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        userRoleMapper.deleteByRoleId(roleId);
        roleMapper.deleteById(roleId);
    }

    @Override
    public List<PermissionVO> listPermissionTree() {
        List<Permission> permissions = permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId)
        );
        return buildPermissionTree(permissions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO createPermission(PermissionSaveDTO dto) {
        validatePermissionCodeUnique(null, dto.getPermissionCode());
        validateParentPermission(dto.getParentId(), null);
        Permission permission = new Permission();
        applyPermission(permission, dto);
        permissionMapper.insert(permission);
        return toPermissionVO(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionVO updatePermission(Long permissionId, PermissionSaveDTO dto) {
        Permission permission = requirePermission(permissionId);
        validatePermissionCodeUnique(permissionId, dto.getPermissionCode());
        validateParentPermission(dto.getParentId(), permissionId);
        applyPermission(permission, dto);
        permissionMapper.updateById(permission);
        return toPermissionVO(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long permissionId) {
        requirePermission(permissionId);
        deletePermissionCascade(permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolePermissions(Long roleId, RolePermissionAssignDTO dto) {
        requireRole(roleId);
        List<Long> permissionIds = normalizeIds(dto.getPermissionIds());
        validatePermissionIds(permissionIds);

        rolePermissionMapper.deleteByRoleId(roleId);
        permissionIds.forEach(permissionId -> rolePermissionMapper.insertRolePermission(roleId, permissionId));
    }

    @Override
    public IPage<RbacUserVO> listUsers(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<User> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), query -> query
                .like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
                .or()
                .like(User::getPhone, keyword)
                .or()
                .like(User::getEmail, keyword))
            .eq(status != null, User::getStatus, status)
            .orderByDesc(User::getCreateTime);

        IPage<User> userPage = userMapper.selectPage(page, wrapper);
        Page<RbacUserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream().map(this::toRbacUserVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, UserRoleAssignDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        List<Long> roleIds = normalizeIds(dto.getRoleIds());
        validateRoleIds(roleIds);

        userRoleMapper.deleteByUserId(userId);
        roleIds.forEach(roleId -> userRoleMapper.insertUserRole(userId, roleId));
    }

    private void applyRole(Role role, RoleSaveDTO dto) {
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleCode(dto.getRoleCode().trim());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() == null ? ENABLED : dto.getStatus());
    }

    private void applyPermission(Permission permission, PermissionSaveDTO dto) {
        permission.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        permission.setPermissionName(dto.getPermissionName().trim());
        permission.setPermissionCode(dto.getPermissionCode().trim());
        permission.setPermissionType(dto.getPermissionType());
        permission.setPath(dto.getPath());
        permission.setIcon(dto.getIcon());
        permission.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        permission.setStatus(dto.getStatus() == null ? ENABLED : dto.getStatus());
    }

    private Role requireRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private Permission requirePermission(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }
        return permission;
    }

    private void validateRoleCodeUnique(Long currentRoleId, String roleCode) {
        Long count = roleMapper.selectCount(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)
                .ne(currentRoleId != null, Role::getId, currentRoleId)
        );
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
    }

    private void validatePermissionCodeUnique(Long currentPermissionId, String permissionCode) {
        Long count = permissionMapper.selectCount(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermissionCode, permissionCode)
                .ne(currentPermissionId != null, Permission::getId, currentPermissionId)
        );
        if (count > 0) {
            throw new BusinessException("权限码已存在");
        }
    }

    private void validateParentPermission(Long parentId, Long currentPermissionId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (currentPermissionId != null && parentId.equals(currentPermissionId)) {
            throw new BusinessException("父级权限不能选择自身");
        }
        Permission parent = permissionMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("父级权限不存在");
        }
        if (currentPermissionId != null && isDescendant(parentId, currentPermissionId)) {
            throw new BusinessException("父级权限不能选择当前权限的子级");
        }
    }

    private boolean isDescendant(Long candidateId, Long ancestorId) {
        Permission cursor = permissionMapper.selectById(candidateId);
        while (cursor != null && cursor.getParentId() != null && cursor.getParentId() != 0) {
            if (ancestorId.equals(cursor.getParentId())) {
                return true;
            }
            cursor = permissionMapper.selectById(cursor.getParentId());
        }
        return false;
    }

    private void validatePermissionIds(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return;
        }
        Long count = permissionMapper.selectCount(new LambdaQueryWrapper<Permission>().in(Permission::getId, permissionIds));
        if (count != permissionIds.size()) {
            throw new BusinessException("存在无效权限ID");
        }
    }

    private void validateRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            throw new BusinessException("至少选择一个角色");
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds));
        if (count != roleIds.size()) {
            throw new BusinessException("存在无效角色ID");
        }
    }

    private void deletePermissionCascade(Long permissionId) {
        List<Permission> children = permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, permissionId)
        );
        children.forEach(child -> deletePermissionCascade(child.getId()));
        rolePermissionMapper.deleteByPermissionId(permissionId);
        permissionMapper.deleteById(permissionId);
    }

    private RoleVO toRoleVO(Role role) {
        RoleVO vo = BeanUtil.copyProperties(role, RoleVO.class);
        vo.setPermissionIds(rolePermissionMapper.selectPermissionIdsByRoleId(role.getId()));
        vo.setPermissionCodes(permissionMapper.selectPermissionCodesByRoleId(role.getId()));
        return vo;
    }

    private RbacUserVO toRbacUserVO(User user) {
        RbacUserVO vo = BeanUtil.copyProperties(user, RbacUserVO.class);
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        vo.setRoleIds(roleIds);
        if (roleIds.isEmpty()) {
            vo.setRoleCodes(List.of());
            vo.setRoleNames(List.of());
            return vo;
        }

        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getId, item -> item));
        vo.setRoleCodes(roleIds.stream()
            .map(roleMap::get)
            .filter(item -> item != null)
            .map(Role::getRoleCode)
            .collect(Collectors.toList()));
        vo.setRoleNames(roleIds.stream()
            .map(roleMap::get)
            .filter(item -> item != null)
            .map(Role::getRoleName)
            .collect(Collectors.toList()));
        return vo;
    }

    private List<PermissionVO> buildPermissionTree(List<Permission> permissions) {
        Map<Long, PermissionVO> nodeMap = new LinkedHashMap<>();
        permissions.forEach(permission -> nodeMap.put(permission.getId(), toPermissionVO(permission)));

        List<PermissionVO> roots = new ArrayList<>();
        nodeMap.values().forEach(node -> {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                return;
            }
            nodeMap.get(parentId).getChildren().add(node);
        });
        sortPermissionNodes(roots);
        return roots;
    }

    private void sortPermissionNodes(List<PermissionVO> nodes) {
        nodes.sort(Comparator.comparing((PermissionVO item) -> item.getSortOrder() == null ? 0 : item.getSortOrder())
            .thenComparing(item -> item.getId() == null ? 0L : item.getId()));
        nodes.forEach(item -> sortPermissionNodes(item.getChildren()));
    }

    private PermissionVO toPermissionVO(Permission permission) {
        PermissionVO vo = BeanUtil.copyProperties(permission, PermissionVO.class);
        if (vo.getChildren() == null) {
            vo.setChildren(new ArrayList<>());
        }
        return vo;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> seen = new HashSet<>();
        return ids.stream()
            .filter(id -> id != null && id > 0)
            .filter(seen::add)
            .collect(Collectors.toList());
    }
}
