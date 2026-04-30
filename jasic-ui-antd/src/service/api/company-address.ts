/**
 * 公司地址簿：联系人、默认地址等维护接口。
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
}

export interface CompanyAddressUpdateDTO extends CompanyAddressCreateDTO {
  id: number;
}

/** 作用：查询当前公司收货地址列表。 */
export function listCompanyAddress() {
  return request<CompanyAddressVO[]>({ url: '/system/company-address/list', method: 'get' });
}

/** 作用：新增收货地址。 */
export function createCompanyAddress(data: CompanyAddressCreateDTO) {
  return request<number>({ url: '/system/company-address', method: 'post', data });
}

/** 作用：更新收货地址。 */
export function updateCompanyAddress(data: CompanyAddressUpdateDTO) {
  return request({ url: '/system/company-address', method: 'put', data });
}

/** 作用：删除收货地址。 */
export function deleteCompanyAddress(addressId: IdLike) {
  return request({ url: `/system/company-address/${addressId}`, method: 'delete' });
}

/** 作用：将指定地址设为默认。 */
export function setDefaultCompanyAddress(addressId: IdLike) {
  return request({ url: `/system/company-address/${addressId}/default`, method: 'put' });
}
