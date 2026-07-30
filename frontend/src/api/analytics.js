import request from '@/utils/request'

/**
 * 记录用户行为
 */
export function recordBehavior(data) {
  return request({
    url: '/analytics/behavior',
    method: 'post',
    data
  })
}

/**
 * 批量记录用户行为
 */
export function recordBehaviorBatch(data) {
  return request({
    url: '/analytics/behavior/batch',
    method: 'post',
    data
  })
}

/**
 * 获取用户行为分析报告
 */
export function getAnalyticsReport(params) {
  return request({
    url: '/analytics/report',
    method: 'get',
    params
  })
}

/**
 * 获取实时统计数据
 */
export function getRealTimeStats() {
  return request({
    url: '/analytics/realtime',
    method: 'get'
  })
}

/**
 * 获取用户画像
 */
export function getUserProfile(userId) {
  return request({
    url: `/analytics/user/${userId}`,
    method: 'get'
  })
}

/**
 * 分页查询用户行为记录
 */
export function getUserBehaviors(userId, params) {
  return request({
    url: `/analytics/behavior/user/${userId}`,
    method: 'get',
    params
  })
}

/**
 * 获取热门商品
 */
export function getHotProducts(params) {
  return request({
    url: '/analytics/hot-products',
    method: 'get',
    params
  })
}

/**
 * 获取用户活跃度趋势
 */
export function getActivityTrend(params) {
  return request({
    url: '/analytics/activity-trend',
    method: 'get',
    params
  })
}

/**
 * 获取复购率统计
 */
export function getRepurchaseRate(params) {
  return request({
    url: '/analytics/repurchase-rate',
    method: 'get',
    params
  })
}
