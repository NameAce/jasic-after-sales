import { request } from '../request';

/**
 * 系统管理域接口：用户/角色/菜单/字典/通知模板/同步任务等后台配置与运维。
 */

type IdLike = string | number;
type Query = Record<string, unknown>;

export interface SysUserQuery extends Query {
  /** 与 jasic-ui `views/system/user/index.vue` 的 `queryParams` 及后端列表一致 */
  pageNum?: number;
  pageSize?: number;
  username?: string;
  realName?: string;
  phone?: string;
  email?: string;
  status?: number;
  targetCompanyId?: number;
}

export interface SysUserDTO {
  id?: number;
  username: string;
  password?: string;
  realName?: string;
  phone: string;
  email?: string;
  sex?: number;
  status?: number;
  remark?: string;
  targetCompanyId?: number;
  roleIds?: number[];
}

export interface ResetUserPasswordDTO {
  userId: IdLike;
  targetCompanyId?: number;
  newPassword?: string;
}

export interface SysUserVO {
  id: number;
  username: string;
  realName?: string;
  phone?: string;
  email?: string;
  avatar?: string;
  sex?: number;
  status?: number;
  remark?: string;
  currentCompanyId?: number;
  currentCompanyName?: string;
  currentTypeCode?: string;
  currentSubjectType?: string;
  perms?: string[];
  createTime?: string;
}

export interface SysRoleQuery extends Query {
  /** 与 jasic-ui `views/system/role/index.vue` 的 PageHelper 分页一致 */
  pageNum?: number;
  pageSize?: number;
  roleName?: string;
  roleKey?: string;
  status?: number;
  targetCompanyId?: number;
}

export interface SysRoleVO {
  id: number;
  companyId?: number;
  targetCompanyId?: number;
  roleName: string;
  roleKey: string;
  dataScope?: string;
  roleType?: number;
  isSystem?: number;
  status?: number;
  orderNum?: number;
  remark?: string;
  menuIds?: number[];
  createTime?: string;
}

export interface SysMenuDTO {
  id?: number;
  subjectType: string;
  menuName: string;
  parentId?: number;
  menuType: 'M' | 'C' | 'F';
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  orderNum?: number;
  isVisible?: number;
  status?: number;
  remark?: string;
}

export interface SysMenuVO {
  id: number;
  subjectType?: string;
  menuName: string;
  parentId?: number;
  menuType?: string;
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  orderNum?: number;
  isVisible?: number;
  status?: number;
  children?: SysMenuVO[];
}

interface PageResult<T> {
  total: number;
  records: T[];
  pageNum?: number;
  pageSize?: number;
}

// user
export function listUser(params?: SysUserQuery) {
  return request<PageResult<SysUserVO>>({ url: '/system/user/list', method: 'get', params });
}

export function getUser(userId: IdLike, params?: { targetCompanyId?: number }) {
  return request<SysUserVO>({ url: `/system/user/${userId}`, method: 'get', params });
}

export function addUser(data: SysUserDTO) {
  return request<number>({ url: '/system/user', method: 'post', data });
}

export function updateUser(data: SysUserDTO) {
  return request({ url: '/system/user', method: 'put', data });
}

export function deleteUser(userId: IdLike, params?: { targetCompanyId?: number }) {
  return request({ url: `/system/user/${userId}`, method: 'delete', params });
}

/** 与 jasic-ui `POST /system/user/:userId/kickout` */
export function kickoutUser(userId: IdLike, params?: { targetCompanyId?: number }) {
  return request({ url: `/system/user/${userId}/kickout`, method: 'post', params });
}

/** 与 jasic-ui `PUT /system/user/reset-pwd` 一致 */
export function resetPwd(data: ResetUserPasswordDTO) {
  return request({ url: '/system/user/reset-pwd', method: 'put', data });
}

// role
export function listRole(params?: SysRoleQuery) {
  return request<PageResult<SysRoleVO>>({ url: '/system/role/list', method: 'get', params });
}

export function roleOptions(params?: { targetCompanyId?: number }) {
  return request<SysRoleVO[]>({ url: '/system/role/options', method: 'get', params });
}

export function getRole(roleId: IdLike, params?: { targetCompanyId?: number }) {
  return request<SysRoleVO>({ url: `/system/role/${roleId}`, method: 'get', params });
}

export function addRole(data: SysRoleVO) {
  return request<number>({ url: '/system/role', method: 'post', data });
}

export function updateRole(data: SysRoleVO) {
  return request({ url: '/system/role', method: 'put', data });
}

export function deleteRole(roleId: IdLike, params?: { targetCompanyId?: number }) {
  return request({ url: `/system/role/${roleId}`, method: 'delete', params });
}

/** 与 jasic-ui `GET /system/role/data-scope-options` */
export function roleDataScopeOptions(params?: { targetCompanyId?: number }) {
  return request<unknown[]>({ url: '/system/role/data-scope-options', method: 'get', params });
}

/** 与 jasic-ui `PUT /system/role/:roleId/menus` */
export function assignRoleMenus(roleId: IdLike, data: IdLike[], params?: { targetCompanyId?: number }) {
  return request({ url: `/system/role/${roleId}/menus`, method: 'put', params, data });
}

export function assignUserRoles(userId: IdLike, data: IdLike[], params?: { targetCompanyId?: number }) {
  return request({ url: `/system/user/${userId}/roles`, method: 'put', params, data });
}

/** 与 jasic-ui `GET /system/region/:userId/regions` */
export function getUserRegions(userId: IdLike, params?: { targetCompanyId?: number }) {
  return request<unknown[]>({ url: `/system/region/${userId}/regions`, method: 'get', params });
}

/** 与 jasic-ui `PUT /system/region/:userId/regions` */
export function assignUserRegions(userId: IdLike, data: unknown, params?: { targetCompanyId?: number }) {
  return request({ url: `/system/region/${userId}/regions`, method: 'put', params, data });
}

// menu
export function menuTree(subjectType?: string) {
  return request<SysMenuVO[]>({ url: '/system/menu/tree', method: 'get', params: { subjectType } });
}

export function menuList(subjectType?: string) {
  return request<SysMenuVO[]>({ url: '/system/menu/list', method: 'get', params: { subjectType } });
}

/** 与 jasic-ui `GET /system/menu/:menuId` */
export function getMenu(menuId: IdLike) {
  return request<SysMenuVO>({ url: `/system/menu/${menuId}`, method: 'get' });
}

export function addMenu(data: SysMenuDTO) {
  return request<number>({ url: '/system/menu', method: 'post', data });
}

export function updateMenu(data: SysMenuDTO) {
  return request({ url: '/system/menu', method: 'put', data });
}

export function deleteMenu(menuId: IdLike) {
  return request({ url: `/system/menu/${menuId}`, method: 'delete' });
}

/** 与 jasic-ui `GET /system/menu/type-code-tree` */
export function typeCodeMenuTree(typeCode: string) {
  return request<SysMenuVO[]>({ url: '/system/menu/type-code-tree', method: 'get', params: { typeCode } });
}

/** 与 jasic-ui `GET /system/menu/type-code-menu-ids` */
export function typeCodeMenuIds(typeCode: string) {
  return request<number[]>({ url: '/system/menu/type-code-menu-ids', method: 'get', params: { typeCode } });
}

/** 与 jasic-ui `PUT /system/menu/assign-type-code-menus` */
export function assignTypeCodeMenus(typeCode: string, menuIds: IdLike[]) {
  return request({
    url: '/system/menu/assign-type-code-menus',
    method: 'put',
    params: { typeCode },
    data: menuIds
  });
}

/** 与 jasic-ui `GET /system/menu/publish-options` */
export function menuPublishOptions(subjectType?: string) {
  return request<unknown[]>({ url: '/system/menu/publish-options', method: 'get', params: { subjectType } });
}

/** 与 jasic-ui `POST /system/menu/publish` */
export function publishMenu(data: Query) {
  return request({ url: '/system/menu/publish', method: 'post', data });
}

/** 与 jasic-ui `POST /system/menu/copy` */
export function copyMenus(data: Query) {
  return request({ url: '/system/menu/copy', method: 'post', data });
}

export function listDictType(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/dict/type/list', method: 'get', params });
}

/** 与 jasic-ui `GET /system/dict/type/:id` */
export function getDictType(id: IdLike) {
  return request<Query>({ url: `/system/dict/type/${id}`, method: 'get' });
}

/** 与 jasic-ui `POST /system/dict/type` */
export function addDictType(data: Query) {
  return request<number>({ url: '/system/dict/type', method: 'post', data });
}

/** 与 jasic-ui `PUT /system/dict/type` */
export function updateDictType(data: Query) {
  return request({ url: '/system/dict/type', method: 'put', data });
}

/** 与 jasic-ui `DELETE /system/dict/type/:id` */
export function deleteDictType(id: IdLike) {
  return request({ url: `/system/dict/type/${id}`, method: 'delete' });
}

/** 与 jasic-ui `DELETE /system/dict/type/refresh-cache` */
export function refreshDictTypeCache() {
  return request({ url: '/system/dict/type/refresh-cache', method: 'delete' });
}

export function listDictData(dictType: string, params?: Query) {
  return request<PageResult<Query>>({
    url: '/system/dict/data/list',
    method: 'get',
    params: { ...params, dictType }
  });
}

/** 与 jasic-ui `GET /system/dict/data/:id` */
export function getDictData(id: IdLike) {
  return request<Query>({ url: `/system/dict/data/${id}`, method: 'get' });
}

/** 与 jasic-ui `GET /system/dict/data/type/:dictType` */
export function listDictDataByType(dictType: string) {
  return request<Query[]>({ url: `/system/dict/data/type/${dictType}`, method: 'get' });
}

/** 与 jasic-ui `POST /system/dict/data` */
export function addDictData(data: Query) {
  return request<number>({ url: '/system/dict/data', method: 'post', data });
}

/** 与 jasic-ui `PUT /system/dict/data` */
export function updateDictData(data: Query) {
  return request({ url: '/system/dict/data', method: 'put', data });
}

/** 与 jasic-ui `DELETE /system/dict/data/:id` */
export function deleteDictData(id: IdLike) {
  return request({ url: `/system/dict/data/${id}`, method: 'delete' });
}

export function listSystemConfig(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/config/list', method: 'get', params });
}

/** 与 jasic-ui `GET /system/config/:id` */
export function getConfig(id: IdLike) {
  return request<Query>({ url: `/system/config/${id}`, method: 'get' });
}

/** 与 jasic-ui `GET /system/config/key/:configKey` */
export function getConfigByKey(configKey: string) {
  return request<Query>({ url: `/system/config/key/${configKey}`, method: 'get' });
}

/** 与 jasic-ui `POST /system/config` */
export function addConfig(data: Query) {
  return request<number>({ url: '/system/config', method: 'post', data });
}

/** 与 jasic-ui `PUT /system/config` */
export function updateConfig(data: Query) {
  return request({ url: '/system/config', method: 'put', data });
}

/** 与 jasic-ui `DELETE /system/config/:id` */
export function deleteConfig(id: IdLike) {
  return request({ url: `/system/config/${id}`, method: 'delete' });
}

/** 与 jasic-ui `DELETE /system/config/refresh-cache` */
export function refreshConfigCache() {
  return request({ url: '/system/config/refresh-cache', method: 'delete' });
}

export function listNotifyTemplate(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/notify/template/list', method: 'get', params });
}

/** 与 jasic-ui `GET /system/notify/template/:id` */
export function getNotifyTemplate(id: IdLike) {
  return request<Query>({ url: `/system/notify/template/${id}`, method: 'get' });
}

/** 与 jasic-ui `GET /system/notify/template/:templateCode/channels` */
export function listNotifyTemplateChannels(templateCode: string) {
  return request<unknown[]>({ url: `/system/notify/template/${templateCode}/channels`, method: 'get' });
}

/** 与 jasic-ui `PUT /system/notify/template/:templateCode/channels` */
export function saveNotifyTemplateChannels(templateCode: string, data: unknown) {
  return request({ url: `/system/notify/template/${templateCode}/channels`, method: 'put', data });
}

/** 与 jasic-ui `POST /system/notify/template/custom` */
export function addNotifyTemplateCustom(data: Query) {
  return request<number>({ url: '/system/notify/template/custom', method: 'post', data });
}

/** 与 jasic-ui `PUT /system/notify/template/custom` */
export function updateNotifyTemplateCustom(data: Query) {
  return request({ url: '/system/notify/template/custom', method: 'put', data });
}

/** 与 jasic-ui `DELETE /system/notify/template/custom/:id` */
export function deleteNotifyTemplateCustom(id: IdLike) {
  return request({ url: `/system/notify/template/custom/${id}`, method: 'delete' });
}

/** 与 jasic-ui `POST /system/notify/template/preview` */
export function previewNotifyTemplate(data: Query) {
  return request<Query>({ url: '/system/notify/template/preview', method: 'post', data });
}

/** 与 jasic-ui `POST /system/notify/template/refresh-cache` */
export function refreshNotifyTemplateCache() {
  return request({ url: '/system/notify/template/refresh-cache', method: 'post' });
}

/** 与 jasic-ui 机器条码档案列表 */
export function listMachineBarcode(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/machine-barcode/list', method: 'get', params });
}

/** 与 jasic-ui `GET /system/machine-barcode/:id` */
export function getMachineBarcode(id: IdLike, params?: Query) {
  return request<Query>({ url: `/system/machine-barcode/${id}`, method: 'get', params });
}

/** 机器条码档案可维护总部选项 */
export function listMachineBarcodeHqOptions() {
  return request<Query[]>({ url: '/system/machine-barcode/hq-options', method: 'get' });
}

/** 与 jasic-ui `POST /system/machine-barcode/full-sync` */
export function fullSyncMachineBarcode() {
  return request({ url: '/system/machine-barcode/full-sync', method: 'post' });
}

export function listSyncTask(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/sync-task/list', method: 'get', params });
}

/** 与 jasic-ui `GET /system/sync-task/:id` */
export function getSyncTask(id: IdLike) {
  return request<Query>({ url: `/system/sync-task/${id}`, method: 'get' });
}

/** 与 jasic-ui `GET /system/sync-task/handler-options` */
export function listSyncTaskHandlerOptions() {
  return request<Query[]>({ url: '/system/sync-task/handler-options', method: 'get' });
}

/** 与 jasic-ui `POST /system/sync-task` */
export function addSyncTask(data: Query) {
  return request<number>({ url: '/system/sync-task', method: 'post', data });
}

/** 与 jasic-ui `PUT /system/sync-task` */
export function updateSyncTask(data: Query) {
  return request({ url: '/system/sync-task', method: 'put', data });
}

/** 与 jasic-ui `POST /system/sync-task/:id/execute` */
export function executeSyncTask(id: IdLike) {
  return request({ url: `/system/sync-task/${id}/execute`, method: 'post' });
}

/** 与 jasic-ui `GET /system/sync-task/log/list` */
export function listSyncTaskLog(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/sync-task/log/list', method: 'get', params });
}

export function listFaultRepairConfig(params?: Query) {
  return request<PageResult<Query>>({ url: '/system/fault-repair-config/list', method: 'get', params });
}

export function getFaultRepairConfig(id: IdLike, params?: { ownerHqId?: IdLike }) {
  return request<Query>({ url: `/system/fault-repair-config/${id}`, method: 'get', params });
}

export function addFaultRepairConfig(data: Query) {
  return request<number>({ url: '/system/fault-repair-config', method: 'post', data });
}

export function updateFaultRepairConfig(data: Query) {
  return request({ url: '/system/fault-repair-config', method: 'put', data });
}

/** 与 jasic-ui `GET /system/fault-repair-config/company-options` */
export function listFaultRepairConfigCompanyOptions() {
  return request<Query[]>({ url: '/system/fault-repair-config/company-options', method: 'get' });
}

/** 与 jasic-ui `GET /system/role-template/list`（可选 typeCode） */
export function listRoleTemplate(typeCode?: string, params?: Query) {
  const p: Query = { ...(params || {}) };
  if (typeCode) p.typeCode = typeCode;
  return request<PageResult<Query>>({ url: '/system/role-template/list', method: 'get', params: p });
}

/** 与 jasic-ui `GET /system/role-template/data-scope-options` */
export function roleTemplateDataScopeOptions(typeCode: string) {
  return request<Query[]>({
    url: '/system/role-template/data-scope-options',
    method: 'get',
    params: { typeCode }
  });
}

/** 与 jasic-ui `GET /system/role-template/data-scope-option-map` */
export function roleTemplateDataScopeOptionMap() {
  return request<Record<string, string>>({
    url: '/system/role-template/data-scope-option-map',
    method: 'get'
  });
}

export function getRoleTemplate(id: IdLike) {
  return request<Query>({ url: `/system/role-template/${id}`, method: 'get' });
}

export function addRoleTemplate(data: Query) {
  return request<number>({ url: '/system/role-template', method: 'post', data });
}

export function updateRoleTemplate(data: Query) {
  return request({ url: '/system/role-template', method: 'put', data });
}

export function deleteRoleTemplate(id: IdLike) {
  return request({ url: `/system/role-template/${id}`, method: 'delete' });
}

/** 与 jasic-ui `POST /system/role-template/:templateId/sync` */
export function syncRoleTemplate(templateId: IdLike) {
  return request({ url: `/system/role-template/${templateId}/sync`, method: 'post' });
}

/** 与 jasic-ui `GET /system/region/list?targetCompanyId=`（返回大区列表，多为数组或统一包装） */
export function listRegion(targetCompanyId: IdLike, params?: Query) {
  return request<Query[] | PageResult<Query>>({
    url: '/system/region/list',
    method: 'get',
    params: { ...params, targetCompanyId }
  });
}

/** 与 jasic-ui `POST /system/region`，归属公司通过 targetCompanyId 表达。 */
export function addRegion(data: Query & { targetCompanyId?: IdLike }) {
  return request<number>({ url: '/system/region', method: 'post', data });
}

/** 与 jasic-ui `PUT /system/region`，归属公司通过 targetCompanyId 表达。 */
export function updateRegion(data: Query & { targetCompanyId?: IdLike }) {
  return request({ url: '/system/region', method: 'put', data });
}

/** 与 jasic-ui `DELETE /system/region/:id` */
export function deleteRegion(id: IdLike, params?: { targetCompanyId?: IdLike }) {
  return request({ url: `/system/region/${id}`, method: 'delete', params });
}
