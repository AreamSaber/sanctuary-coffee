import request from '@/utils/request'

/**
 * 获取订单结算信息
 */
export function getOrderSettlement(orderId) {
  return request({
    url: `/payment/settlement/${orderId}`,
    method: 'get'
  })
}

/**
 * 创建支付单
 */
export function createPayment(data) {
  return request({
    url: '/payment/create',
    method: 'post',
    data
  })
}

/**
 * 确认支付完成
 */
export function confirmPayment(paymentNo) {
  return request({
    url: `/payment/confirm/${paymentNo}`,
    method: 'post'
  })
}

/**
 * 申请退款
 */
export function applyRefund(orderId, reason) {
  return request({
    url: `/payment/refund/${orderId}`,
    method: 'post',
    params: { reason }
  })
}

/**
 * 管理员分页查询退款
 */
export function getAdminRefundPage(params) {
  return request({
    url: '/payment/refund/admin/page',
    method: 'get',
    params
  })
}

/**
 * 管理员查询退款详情
 */
export function getAdminRefundDetail(refundId) {
  return request({
    url: `/payment/refund/admin/${refundId}`,
    method: 'get'
  })
}

/**
 * 管理员审核通过退款
 */
export function approveAdminRefund(refundId, data = {}) {
  return request({
    url: `/payment/refund/admin/${refundId}/approve`,
    method: 'post',
    data
  })
}

/**
 * 管理员驳回退款
 */
export function rejectAdminRefund(refundId, data = {}) {
  return request({
    url: `/payment/refund/admin/${refundId}/reject`,
    method: 'post',
    data
  })
}
