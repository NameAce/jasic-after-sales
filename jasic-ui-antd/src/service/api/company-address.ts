/**
 * 公司地址簿：联系人、默认地址等维护接口。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { request } from '../request';

type IdLike = string | number;

export interface CompanyAddressVO {
  id: number;
  companyId: number;
  contactName: string;
  contactPhone: string;
  address: string;
  isDefault: 0 | 1;
}

export interface CompanyAddressCreateDTO {
  contactName: string;
  contactPhone: string;
  address: string;
  isDefault?: 0 | 1;
  targetCompanyId?: number;
}

export interface CompanyAddressUpdateDTO extends CompanyAddressCreateDTO {
  id: number;
}

/** 作用：查询当前或已指定目标公司的收货地址列表。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function listCompanyAddress(params?: { targetCompanyId?: number }) {
  return request<CompanyAddressVO[]>({ url: '/system/company-address/list', method: 'get', params });
}

/** 作用：新增收货地址。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function createCompanyAddress(data: CompanyAddressCreateDTO) {
  return request<number>({ url: '/system/company-address', method: 'post', data });
}

/** 作用：更新收货地址。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function updateCompanyAddress(data: CompanyAddressUpdateDTO) {
  return request({ url: '/system/company-address', method: 'put', data });
}

/** 作用：删除收货地址。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function deleteCompanyAddress(addressId: IdLike, params?: { targetCompanyId?: number }) {
  return request({ url: `/system/company-address/${addressId}`, method: 'delete', params });
}

/** 作用：将指定地址设为默认。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function setDefaultCompanyAddress(addressId: IdLike, params?: { targetCompanyId?: number }) {
  return request({ url: `/system/company-address/${addressId}/default`, method: 'put', params });
}
