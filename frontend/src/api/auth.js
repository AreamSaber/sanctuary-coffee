import request from '@/utils/request'

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 查询是否需要初始化测试账号密码
 */
export function getPasswordInitStatus() {
  return request({
    url: '/auth/setup/status',
    method: 'get'
  })
}

/**
 * 批量初始化空密码测试账号
 */
export function initializeBlankPasswords(data) {
  return request({
    url: '/auth/setup/passwords',
    method: 'post',
    data
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
