import request from '@/utils/request'

// --- 公司类型管理 ---
export function listCompanyType() {
  return request({ url: '/org/company-type/list', method: 'get' })
}
export function addCompanyType(data) {
  return request({ url: '/org/company-type', method: 'post', data })
}
export function updateCompanyType(data) {
  return request({ url: '/org/company-type', method: 'put', data })
}
export function deleteCompanyType(id) {
  return request({ url: `/org/company-type/${id}`, method: 'delete' })
}

// --- 公司管理 ---
export function listCompany(params) {
  return request({ url: '/org/company/list', method: 'get', params })
}
export function getCompany(id) {
  return request({ url: `/org/company/${id}`, method: 'get' })
}
export function addCompany(data) {
  return request({ url: '/org/company', method: 'post', data })
}
export function updateCompany(data) {
  return request({ url: '/org/company', method: 'put', data })
}
export function deleteCompany(id) {
  return request({ url: `/org/company/${id}`, method: 'delete' })
}
export function listExternalCompany(params) {
  return request({ url: '/org/company/external/list', method: 'get', params })
}
export function getExternalCompanyImportPreview(custId) {
  return request({ url: `/org/company/external/${custId}/import-preview`, method: 'get' })
}

export function listAreaOptions(parentCode) {
  return request({ url: '/org/area/options', method: 'get', params: { parentCode } })
}

export function getAreaDetail(areaCode) {
  return request({ url: `/org/area/${areaCode}`, method: 'get' })
}

// --- 签约关系管理 ---
export function listHqFirstContract(params) {
  return request({ url: '/org/contract/hq-first/list', method: 'get', params })
}
export function addHqFirstContract(data) {
  return request({ url: '/org/contract/hq-first', method: 'post', data })
}
export function updateHqFirstContract(data) {
  return request({ url: '/org/contract/hq-first', method: 'put', data })
}
export function deleteHqFirstContract(id) {
  return request({ url: `/org/contract/hq-first/${id}`, method: 'delete' })
}
export function listCrmHqFirstContractImport(params) {
  return request({ url: '/org/contract/hq-first/crm-import/list', method: 'get', params })
}
export function importCrmHqFirstContract(data) {
  return request({ url: '/org/contract/hq-first/crm-import', method: 'post', data })
}

export function listFirstSecondRelation(params) {
  return request({ url: '/org/contract/first-second/list', method: 'get', params })
}
export function addFirstSecondRelation(data) {
  return request({ url: '/org/contract/first-second', method: 'post', data })
}
export function deleteFirstSecondRelation(id) {
  return request({ url: `/org/contract/first-second/${id}`, method: 'delete' })
}
