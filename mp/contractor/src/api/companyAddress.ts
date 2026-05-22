import { http } from '@/utils/http'
import type { SavedAddress } from '@/utils/addressStorage'

// --- CompanyAddress ---

/**
 * 公司地址簿新增参数（POST `/system/company-address`）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CompanyAddressCreateDTO {
  /**
 * 详细地址（整行）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  address: string
  contactName: string
  contactPhone: string
  /**
 * 是否默认地址（1=是，0=否）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  isDefault?: number
}

/**
 * 新增公司地址
 * 真源：[jasic-ui/src/api/companyAddress.js](../../../../jasic-ui/src/api/companyAddress.js) `createCompanyAddress`
 * @returns 成功时 data 为新地址 ID（long）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const addCompanyAddress = (data: CompanyAddressCreateDTO) => {
  return http<number>({
    url: '/system/company-address',
    method: 'POST',
    data,
  })
}

/**
 * 公司地址簿修改参数（PUT `/system/company-address`）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CompanyAddressUpdateDTO {
  /**
 * 详细地址（与表单一致：省市区 + 街道门牌等合并为一条）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  address: string
  contactName: string
  contactPhone: string
  id: number
  /**
 * 是否默认地址（1=是，0=否）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  isDefault?: number
}

/**
 * 修改公司地址
 * 真源：[jasic-ui/src/api/companyAddress.js](../../../../jasic-ui/src/api/companyAddress.js) `updateCompanyAddress`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const updateCompanyAddress = (data: CompanyAddressUpdateDTO) => {
  return http<null>({
    url: '/system/company-address',
    method: 'PUT',
    data,
  })
}

/**
 * 公司地址列表项（与 `GET /system/company-address/list` 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CompanyAddressVO {
  id: number
  companyId: number
  contactName: string
  contactPhone: string
  /**
 * 详细地址（整段文案）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  address: string
  /**
 * 是否默认地址（1=是，0=否）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  isDefault: number
}

/**
 * 查询当前公司地址列表
 * 真源：[jasic-ui/src/api/companyAddress.js](../../../../jasic-ui/src/api/companyAddress.js) `listCompanyAddress`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const listCompanyAddress = () => {
  return http<CompanyAddressVO[]>({
    url: '/system/company-address/list',
    method: 'GET',
  })
}

/**
 * 删除公司地址（DELETE `/system/company-address/{addressId}`）
 * 真源：[jasic-ui/src/api/companyAddress.js](../../../../jasic-ui/src/api/companyAddress.js) `deleteCompanyAddress`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const deleteCompanyAddress = (addressId: number) => {
  return http<null>({
    url: `/system/company-address/${addressId}`,
    method: 'DELETE',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
}

/**
 * 设为默认公司地址
 * `PUT /system/company-address/{addressId}/default`（path：`addressId` int64）
 * 响应：`Result<Void>`（code/msg）
 * 真源：[jasic-ui/src/api/companyAddress.js](../../../../jasic-ui/src/api/companyAddress.js) `setDefaultCompanyAddress`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const setDefaultCompanyAddress = (addressId: number) => {
  return http<null>({
    url: `/system/company-address/${addressId}/default`,
    method: 'PUT',
  })
}

/**
 * 将公司地址列表 VO 转为本地缓存结构（无省市区拆分时，整段放入 detail 供展示与编辑）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
