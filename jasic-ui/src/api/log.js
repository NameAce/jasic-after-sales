import request from '@/utils/request'

export function listOperLog(params) {
  return request({ url: '/log/oper-log/list', method: 'get', params })
}

export function deleteOperLog(ids) {
  return request({ url: `/log/oper-log/${ids}`, method: 'delete' })
}

export function cleanOperLog() {
  return request({ url: '/log/oper-log/clean', method: 'delete' })
}
