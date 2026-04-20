import request from '@/utils/request'

export function getNotifyTodoCount() {
  return request({ url: '/system/notify/todo/count', method: 'get' })
}

export function getNotifyTodoPage(params) {
  return request({ url: '/system/notify/todo/page', method: 'get', params })
}

export function markNotifyMessageRead(messageId) {
  return request({ url: `/system/notify/message/${messageId}/read`, method: 'post' })
}
