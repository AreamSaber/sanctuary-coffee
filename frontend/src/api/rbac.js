import request from '@/utils/request'

export function getRoles(params) {
  return request({
    url: '/rbac/roles',
    method: 'get',
    params
  })
}

export function createRole(data) {
  return request({
    url: '/rbac/roles',
    method: 'post',
    data
  })
}

export function updateRole(id, data) {
  return request({
    url: `/rbac/roles/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/rbac/roles/${id}`,
    method: 'delete'
  })
}

export function assignRolePermissions(id, permissionIds) {
  return request({
    url: `/rbac/roles/${id}/permissions`,
    method: 'put',
    data: { permissionIds }
  })
}

export function getPermissionTree() {
  return request({
    url: '/rbac/permissions/tree',
    method: 'get'
  })
}

export function createPermission(data) {
  return request({
    url: '/rbac/permissions',
    method: 'post',
    data
  })
}

export function updatePermission(id, data) {
  return request({
    url: `/rbac/permissions/${id}`,
    method: 'put',
    data
  })
}

export function deletePermission(id) {
  return request({
    url: `/rbac/permissions/${id}`,
    method: 'delete'
  })
}

export function getRbacUsers(params) {
  return request({
    url: '/rbac/users',
    method: 'get',
    params
  })
}

export function assignUserRoles(userId, roleIds) {
  return request({
    url: `/rbac/users/${userId}/roles`,
    method: 'put',
    data: { roleIds }
  })
}
