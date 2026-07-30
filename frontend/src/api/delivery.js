import request from '@/utils/request'

/**
 * 获取可用配送方式
 */
export function getDeliveryMethods() {
  return request({
    url: '/delivery/methods',
    method: 'get'
  })
}

// ========== 配送方式管理 ==========

/**
 * 分页查询配送方式
 */
export function getMethodPage(params) {
  return request({
    url: '/delivery/method/page',
    method: 'get',
    params
  })
}

/**
 * 获取全部配送方式
 */
export function getMethodList() {
  return request({
    url: '/delivery/method/list',
    method: 'get'
  })
}

/**
 * 创建配送方式
 */
export function createMethod(data) {
  return request({
    url: '/delivery/method',
    method: 'post',
    data
  })
}

/**
 * 更新配送方式
 */
export function updateMethod(data) {
  return request({
    url: '/delivery/method',
    method: 'put',
    data
  })
}

/**
 * 删除配送方式
 */
export function deleteMethod(id) {
  return request({
    url: `/delivery/method/${id}`,
    method: 'delete'
  })
}

/**
 * 获取配送详情
 */
export function getDeliveryDetail(orderId) {
  return request({
    url: `/delivery/detail/${orderId}`,
    method: 'get'
  })
}

/**
 * 获取配送轨迹
 */
export function getDeliveryTracking(orderId) {
  return request({
    url: `/delivery/tracking/${orderId}`,
    method: 'get'
  })
}

/**
 * 获取当前账号配送任务
 */
export function getDeliveryTasks(params) {
  return request({
    url: '/delivery/tasks',
    method: 'get',
    params
  })
}

/**
 * 分配配送员；不传 staffId 时由后端自动选择可用配送员
 */
export function assignDelivery(orderId, staffId) {
  return request({
    url: `/delivery/${orderId}/assign`,
    method: 'post',
    params: staffId ? { staffId } : undefined
  })
}

/**
 * 配送员接单
 */
export function acceptDelivery(orderId) {
  return request({
    url: `/delivery/${orderId}/accept`,
    method: 'post'
  })
}

/**
 * 开始配送
 */
export function startDelivery(orderId) {
  return request({
    url: `/delivery/${orderId}/start`,
    method: 'post'
  })
}

/**
 * 确认送达
 */
export function completeDelivery(orderId) {
  return request({
    url: `/delivery/${orderId}/complete`,
    method: 'post'
  })
}

// ========== 配送区域管理 ==========

/**
 * 分页查询配送区域
 */
export function getRegionPage(params) {
  return request({
    url: '/delivery/region/page',
    method: 'get',
    params
  })
}

/**
 * 获取配送区域树形结构
 */
export function getRegionTree() {
  return request({
    url: '/delivery/region/tree',
    method: 'get'
  })
}

/**
 * 创建配送区域
 */
export function createRegion(data) {
  return request({
    url: '/delivery/region',
    method: 'post',
    data
  })
}

/**
 * 更新配送区域
 */
export function updateRegion(data) {
  return request({
    url: '/delivery/region',
    method: 'put',
    data
  })
}

/**
 * 删除配送区域
 */
export function deleteRegion(id) {
  return request({
    url: `/delivery/region/${id}`,
    method: 'delete'
  })
}

// ========== 配送员管理 ==========

/**
 * 分页查询配送员
 */
export function getStaffPage(params) {
  return request({
    url: '/delivery/staff/page',
    method: 'get',
    params
  })
}

/**
 * 获取配送员详情
 */
export function getStaffDetail(id) {
  return request({
    url: `/delivery/staff/${id}`,
    method: 'get'
  })
}

/**
 * 创建配送员
 */
export function createStaff(data) {
  return request({
    url: '/delivery/staff',
    method: 'post',
    data
  })
}

/**
 * 更新配送员
 */
export function updateStaff(data) {
  return request({
    url: '/delivery/staff',
    method: 'put',
    data
  })
}

/**
 * 删除配送员
 */
export function deleteStaff(id) {
  return request({
    url: `/delivery/staff/${id}`,
    method: 'delete'
  })
}

/**
 * 更新配送员状态
 */
export function updateStaffStatus(id, status) {
  return request({
    url: `/delivery/staff/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 分配配送员到区域
 */
export function assignStaffToRegion(staffId, regionId) {
  return request({
    url: `/delivery/staff/${staffId}/region/${regionId}`,
    method: 'put'
  })
}

/**
 * 获取区域下的配送员
 */
export function getStaffByRegion(regionId) {
  return request({
    url: `/delivery/staff/region/${regionId}`,
    method: 'get'
  })
}

/**
 * 获取空闲的配送员
 */
export function getAvailableStaff(regionId) {
  return request({
    url: `/delivery/staff/available/${regionId}`,
    method: 'get'
  })
}

/**
 * 自动分配配送员
 */
export function autoAssignStaff(orderId, regionId) {
  return request({
    url: '/delivery/staff/auto-assign',
    method: 'post',
    params: { orderId, regionId }
  })
}

// ========== 配送异常处理 ==========

/**
 * 上报配送异常
 */
export function reportDeliveryException(deliveryId, exceptionType, exceptionDesc) {
  return request({
    url: '/delivery/exception/report',
    method: 'post',
    params: { deliveryId, exceptionType, exceptionDesc }
  })
}

/**
 * 处理配送异常
 */
export function handleDeliveryException(exceptionId, handleResult) {
  return request({
    url: `/delivery/exception/${exceptionId}/handle`,
    method: 'put',
    params: { handleResult }
  })
}

/**
 * 分页查询配送异常记录
 */
export function getDeliveryExceptionPage(params) {
  return request({
    url: '/delivery/exceptions',
    method: 'get',
    params
  })
}
