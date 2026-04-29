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

export function listCompanyAddress() {
  return request<CompanyAddressVO[]>({ url: '/system/company-address/list', method: 'get' });
}

export function createCompanyAddress(data: CompanyAddressCreateDTO) {
  return request<number>({ url: '/system/company-address', method: 'post', data });
}

export function updateCompanyAddress(data: CompanyAddressUpdateDTO) {
  return request({ url: '/system/company-address', method: 'put', data });
}

export function deleteCompanyAddress(addressId: IdLike) {
  return request({ url: `/system/company-address/${addressId}`, method: 'delete' });
}

export function setDefaultCompanyAddress(addressId: IdLike) {
  return request({ url: `/system/company-address/${addressId}/default`, method: 'put' });
}
