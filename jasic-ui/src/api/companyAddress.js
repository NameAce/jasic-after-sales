import request from '@/utils/request'

export function listCompanyAddress() {
  return request({ url: '/system/company-address/list', method: 'get' })
}

export function createCompanyAddress(data) {
  return request({ url: '/system/company-address', method: 'post', data })
}

export function updateCompanyAddress(data) {
  return request({ url: '/system/company-address', method: 'put', data })
}

export function deleteCompanyAddress(addressId) {
  return request({ url: `/system/company-address/${addressId}`, method: 'delete' })
}

export function setDefaultCompanyAddress(addressId) {
  return request({ url: `/system/company-address/${addressId}/default`, method: 'put' })
}
