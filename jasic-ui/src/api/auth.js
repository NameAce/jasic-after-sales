import request from '@/utils/request'

export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

export function chooseCompany(data) {
  return request({ url: '/auth/choose-company', method: 'post', data })
}

export function getUserInfo() {
  return request({ url: '/auth/user-info', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/auth/profile', method: 'put', data })
}

export function changePassword(data) {
  return request({ url: '/auth/change-password', method: 'put', data })
}

export function getMenus() {
  return request({ url: '/auth/menus', method: 'get' })
}

export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}

export function getWechatBindStatus() {
  return request({ url: '/auth/wechat-bind/status', method: 'get' })
}

export function createWechatBindCode() {
  return request({ url: '/auth/wechat-bind/code', method: 'post' })
}
