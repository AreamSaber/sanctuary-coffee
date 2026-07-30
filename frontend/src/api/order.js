import request from '@/utils/request'

/**
 * 创建订单
 */
export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

/**
 * 分页查询我的订单
 */
export function getOrderPage(params) {
  return request({
    url: '/order/page',
    method: 'get',
    params
  })
}

/**
 * 查询我的订单详情
 */
export function getOrderDetail(orderId) {
  return request({
    url: `/order/${orderId}`,
    method: 'get'
  })
}

/**
 * 取消订单
 */
export function cancelOrder(orderId, reason) {
  return request({
    url: `/order/cancel/${orderId}`,
    method: 'post',
    params: { reason }
  })
}

/**
 * 兼容旧支付接口
 */
export function payOrder(orderId, paymentMethod = 'ALIPAY') {
  return request({
    url: `/order/pay/${orderId}`,
    method: 'post',
    params: { paymentMethod }
  })
}

/**
 * 确认收货
 */
export function confirmReceipt(orderId) {
  return request({
    url: `/order/confirm/${orderId}`,
    method: 'post'
  })
}

/**
 * 删除订单
 */
export function deleteOrder(orderId) {
  return request({
    url: `/order/${orderId}`,
    method: 'delete'
  })
}

/**
 * 管理员分页查询订单
 */
export function getAdminOrderPage(params) {
  return request({
    url: '/order/admin/page',
    method: 'get',
    params
  })
}

/**
 * 管理员查询订单详情
 */
export function getAdminOrderDetail(orderId) {
  return request({
    url: `/order/admin/${orderId}`,
    method: 'get'
  })
}

/**
 * 管理员取消待付款订单
 */
export function adminCancelOrder(orderId, reason) {
  return request({
    url: `/order/admin/${orderId}/cancel`,
    method: 'post',
    params: { reason }
  })
}
