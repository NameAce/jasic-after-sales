import request from '@/utils/request'

// --- User ---
export function listUser(params) {
  return request({ url: '/system/user/list', method: 'get', params })
}
export function getUser(userId) {
  return request({ url: `/system/user/${userId}`, method: 'get' })
}
export function addUser(data) {
  return request({ url: '/system/user', method: 'post', data })
}
export function updateUser(data) {
  return request({ url: '/system/user', method: 'put', data })
}
export function deleteUser(userId) {
  return request({ url: `/system/user/${userId}`, method: 'delete' })
}
export function resetPwd(data) {
  return request({ url: '/system/user/reset-pwd', method: 'put', data })
}
export function kickoutUser(userId) {
  return request({ url: `/system/user/${userId}/kickout`, method: 'post' })
}
export function assignUserRoles(userId, data) {
  return request({ url: `/system/user/${userId}/roles`, method: 'put', data })
}
export function getUserRegions(userId) {
  return request({ url: `/system/region/${userId}/regions`, method: 'get' })
}

// --- Role ---
export function listRole(params) {
  return request({ url: '/system/role/list', method: 'get', params })
}
export function roleOptions() {
  return request({ url: '/system/role/options', method: 'get' })
}
export function getRole(roleId) {
  return request({ url: `/system/role/${roleId}`, method: 'get' })
}
export function roleDataScopeOptions() {
  return request({ url: '/system/role/data-scope-options', method: 'get' })
}
export function addRole(data) {
  return request({ url: '/system/role', method: 'post', data })
}
export function updateRole(data) {
  return request({ url: '/system/role', method: 'put', data })
}
export function deleteRole(roleId) {
  return request({ url: `/system/role/${roleId}`, method: 'delete' })
}
export function assignRoleMenus(roleId, data) {
  return request({ url: `/system/role/${roleId}/menus`, method: 'put', data })
}

// --- Menu ---
export function menuTree(subjectType) {
  return request({ url: '/system/menu/tree', method: 'get', params: { subjectType } })
}
export function menuList(subjectType) {
  return request({ url: '/system/menu/list', method: 'get', params: { subjectType } })
}
export function getMenu(menuId) {
  return request({ url: `/system/menu/${menuId}`, method: 'get' })
}
export function addMenu(data) {
  return request({ url: '/system/menu', method: 'post', data })
}
export function updateMenu(data) {
  return request({ url: '/system/menu', method: 'put', data })
}
export function deleteMenu(menuId) {
  return request({ url: `/system/menu/${menuId}`, method: 'delete' })
}
export function typeCodeMenuTree(typeCode) {
  return request({ url: '/system/menu/type-code-tree', method: 'get', params: { typeCode } })
}
export function typeCodeMenuIds(typeCode) {
  return request({ url: '/system/menu/type-code-menu-ids', method: 'get', params: { typeCode } })
}
export function assignTypeCodeMenus(typeCode, menuIds) {
  return request({ url: '/system/menu/assign-type-code-menus', method: 'put', params: { typeCode }, data: menuIds })
}
export function menuPublishOptions(subjectType) {
  return request({ url: '/system/menu/publish-options', method: 'get', params: { subjectType } })
}
export function publishMenu(data) {
  return request({ url: '/system/menu/publish', method: 'post', data })
}
export function copyMenus(data) {
  return request({ url: '/system/menu/copy', method: 'post', data })
}

// --- Dict Type ---
export function listDictType(params) {
  return request({ url: '/system/dict/type/list', method: 'get', params })
}
export function getDictType(id) {
  return request({ url: `/system/dict/type/${id}`, method: 'get' })
}
export function addDictType(data) {
  return request({ url: '/system/dict/type', method: 'post', data })
}
export function updateDictType(data) {
  return request({ url: '/system/dict/type', method: 'put', data })
}
export function deleteDictType(id) {
  return request({ url: `/system/dict/type/${id}`, method: 'delete' })
}
export function refreshDictTypeCache() {
  return request({ url: '/system/dict/type/refresh-cache', method: 'delete' })
}

// --- Dict Data ---
export function listDictData(params) {
  return request({ url: '/system/dict/data/list', method: 'get', params })
}
export function getDictData(id) {
  return request({ url: `/system/dict/data/${id}`, method: 'get' })
}
export function listDictDataByType(dictType) {
  return request({ url: `/system/dict/data/type/${dictType}`, method: 'get' })
}
export function addDictData(data) {
  return request({ url: '/system/dict/data', method: 'post', data })
}
export function updateDictData(data) {
  return request({ url: '/system/dict/data', method: 'put', data })
}
export function deleteDictData(id) {
  return request({ url: `/system/dict/data/${id}`, method: 'delete' })
}

// --- Config ---
export function listConfig(params) {
  return request({ url: '/system/config/list', method: 'get', params })
}
export function getConfig(id) {
  return request({ url: `/system/config/${id}`, method: 'get' })
}
export function getConfigByKey(configKey) {
  return request({ url: `/system/config/key/${configKey}`, method: 'get' })
}
export function addConfig(data) {
  return request({ url: '/system/config', method: 'post', data })
}
export function updateConfig(data) {
  return request({ url: '/system/config', method: 'put', data })
}
export function deleteConfig(id) {
  return request({ url: `/system/config/${id}`, method: 'delete' })
}
export function refreshConfigCache() {
  return request({ url: '/system/config/refresh-cache', method: 'delete' })
}

// --- Notify Template ---
export function listNotifyTemplate(params) {
  return request({ url: '/system/notify/template/list', method: 'get', params })
}
export function getNotifyTemplate(id) {
  return request({ url: `/system/notify/template/${id}`, method: 'get' })
}
export function listNotifyTemplateChannels(templateCode) {
  return request({ url: `/system/notify/template/${templateCode}/channels`, method: 'get' })
}
export function saveNotifyTemplateChannels(templateCode, data) {
  return request({ url: `/system/notify/template/${templateCode}/channels`, method: 'put', data })
}
export function addNotifyTemplateCustom(data) {
  return request({ url: '/system/notify/template/custom', method: 'post', data })
}
export function updateNotifyTemplateCustom(data) {
  return request({ url: '/system/notify/template/custom', method: 'put', data })
}
export function deleteNotifyTemplateCustom(id) {
  return request({ url: `/system/notify/template/custom/${id}`, method: 'delete' })
}
export function previewNotifyTemplate(data) {
  return request({ url: '/system/notify/template/preview', method: 'post', data })
}
export function refreshNotifyTemplateCache() {
  return request({ url: '/system/notify/template/refresh-cache', method: 'post' })
}

// --- Machine Barcode ---
export function listMachineBarcode(params) {
  return request({ url: '/system/machine-barcode/list', method: 'get', params })
}
export function getMachineBarcode(id) {
  return request({ url: `/system/machine-barcode/${id}`, method: 'get' })
}
export function fullSyncMachineBarcode() {
  return request({ url: '/system/machine-barcode/full-sync', method: 'post' })
}

// --- Sync Task ---
export function listSyncTask(params) {
  return request({ url: '/system/sync-task/list', method: 'get', params })
}
export function getSyncTask(id) {
  return request({ url: `/system/sync-task/${id}`, method: 'get' })
}
export function listSyncTaskHandlerOptions() {
  return request({ url: '/system/sync-task/handler-options', method: 'get' })
}
export function addSyncTask(data) {
  return request({ url: '/system/sync-task', method: 'post', data })
}
export function updateSyncTask(data) {
  return request({ url: '/system/sync-task', method: 'put', data })
}
export function executeSyncTask(id) {
  return request({ url: `/system/sync-task/${id}/execute`, method: 'post' })
}
export function listSyncTaskLog(params) {
  return request({ url: '/system/sync-task/log/list', method: 'get', params })
}

// --- Fault Repair Config ---
export function listFaultRepairConfig(params) {
  return request({ url: '/system/fault-repair-config/list', method: 'get', params })
}
export function getFaultRepairConfig(id) {
  return request({ url: `/system/fault-repair-config/${id}`, method: 'get' })
}
export function listFaultRepairConfigCompanyOptions() {
  return request({ url: '/system/fault-repair-config/company-options', method: 'get' })
}
export function addFaultRepairConfig(data) {
  return request({ url: '/system/fault-repair-config', method: 'post', data })
}
export function updateFaultRepairConfig(data) {
  return request({ url: '/system/fault-repair-config', method: 'put', data })
}

// --- Role Template ---
export function listRoleTemplate(typeCode) {
  const params = typeCode ? { typeCode } : {}
  return request({ url: '/system/role-template/list', method: 'get', params })
}
export function roleTemplateDataScopeOptions(typeCode) {
  return request({ url: '/system/role-template/data-scope-options', method: 'get', params: { typeCode } })
}
export function roleTemplateDataScopeOptionMap() {
  return request({ url: '/system/role-template/data-scope-option-map', method: 'get' })
}
export function getRoleTemplate(templateId) {
  return request({ url: `/system/role-template/${templateId}`, method: 'get' })
}
export function addRoleTemplate(data) {
  return request({ url: '/system/role-template', method: 'post', data })
}
export function updateRoleTemplate(data) {
  return request({ url: '/system/role-template', method: 'put', data })
}
export function deleteRoleTemplate(templateId) {
  return request({ url: `/system/role-template/${templateId}`, method: 'delete' })
}
export function syncRoleTemplate(templateId) {
  return request({ url: `/system/role-template/${templateId}/sync`, method: 'post' })
}

// --- Region ---
export function listRegion(companyId) {
  return request({ url: '/system/region/list', method: 'get', params: { companyId } })
}
export function addRegion(data) {
  return request({ url: '/system/region', method: 'post', data })
}
export function updateRegion(data) {
  return request({ url: '/system/region', method: 'put', data })
}
export function deleteRegion(id) {
  return request({ url: `/system/region/${id}`, method: 'delete' })
}
export function assignUserRegions(userId, data) {
  return request({ url: `/system/region/${userId}/regions`, method: 'put', data })
}
