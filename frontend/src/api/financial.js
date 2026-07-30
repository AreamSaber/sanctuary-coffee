import request from '@/utils/request'

export function getFinancialReport(params) {
  if (params.type === 'daily') {
    return request({
      url: '/financial/report/daily',
      method: 'get',
      params: { date: params.date }
    })
  }

  if (params.type === 'monthly') {
    return request({
      url: '/financial/report/monthly',
      method: 'get',
      params: { year: params.year, month: params.month }
    })
  }

  if (params.type === 'yearly') {
    return request({
      url: '/financial/report/yearly',
      method: 'get',
      params: { year: params.year }
    })
  }

  return request({
    url: '/financial/report',
    method: 'get',
    params: {
      startDate: params.startDate,
      endDate: params.endDate
    }
  })
}

export function exportFinancialReport(params) {
  return request({
    url: '/financial/report/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function applyInvoice(orderId, data) {
  return request({
    url: '/financial/invoice/apply',
    method: 'post',
    params: { orderId },
    data
  })
}

export function getMyInvoices(params) {
  return request({
    url: '/financial/invoice/my',
    method: 'get',
    params
  })
}

export function getAllInvoices(params) {
  return request({
    url: '/financial/invoice/all',
    method: 'get',
    params
  })
}

export function getInvoiceDetail(id) {
  return request({
    url: `/financial/invoice/${id}`,
    method: 'get'
  })
}

export function issueInvoice(id) {
  return request({
    url: `/financial/invoice/${id}/issue`,
    method: 'post'
  })
}

export function resendInvoice(id) {
  return request({
    url: `/financial/invoice/${id}/resend`,
    method: 'post'
  })
}
