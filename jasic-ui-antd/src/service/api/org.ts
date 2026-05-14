import { request } from '../request';

/**
 * 组织与客商域接口：公司类型/档案、合同与区域级联、外部客户导入等。
 */

type IdLike = string | number;
type Query = Record<string, unknown>;

export interface SysCompanyType {
  id?: number;
  typeCode: string;
  typeName: string;
  subjectType?: 'PLATFORM' | 'HQ' | 'SERVICE';
  remark?: string;
  orderNum?: number;
}

export interface SysCompanyQuery extends Query {
  /** 与 jasic-ui `views/org/company/index.vue` 分页一致 */
  pageNum?: number;
  pageSize?: number;
  companyName?: string;
  typeCode?: string;
  category?: 'HQ' | 'FIRST_LEVEL' | 'SECOND_LEVEL';
  status?: 0 | 1;
}

export interface SysCompany {
  id: number;
  companyName: string;
  companyShortName?: string;
  companyCode?: string;
  typeCode?: string;
  contactName?: string;
  contactPhone?: string;
  provinceCode?: string;
  provinceName?: string;
  cityCode?: string;
  cityName?: string;
  districtCode?: string;
  districtName?: string;
  detailAddress?: string;
  fullAddress?: string;
  geocodeStatus?: string;
  servicePhone?: string;
  sourceType?: string;
  salesOrg?: string;
  status?: number;
  remark?: string;
}

export interface SysCompanyPageResult {
  total: number;
  records: SysCompany[];
}

export interface SysCompanyDTO {
  id?: number;
  companyName: string;
  companyShortName?: string;
  companyCode?: string;
  typeCode: string;
  contactName: string;
  contactPhone: string;
  provinceCode: string;
  provinceName?: string;
  cityCode: string;
  cityName?: string;
  districtCode: string;
  districtName?: string;
  detailAddress: string;
  adminUsername?: string;
  servicePhone?: string;
  sourceType?: string;
  salesOrg?: string;
  status?: 0 | 1;
  remark?: string;
}

export interface HqFirstContractVO {
  id: number;
  hqCompanyId?: number;
  firstCompanyId?: number;
  regionId?: number;
  hqCompanyName?: string;
  firstCompanyName?: string;
  regionName?: string;
  contractNo?: string;
  status?: 0 | 1;
  remark?: string;
  contractTime?: string;
  createTime?: string;
}

export interface HqFirstContractPageResult {
  total: number;
  records: HqFirstContractVO[];
}

export interface ExternalCompanyVO {
  id: number;
  companyName?: string;
  companyCode?: string;
  contactName?: string;
  contactPhone?: string;
  status?: number;
}

// company type
export function listCompanyType() {
  return request<SysCompanyType[]>({ url: '/org/company-type/list', method: 'get' });
}

export function addCompanyType(data: SysCompanyType) {
  return request<number>({ url: '/org/company-type', method: 'post', data });
}

export function updateCompanyType(data: SysCompanyType) {
  return request({ url: '/org/company-type', method: 'put', data });
}

export function deleteCompanyType(id: IdLike) {
  return request({ url: `/org/company-type/${id}`, method: 'delete' });
}

// company
export function listCompany(params?: SysCompanyQuery) {
  return request<SysCompanyPageResult>({ url: '/org/company/list', method: 'get', params });
}

export function getCompany(id: IdLike) {
  return request({ url: `/org/company/${id}`, method: 'get' });
}

export function addCompany(data: SysCompanyDTO) {
  return request({ url: '/org/company', method: 'post', data });
}

export function updateCompany(data: SysCompanyDTO) {
  return request({ url: '/org/company', method: 'put', data });
}

export function deleteCompany(id: IdLike) {
  return request({ url: `/org/company/${id}`, method: 'delete' });
}

// contract
export function listHqFirstContract(params?: Query) {
  return request<HqFirstContractPageResult>({ url: '/org/contract/hq-first/list', method: 'get', params });
}

export function addHqFirstContract(data: Query) {
  return request({ url: '/org/contract/hq-first', method: 'post', data });
}

export function updateHqFirstContract(data: Query) {
  return request({ url: '/org/contract/hq-first', method: 'put', data });
}

export function deleteHqFirstContract(id: IdLike, params?: Query) {
  return request({ url: `/org/contract/hq-first/${id}`, method: 'delete', params });
}

export function listCrmHqFirstContractImport(params?: Query) {
  return request({ url: '/org/contract/hq-first/crm-import/list', method: 'get', params });
}

export function importCrmHqFirstContract(data?: Query) {
  return request({ url: '/org/contract/hq-first/crm-import', method: 'post', data });
}

/** 与 jasic-ui `listFirstSecondRelation` 同路径 */
export function listFirstSecondRelation(params?: Query) {
  return request({ url: '/org/contract/first-second/list', method: 'get', params });
}

export function listCrmFirstSecondRelationImport(params?: Query) {
  return request({ url: '/org/contract/first-second/crm-import/list', method: 'get', params });
}

export function importCrmFirstSecondRelation(data?: Query) {
  return request({ url: '/org/contract/first-second/crm-import', method: 'post', data });
}

export function addFirstSecondRelation(data: Query) {
  return request({ url: '/org/contract/first-second', method: 'post', data });
}

export function deleteFirstSecondRelation(id: IdLike, params?: Query) {
  return request({ url: `/org/contract/first-second/${id}`, method: 'delete', params });
}

/** @deprecated 使用 listFirstSecondRelation */
export const listFirstSecondContract = listFirstSecondRelation;
/** @deprecated 使用 addFirstSecondRelation */
export const addFirstSecondContract = addFirstSecondRelation;
/** @deprecated 使用 deleteFirstSecondRelation */
export const deleteFirstSecondContract = deleteFirstSecondRelation;

/** 与 jasic-ui `GET /org/company/external/list` 一致 */
export function listExternalCompany(params?: Query) {
  return request<{ total: number; records: ExternalCompanyVO[] }>({
    url: '/org/company/external/list',
    method: 'get',
    params
  });
}

/** 与 jasic-ui `GET /org/company/external/:custId/import-preview` 一致 */
export function getExternalCompanyImportPreview(custId: IdLike) {
  return request({ url: `/org/company/external/${custId}/import-preview`, method: 'get' });
}

/** 与后端 `SysAreaOptionVO` 一致 */
export interface SysAreaOptionVO {
  areaCode: string;
  areaName: string;
  parentCode?: string;
  areaLevel?: string;
  leaf?: boolean;
}

/** 与后端 `SysArea` 实体一致（接口常用字段） */
export interface SysArea {
  areaCode: string;
  areaName: string;
  parentCode?: string;
  areaLevel?: string;
  fullName?: string;
}

/** 与 jasic-ui `GET /org/area/options` 一致 */
export function listAreaOptions(parentCode?: string) {
  return request<SysAreaOptionVO[]>({ url: '/org/area/options', method: 'get', params: { parentCode } });
}

/** 与 jasic-ui `GET /org/area/:areaCode` 一致 */
export function getAreaDetail(areaCode: IdLike) {
  return request<SysArea>({ url: `/org/area/${areaCode}`, method: 'get' });
}
