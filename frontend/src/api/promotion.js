import request from '@/utils/request'

/**
 * 分页查询促销活动
 */
export function getPromotionPage(params) {
  return request({
    url: '/promotion/page',
    method: 'get',
    params
  })
}

/**
 * 创建促销活动
 */
export function createPromotion(data) {
  return request({
    url: '/promotion',
    method: 'post',
    data
  })
}

/**
 * 更新促销活动
 */
export function updatePromotion(data) {
  return request({
    url: '/promotion',
    method: 'put',
    data
  })
}

/**
 * 删除促销活动
 */
export function deletePromotion(id) {
  return request({
    url: `/promotion/${id}`,
    method: 'delete'
  })
}

/**
 * 更新促销活动状态
 */
export function updatePromotionStatus(id, status) {
  return request({
    url: `/promotion/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 获取当前有效的促销活动
 */
export function getActivePromotions() {
  return request({
    url: '/promotion/active',
    method: 'get'
  })
}

/**
 * 获取商品的促销活动
 */
export function getProductPromotion(productId) {
  return request({
    url: `/promotion/product/${productId}`,
    method: 'get'
  })
}
