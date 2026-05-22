import { request } from '../request';

/**
 * 组织与客商域接口：公司类型/档案、合同与区域级联、外部客户导入等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
  /** 与 jasic-ui `views/org/company/index.vue` 分页一致
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  pageNum?: number;
  pageSize?: number;
  companyName?: string;
  typeCode?: string;
  category?: 'HQ' | 'FIRST_LEVEL' | 'SECOND_LEVEL';
  status?: 0 | 1;
  /** 主体类型（PLATFORM/HQ/SERVICE），首页组织治理跳转筛选 */
  subjectType?: 'PLATFORM' | 'HQ' | 'SERVICE';
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
/**
 * 作用：分页或列表查询（/org/company-type）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listCompanyType() {
  return request<SysCompanyType[]>({ url: '/org/company-type/list', method: 'get' });
}

/**
 * 作用：新增（/org/company-type）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function addCompanyType(data: SysCompanyType) {
  return request<number>({ url: '/org/company-type', method: 'post', data });
}

/**
 * 作用：更新或保存（/org/company-type/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function updateCompanyType(data: SysCompanyType) {
  return request({ url: '/org/company-type', method: 'put', data });
}

/**
 * 作用：删除或清理（/org/company-type/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function deleteCompanyType(id: IdLike) {
  return request({ url: `/org/company-type/${id}`, method: 'delete' });
}

// company
/**
 * 作用：分页或列表查询（/org/company/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listCompany(params?: SysCompanyQuery) {
  return request<SysCompanyPageResult>({ url: '/org/company/list', method: 'get', params });
}

/**
 * 作用：查询详情或选项（/org/company）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getCompany(id: IdLike) {
  return request({ url: `/org/company/${id}`, method: 'get' });
}

/**
 * 作用：新增（/org/company）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function addCompany(data: SysCompanyDTO) {
  return request({ url: '/org/company', method: 'post', data });
}

/**
 * 作用：更新或保存（/org/company/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function updateCompany(data: SysCompanyDTO) {
  return request({ url: '/org/company', method: 'put', data });
}

/**
 * 作用：删除或清理（/org/company/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function deleteCompany(id: IdLike) {
  return request({ url: `/org/company/${id}`, method: 'delete' });
}

// contract
/**
 * 作用：分页或列表查询（/org/contract/hq-first）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listHqFirstContract(params?: Query) {
  return request<HqFirstContractPageResult>({ url: '/org/contract/hq-first/list', method: 'get', params });
}

/**
 * 作用：新增（/org/contract/hq-first）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function addHqFirstContract(data: Query) {
  return request({ url: '/org/contract/hq-first', method: 'post', data });
}

/**
 * 作用：更新或保存（/org/contract/hq-first/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function updateHqFirstContract(data: Query) {
  return request({ url: '/org/contract/hq-first', method: 'put', data });
}

/**
 * 作用：删除或清理（/org/contract/hq-first/crm-import/list）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function deleteHqFirstContract(id: IdLike, params?: Query) {
  return request({ url: `/org/contract/hq-first/${id}`, method: 'delete', params });
}

/**
 * 作用：分页或列表查询（/org/contract/hq-first/crm-import）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listCrmHqFirstContractImport(params?: Query) {
  return request({ url: '/org/contract/hq-first/crm-import/list', method: 'get', params });
}

/**
 * 作用：导入（/org/contract/hq-first/crm-import）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function importCrmHqFirstContract(data?: Query) {
  return request({ url: '/org/contract/hq-first/crm-import', method: 'post', data });
}

/** 与 jasic-ui `listFirstSecondRelation` 同路径
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listFirstSecondRelation(params?: Query) {
  return request({ url: '/org/contract/first-second/list', method: 'get', params });
}

/**
 * 作用：分页或列表查询（/org/contract/first-second/crm-import）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listCrmFirstSecondRelationImport(params?: Query) {
  return request({ url: '/org/contract/first-second/crm-import/list', method: 'get', params });
}

/**
 * 作用：导入（/org/contract/first-second/crm-import）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function importCrmFirstSecondRelation(data?: Query) {
  return request({ url: '/org/contract/first-second/crm-import', method: 'post', data });
}

/**
 * 作用：新增（/org/contract/first-second/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function addFirstSecondRelation(data: Query) {
  return request({ url: '/org/contract/first-second', method: 'post', data });
}

/**
 * 作用：删除或清理（/org/contract/first-second/${id}）。
 * @returns 接口 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function deleteFirstSecondRelation(id: IdLike, params?: Query) {
  return request({ url: `/org/contract/first-second/${id}`, method: 'delete', params });
}

/** @deprecated 使用 listFirstSecondRelation
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const listFirstSecondContract = listFirstSecondRelation;
/** @deprecated 使用 addFirstSecondRelation
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const addFirstSecondContract = addFirstSecondRelation;
/** @deprecated 使用 deleteFirstSecondRelation
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const deleteFirstSecondContract = deleteFirstSecondRelation;

/** 与 jasic-ui `GET /org/company/external/list` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listExternalCompany(params?: Query) {
  return request<{ total: number; records: ExternalCompanyVO[] }>({
    url: '/org/company/external/list',
    method: 'get',
    params
  });
}

/** 与 jasic-ui `GET /org/company/external/:custId/import-preview` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getExternalCompanyImportPreview(custId: IdLike) {
  return request({ url: `/org/company/external/${custId}/import-preview`, method: 'get' });
}

/** 与后端 `SysAreaOptionVO` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface SysAreaOptionVO {
  areaCode: string;
  areaName: string;
  parentCode?: string;
  areaLevel?: string;
  leaf?: boolean;
}

/** 与后端 `SysArea` 实体一致（接口常用字段）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface SysArea {
  areaCode: string;
  areaName: string;
  parentCode?: string;
  areaLevel?: string;
  fullName?: string;
}

/** 与 jasic-ui `GET /org/area/options` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listAreaOptions(parentCode?: string) {
  return request<SysAreaOptionVO[]>({ url: '/org/area/options', method: 'get', params: { parentCode } });
}

/** 与 jasic-ui `GET /org/area/:areaCode` 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getAreaDetail(areaCode: IdLike) {
  return request<SysArea>({ url: `/org/area/${areaCode}`, method: 'get' });
}
