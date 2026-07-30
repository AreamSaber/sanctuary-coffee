import request from '@/utils/request'

/**
 * 获取销售统计
 */
export function getSalesStatistics() {
  return request({
    url: '/statistics/sales',
    method: 'get'
  })
}

/**
 * 获取商品销售排行
 */
export function getTopSellingProducts(limit = 10) {
  return request({
    url: '/statistics/products/top',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取低库存商品
 */
export function getLowStockProducts(threshold = 10) {
  return request({
    url: '/statistics/products/low-stock',
    method: 'get',
    params: { threshold }
  })
}

/**
 * 获取用户统计
 */
export function getUserStatistics() {
  return request({
    url: '/statistics/users',
    method: 'get'
  })
}

/**
 * 获取每日销售趋势
 */
export function getDailySalesTrend(days = 7) {
  return request({
    url: '/statistics/sales/trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取分类销售占比
 */
export function getCategorySalesDistribution() {
  return request({
    url: '/statistics/categories/distribution',
    method: 'get'
  })
}
