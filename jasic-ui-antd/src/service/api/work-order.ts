import { request } from '../request';

/**
 * 售后工单域接口：列表、状态统计、创建/指派/流转/维修/关闭等全生命周期。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */

type IdLike = string | number;
type Query = Record<string, unknown>;

export interface WorkOrderQuery extends Query {
  pageNum?: number;
  pageSize?: number;
  viewScope?: 'CURRENT' | 'HISTORY' | 'ALL';
  orderNo?: string;
  customerName?: string;
  customerMobile?: string;
  barcode?: string;
  mainStatus?: 'PENDING_ASSIGN' | 'PENDING_TECH_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';
  displayStatus?: 'ALL' | 'WAIT_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';
  hasTransfer?: 0 | 1;
}

/** `GET /system/work-order/status-count`：仅提交前端筛选字段，不提交权限上下文字段。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export type WorkOrderStatusCountQuery = Pick<
  WorkOrderQuery,
  'viewScope' | 'orderNo' | 'customerName' | 'customerMobile' | 'barcode' | 'hasTransfer'
>;

export interface WorkOrderListVO {
  id: number;
  orderNo: string;
  customerName?: string;
  customerMobile?: string;
  barcode?: string;
  brandType?: 'JASIC' | 'NON_JASIC';
  brandTypeLabel?: string;
  serviceMode?: 'MAIL' | 'STORE';
  serviceModeLabel?: string;
  lastOutDate?: string;
  warrantyStatus?: 'IN_WARRANTY' | 'OUT_OF_WARRANTY';
  productModel?: string;
  faultDesc?: string;
  mainStatus?: string;
  mainStatusLabel?: string;
  displayStatus?: string;
  currentAcceptCompanyId?: number;
  currentAcceptCompanyName?: string;
  currentAcceptCompanyPhone?: string;
  assignedUserId?: number;
  assignedUserName?: string;
  hasTransfer?: number;
  transferCount?: number;
  quoteAmount?: number;
  /** 当前列表项可执行动作编码（viewScope=CURRENT 时由后端计算） */
  availableActions?: string[];
  /** 无可执行动作时的只读说明 */
  readonlyReason?: string;
  createTime?: string;
}

export interface WorkOrderPageResult {
  total: number;
  records: WorkOrderListVO[];
}

export interface WorkOrderStatusCountVO {
  mainStatus: string;
  displayStatus: string;
  countNum: number;
}

export interface WorkOrderProxyCreateDTO {
  customerName?: string;
  customerMobile: string;
  barcode?: string;
  serviceMode: 'MAIL' | 'STORE';
  faultItems?: string[];
  faultRemark?: string;
  faultImageFileIds?: number[];
  faultVideoFileIds?: number[];
  faultVoiceFileIds?: number[];
  senderName?: string;
  senderMobile?: string;
  senderAddress?: string;
  sendExpressNo?: string;
  senderVoucherFileIds?: number[];
}

export interface WorkOrderAssignDTO {
  workOrderId: number;
  assignedUserId: number;
}

export interface WorkOrderTransferDTO {
  workOrderId: number;
  targetCompanyId: number;
  remark?: string;
}

export interface WorkOrderRepairPartItemDTO {
  partName?: string;
  partQty?: number;
}

export interface WorkOrderRepairDTO {
  workOrderId: number;
  quoteAmount?: number;
  quoteDesc?: string;
  faultItems?: string[];
  faultRemark?: string;
  repairDesc?: string;
  repairItems?: string[];
  otherDesc?: string;
  partList?: WorkOrderRepairPartItemDTO[];
  faultOldImageFileIds?: number[];
  faultNewImageFileIds?: number[];
  machineImageFileIds?: number[];
  machineBarcodeImageFileIds?: number[];
  otherImageFileIds?: number[];
}

export interface WorkOrderReviewDTO {
  workOrderId: number;
  repairDesc?: string;
  repairItems?: string[];
  otherDesc?: string;
  partList?: WorkOrderRepairPartItemDTO[];
  faultOldImageFileIds?: number[];
  faultNewImageFileIds?: number[];
  machineImageFileIds?: number[];
  machineBarcodeImageFileIds?: number[];
  otherImageFileIds?: number[];
}

export interface WorkOrderCloseDTO {
  workOrderId: number;
  returnMethod: string;
  returnExpressNo?: string;
  returnVoucherFileIds?: number[];
  closeReason: string;
}

export interface WorkOrderCreateOptionsVO {
  brandTypeOptions?: Array<{ label: string; value: string }>;
  serviceModeOptions?: Array<{ label: string; value: string }>;
  faultItems?: Array<{ label: string; value: string }>;
}

export function listWorkOrder(params?: WorkOrderQuery) {
  return request<WorkOrderPageResult>({ url: '/system/work-order/list', method: 'get', params });
}

export function countWorkOrderStatus(params?: WorkOrderStatusCountQuery) {
  return request<WorkOrderStatusCountVO[]>({ url: '/system/work-order/status-count', method: 'get', params });
}

export function getWorkOrder(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}`, method: 'get' });
}

export function listCreateHqOptions() {
  return request<WorkOrderCreateOptionsVO>({ url: '/system/work-order/create-hq-options', method: 'get' });
}

export function getProxyCreateBarcodeInfo(params?: Query) {
  return request({ url: '/system/work-order/create/proxy/barcode-info', method: 'get', params });
}

export function getUpstreamFirstCreateBarcodeInfo(params?: Query) {
  return request({ url: '/system/work-order/create/upstream-first/barcode-info', method: 'get', params });
}

export function listUpstreamFirstCreateTargetOptions() {
  return request({ url: '/system/work-order/create/upstream-first/target-options', method: 'get' });
}

export function getUpstreamHqCreateBarcodeInfo(params?: Query) {
  return request({ url: '/system/work-order/create/upstream-hq/barcode-info', method: 'get', params });
}

export function listAssignUserOptions(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}/assign-user-options`, method: 'get' });
}

export function listTransferTargetOptions(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}/transfer-target-options`, method: 'get' });
}

export function listRepairFaultOptions(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}/repair-fault-options`, method: 'get' });
}

export function listRepairProductModelOptions(workOrderId: IdLike, params?: Query) {
  return request({ url: `/system/work-order/${workOrderId}/repair-product-model-options`, method: 'get', params });
}

export function createProxyWorkOrder(data: WorkOrderProxyCreateDTO) {
  return request({ url: '/system/work-order/create/proxy', method: 'post', data });
}

export function createUpstreamFirstWorkOrder(data: Query) {
  return request({ url: '/system/work-order/create/upstream-first', method: 'post', data });
}

export function createUpstreamHqWorkOrder(data: Query) {
  return request({ url: '/system/work-order/create/upstream-hq', method: 'post', data });
}

export function assignWorkOrder(data: WorkOrderAssignDTO) {
  return request({ url: '/system/work-order/assign', method: 'put', data });
}

export function techAcceptWorkOrder(data: Query) {
  return request({ url: '/system/work-order/tech-accept', method: 'put', data });
}

export function transferWorkOrder(data: WorkOrderTransferDTO) {
  return request({ url: '/system/work-order/transfer', method: 'put', data });
}

export function repairWorkOrder(data: WorkOrderRepairDTO) {
  return request({ url: '/system/work-order/repair', method: 'post', data });
}

export function reviewWorkOrder(data: WorkOrderReviewDTO) {
  return request({ url: '/system/work-order/review', method: 'post', data });
}

export function updateRepairProductModel(data: Query) {
  return request({ url: '/system/work-order/repair-product-model', method: 'put', data });
}

export function updateWorkOrderSendExpress(data: Query) {
  return request({ url: '/system/work-order/send-express', method: 'put', data });
}

export function closeWorkOrder(data: WorkOrderCloseDTO) {
  return request({ url: '/system/work-order/close', method: 'put', data });
}
