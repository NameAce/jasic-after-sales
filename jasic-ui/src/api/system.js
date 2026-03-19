import request from '@/utils/request'

// --- 用户管理 ---
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

// --- 角色管理 ---
export function listRole(params) {
  return request({ url: '/system/role/list', method: 'get', params })
}
export function roleOptions() {
  return request({ url: '/system/role/options', method: 'get' })
}
export function getRole(roleId) {
  return request({ url: `/system/role/${roleId}`, method: 'get' })
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

// --- 菜单管理 ---
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
export function copyMenus(data) {
  return request({ url: '/system/menu/copy', method: 'post', data })
}

// --- 角色模板 ---
export function listRoleTemplate(typeCode) {
  return request({ url: '/system/role-template/list', method: 'get', params: { typeCode } })
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

// --- 大区管理 ---
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
