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

export function getNotifyTracePage(params) {
  return request({ url: '/system/notify/trace/page', method: 'get', params })
}

export function getNotifyTraceEvent(eventId) {
  return request({ url: `/system/notify/trace/event/${eventId}`, method: 'get' })
}

export function getNotifyTraceDispatch(dispatchId) {
  return request({ url: `/system/notify/trace/dispatch/${dispatchId}`, method: 'get' })
}

export function retryNotifyTraceEvent(eventId) {
  return request({ url: `/system/notify/trace/event/${eventId}/retry`, method: 'post' })
}

export function retryNotifyTraceDispatch(dispatchId) {
  return request({ url: `/system/notify/trace/dispatch/${dispatchId}/retry`, method: 'post' })
}

export function deadNotifyTraceEvent(eventId, data) {
  return request({ url: `/system/notify/trace/event/${eventId}/dead`, method: 'post', data })
}

export function deadNotifyTraceDispatch(dispatchId, data) {
  return request({ url: `/system/notify/trace/dispatch/${dispatchId}/dead`, method: 'post', data })
}
