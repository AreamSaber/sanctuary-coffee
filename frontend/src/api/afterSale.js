import request from '@/utils/request'

/**
 * 分页查询我的售后
 */
export function getMyAfterSalePage(params) {
  return request({
    url: '/after-sale/page',
    method: 'get',
    params
  })
}

/**
 * 查询我的售后详情
 */
export function getMyAfterSaleDetail(afterSaleId) {
  return request({
    url: `/after-sale/${afterSaleId}`,
    method: 'get'
  })
}

/**
 * 提交售后申请
 */
export function applyAfterSale(data) {
  return request({
    url: '/after-sale/apply',
    method: 'post',
    data
  })
}

/**
 * 管理员分页查询售后
 */
export function getAdminAfterSalePage(params) {
  return request({
    url: '/after-sale/admin/page',
    method: 'get',
    params
  })
}

/**
 * 管理员查询售后详情
 */
export function getAdminAfterSaleDetail(afterSaleId) {
  return request({
    url: `/after-sale/admin/${afterSaleId}`,
    method: 'get'
  })
}

/**
 * 管理员审核通过退款售后
 */
export function approveAdminAfterSale(afterSaleId, data = {}) {
  return request({
    url: `/after-sale/admin/${afterSaleId}/approve`,
    method: 'post',
    data
  })
}

/**
 * 管理员驳回退款售后
 */
export function rejectAdminAfterSale(afterSaleId, data = {}) {
  return request({
    url: `/after-sale/admin/${afterSaleId}/reject`,
    method: 'post',
    data
  })
}
