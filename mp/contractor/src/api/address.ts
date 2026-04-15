import { http } from '@/utils/http'
import type { SavedAddress } from '@/utils/addressStorage'

/** 公司地址簿新增参数（POST `/api/system/company-address`） */
export interface CompanyAddressCreateDTO {
  /** 详细地址（整行） */
  address: string
  contactName: string
  contactPhone: string
  /** 是否默认地址（1=是，0=否） */
  isDefault?: number
}

/**
 * 新增公司/客户收货地址
 * @returns 成功时 data/result 为新地址 ID（long）
 */
export const createCustomerAddressAPI = (data: CompanyAddressCreateDTO) => {
  return http<number>({
    url: '/api/system/company-address',
    method: 'POST',
    data,
  })
}

/** 公司地址簿修改参数（PUT `/api/system/company-address`） */
export interface CompanyAddressUpdateDTO {
  /** 详细地址（与表单一致：省市区 + 街道门牌等合并为一条） */
  address: string
  contactName: string
  contactPhone: string
  id: number
  /** 是否默认地址（1=是，0=否） */
  isDefault?: number
}

/**
 * 修改公司收货地址
 */
export const updateCompanyAddressAPI = (data: CompanyAddressUpdateDTO) => {
  return http<null>({
    url: '/api/system/company-address',
    method: 'PUT',
    data,
  })
}

/** 公司地址列表项（与 `GET /api/system/company-address/list` 一致） */
export interface CompanyAddressVO {
  id: number
  companyId: number
  contactName: string
  contactPhone: string
  /** 详细地址（整段文案） */
  address: string
  /** 是否默认地址（1=是，0=否） */
  isDefault: number
}

/**
 * 查询当前公司地址列表
 */
export const getCompanyAddressListAPI = () => {
  return http<CompanyAddressVO[]>({
    url: '/api/system/company-address/list',
    method: 'GET',
  })
}

/**
 * 删除公司地址（DELETE `/api/system/company-address/{addressId}`）
 */
export const deleteCompanyAddressAPI = (addressId: number) => {
  return http<null>({
    url: `/api/system/company-address/${addressId}`,
    method: 'DELETE',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
}

/** 与 `deleteCompanyAddressAPI` 相同（旧页面/文档曾用此命名） */
export const deleteCustomerAddressAPI = deleteCompanyAddressAPI

/**
 * 设为默认公司地址
 * `PUT /api/system/company-address/{addressId}/default`（path：`addressId` int64）
 * 响应：`Result<Void>`（code/msg）
 */
export const setDefaultCustomerAddressAPI = (addressId: number) => {
  return http<null>({
    url: `/api/system/company-address/${addressId}/default`,
    method: 'PUT',
  })
}

/** 将公司地址列表 VO 转为本地缓存结构（无省市区拆分时，整段放入 detail 供展示与编辑） */
export function companyAddressVOToSavedAddress(vo: CompanyAddressVO): SavedAddress {
  const line = typeof vo.address === 'string' ? vo.address.trim() : ''
  return {
    id: String(vo.id),
    name: vo.contactName,
    phone: vo.contactPhone,
    province: '',
    city: '',
    county: '',
    detail: line,
    ...(line ? { fullAddress: line } : {}),
    isDefault: vo.isDefault
  }
}
