import request from '@/utils/request'

export function listWorkOrder(params) {
  return request({ url: '/system/work-order/list', method: 'get', params })
}

export function countWorkOrderStatus(params) {
  return request({ url: '/system/work-order/status-count', method: 'get', params })
}

export function getWorkOrder(workOrderId) {
  return request({ url: `/system/work-order/${workOrderId}`, method: 'get' })
}

export function listCreateHqOptions() {
  return request({ url: '/system/work-order/create-hq-options', method: 'get' })
}

export function getProxyCreateBarcodeInfo(params) {
  return request({ url: '/system/work-order/create/proxy/barcode-info', method: 'get', params })
}

export function getUpstreamFirstCreateBarcodeInfo(params) {
  return request({ url: '/system/work-order/create/upstream-first/barcode-info', method: 'get', params })
}

export function listUpstreamFirstCreateTargetOptions() {
  return request({ url: '/system/work-order/create/upstream-first/target-options', method: 'get' })
}

export function getUpstreamHqCreateBarcodeInfo(params) {
  return request({ url: '/system/work-order/create/upstream-hq/barcode-info', method: 'get', params })
}

export function listAssignUserOptions(workOrderId) {
  return request({ url: `/system/work-order/${workOrderId}/assign-user-options`, method: 'get' })
}

export function listTransferTargetOptions(workOrderId) {
  return request({ url: `/system/work-order/${workOrderId}/transfer-target-options`, method: 'get' })
}

export function listRepairFaultOptions(workOrderId) {
  return request({ url: `/system/work-order/${workOrderId}/repair-fault-options`, method: 'get' })
}

export function listRepairProductModelOptions(workOrderId, params) {
  return request({ url: `/system/work-order/${workOrderId}/repair-product-model-options`, method: 'get', params })
}

export function createProxyWorkOrder(data) {
  return request({ url: '/system/work-order/create/proxy', method: 'post', data })
}

export function createUpstreamFirstWorkOrder(data) {
  return request({ url: '/system/work-order/create/upstream-first', method: 'post', data })
}

export function createUpstreamHqWorkOrder(data) {
  return request({ url: '/system/work-order/create/upstream-hq', method: 'post', data })
}

export function assignWorkOrder(data) {
  return request({ url: '/system/work-order/assign', method: 'put', data })
}

export function techAcceptWorkOrder(data) {
  return request({ url: '/system/work-order/tech-accept', method: 'put', data })
}

export function transferWorkOrder(data) {
  return request({ url: '/system/work-order/transfer', method: 'put', data })
}

export function repairWorkOrder(data) {
  return request({ url: '/system/work-order/repair', method: 'post', data })
}

export function reviewWorkOrder(data) {
  return request({ url: '/system/work-order/review', method: 'post', data })
}

export function updateRepairProductModel(data) {
  return request({ url: '/system/work-order/repair-product-model', method: 'put', data })
}

export function updateWorkOrderSendExpress(data) {
  return request({ url: '/system/work-order/send-express', method: 'put', data })
}

export function closeWorkOrder(data) {
  return request({ url: '/system/work-order/close', method: 'put', data })
}
