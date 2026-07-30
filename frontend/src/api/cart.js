import request from '@/utils/request'

export function getCartList() {
  return request({
    url: '/cart/list',
    method: 'get'
  })
}

export function addToCart(data) {
  return request({
    url: '/cart',
    method: 'post',
    data
  })
}

export function updateQuantity(id, quantity) {
  return request({
    url: `/cart/${id}/quantity`,
    method: 'put',
    params: { quantity }
  })
}

export function removeFromCart(id) {
  return request({
    url: `/cart/${id}`,
    method: 'delete'
  })
}

export function clearCart() {
  return request({
    url: '/cart/clear',
    method: 'delete'
  })
}
