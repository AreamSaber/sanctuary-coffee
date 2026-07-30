import request from '@/utils/request'

/**
 * 分页查询商品列表
 */
export function getProductPage(params) {
  return request({
    url: '/product/page',
    method: 'get',
    params
  })
}

/**
 * 获取商品详情
 */
export function getProductDetail(id) {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}

/**
 * 添加商品
 */
export function addProduct(data) {
  return request({
    url: '/product',
    method: 'post',
    data
  })
}

/**
 * 更新商品
 */
export function updateProduct(data) {
  return request({
    url: '/product',
    method: 'put',
    data
  })
}

/**
 * 删除商品
 */
export function deleteProduct(id) {
  return request({
    url: `/product/${id}`,
    method: 'delete'
  })
}

/**
 * 上架商品
 */
export function onShelf(id) {
  return request({
    url: `/product/${id}/on-shelf`,
    method: 'put'
  })
}

/**
 * 下架商品
 */
export function offShelf(id) {
  return request({
    url: `/product/${id}/off-shelf`,
    method: 'put'
  })
}

/**
 * 获取分类树
 */
export function getCategoryTree() {
  return request({
    url: '/product/category/tree',
    method: 'get'
  })
}

/**
 * 获取分类列表
 */
export function getCategoryList(parentId) {
  return request({
    url: '/product/category/list',
    method: 'get',
    params: { parentId }
  })
}

/**
 * 添加分类
 */
export function addCategory(data) {
  return request({
    url: '/product/category',
    method: 'post',
    data
  })
}

/**
 * 更新分类
 */
export function updateCategory(data) {
  return request({
    url: '/product/category',
    method: 'put',
    data
  })
}

/**
 * 删除分类
 */
export function deleteCategory(id) {
  return request({
    url: `/product/category/${id}`,
    method: 'delete'
  })
}

/**
 * 更新商品库存
 */
export function updateProductStock(id, stock, skuId) {
  return request({
    url: `/product/${id}/stock`,
    method: 'put',
    data: { stock, skuId }
  })
}

/**
 * 快速补货
 */
export function restockProduct(id, quantity, remark, skuId) {
  return request({
    url: `/product/${id}/restock`,
    method: 'post',
    data: { quantity, remark, skuId }
  })
}

/**
 * 获取库存变更记录
 */
export function getStockLog(params) {
  return request({
    url: '/product/stock-log',
    method: 'get',
    params
  })
}
