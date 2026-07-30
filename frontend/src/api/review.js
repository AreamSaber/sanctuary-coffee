import request from '@/utils/request'

/**
 * 获取商品评价列表
 */
export function getProductReviews(productId, params) {
  return request({
    url: `/review/product/${productId}`,
    method: 'get',
    params
  })
}

/**
 * 获取我的评价列表
 */
export function getMyReviews(params) {
  return request({
    url: '/review/my',
    method: 'get',
    params
  })
}

/**
 * 获取所有评价列表（管理员）
 */
export function getAllReviews(params) {
  return request({
    url: '/review/all',
    method: 'get',
    params
  })
}

/**
 * 添加评价
 */
export function addReview(data) {
  return request({
    url: '/review',
    method: 'post',
    data
  })
}

/**
 * 删除评价
 */
export function deleteReview(id) {
  return request({
    url: `/review/${id}`,
    method: 'delete'
  })
}

/**
 * 隐藏评价（管理员）
 */
export function hideReview(id) {
  return request({
    url: `/review/admin/${id}/hide`,
    method: 'put'
  })
}

/**
 * 恢复评价展示（管理员）
 */
export function restoreReview(id) {
  return request({
    url: `/review/admin/${id}/restore`,
    method: 'put'
  })
}

/**
 * 回复评价（管理员）
 */
export function replyReview(id, data) {
  return request({
    url: `/review/admin/${id}/reply`,
    method: 'post',
    data
  })
}

/**
 * 获取商品评价统计
 */
export function getReviewStats(productId) {
  return request({
    url: `/review/stats/${productId}`,
    method: 'get'
  })
}

/**
 * 检查是否可以评价
 */
export function checkCanReview(params) {
  return request({
    url: '/review/check',
    method: 'get',
    params
  })
}
