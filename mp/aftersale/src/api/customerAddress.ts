import { http } from '@/utils/http'
import type { SavedAddress } from '@/utils/addressStorage'

// --- CustomerAddress ---

/** C 端客户地址新增参数（与 `/customer/address` 一致） */
export interface CustomerAddressCreateDTO {
  city: string
  contactMobile: string
  contactName: string
  /** 区县，可选 */
  county?: string
  detailAddress: string
  /** 是否默认地址（1=是，0=否） */
  isDefault?: number
  province: string
}

/**
 * 新增客户收货地址
 * @returns 成功时 data 为新地址 ID（long）
 */
export const addCustomerAddress = (data: CustomerAddressCreateDTO) => {
  return http<number>({
    url: '/customer/address',
    method: 'POST',
    data,
  })
}

/** C 端客户地址修改参数（与 `PUT /customer/address` 一致） */
export interface CustomerAddressUpdateDTO {
  city: string
  contactMobile: string
  contactName: string
  county?: string
  detailAddress: string
  id: number
  province: string
}

/**
 * 修改客户收货地址
 */
export const updateCustomerAddress = (data: CustomerAddressUpdateDTO) => {
  return http<null>({
    url: '/customer/address',
    method: 'PUT',
    data,
  })
}

/** 客户地址列表项（与 `/customer/address/list` 一致） */
export interface CustomerAddressVO {
  id: number
  city: string
  contactMobile: string
  contactName: string
  county: string
  detailAddress: string
  fullAddress: string
  isDefault: number
  province: string
}

/**
 * 查询当前客户地址列表
 */
export const listCustomerAddress = () => {
  return http<CustomerAddressVO[]>({
    url: '/customer/address/list',
    method: 'GET',
  })
}

/**
 * 删除客户收货地址
 */
export const deleteCustomerAddress = (addressId: number) => {
  return http<null>({
    url: `/customer/address/${addressId}`,
    method: 'DELETE',
  })
}

/**
 * 设为默认收货地址
 */
export const setDefaultCustomerAddress = (addressId: number) => {
  return http<null>({
    url: `/customer/address/${addressId}/default`,
    method: 'PUT',
    data: {},
  })
}

/** 将接口 VO 转为本地缓存结构 */
export function customerAddressVOToSavedAddress(vo: CustomerAddressVO): SavedAddress {
  return {
    id: String(vo.id),
    name: vo.contactName,
    phone: vo.contactMobile,
    province: vo.province,
    city: vo.city,
    county: vo.county || '',
    detail: vo.detailAddress,
    isDefault: vo.isDefault
  }
}
