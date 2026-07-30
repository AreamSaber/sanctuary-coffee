import request from '@/utils/request'

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data) {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 */
export function changePassword(oldPassword, newPassword) {
  return request({
    url: '/user/password',
    method: 'put',
    params: { oldPassword, newPassword }
  })
}

/**
 * 获取用户地址列表
 */
export function getAddressList() {
  return request({
    url: '/user/address/list',
    method: 'get'
  })
}

/**
 * 添加地址
 */
export function addAddress(data) {
  return request({
    url: '/user/address',
    method: 'post',
    data
  })
}

/**
 * 更新地址
 */
export function updateAddress(data) {
  return request({
    url: '/user/address',
    method: 'put',
    data
  })
}

/**
 * 删除地址
 */
export function deleteAddress(id) {
  return request({
    url: `/user/address/${id}`,
    method: 'delete'
  })
}

/**
 * 设置默认地址
 */
export function setDefaultAddress(id) {
  return request({
    url: `/user/address/${id}/default`,
    method: 'put'
  })
}

/**
 * 获取默认地址
 */
export function getDefaultAddress() {
  return request({
    url: '/user/address/default',
    method: 'get'
  })
}
