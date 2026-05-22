import { request } from '../request';

/**
 * 售后工单域接口：列表、状态统计、创建/指派/流转/维修/关闭等全生命周期。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
  /** 转出方视角筛选（首页「已转出」卡片跳转传 OUT） */
  transferDirection?: 'OUT';
}

/** `GET /system/work-order/status-count`：仅提交前端筛选字段，不提交权限上下文字段。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderStatusCountQuery = Pick<
  WorkOrderQuery,
  'viewScope' | 'orderNo' | 'customerName' | 'customerMobile' | 'barcode' | 'hasTransfer' | 'transferDirection'
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
  /** 当前列表项可执行动作编码（CURRENT 为流程动作，HISTORY 仅可能透出补寄件单号例外动作） */
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

/** 总部网点工单汇总（`GET /system/work-order/hq-site-summary`） */
export interface WorkOrderHqSiteSummaryVO {
  siteCompanyId?: number;
  siteCompanyName?: string;
  totalCount?: number;
  waitAcceptCount?: number;
  inProgressCount?: number;
  completedCount?: number;
}

export interface WorkOrderHqSiteSummaryQuery {
  siteName?: string;
}

/** 总部网点工单只读列表展示状态（`GET /system/work-order/hq-site-orders`） */
export type WorkOrderHqSiteOrdersDisplayStatus = 'ALL' | 'WAIT_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';

/** 总部查看某承修方网点工单列表查询参数 */
export interface WorkOrderHqSiteOrdersQuery {
  siteCompanyId: number;
  displayStatus?: WorkOrderHqSiteOrdersDisplayStatus;
  orderNo?: string;
  customerName?: string;
  customerMobile?: string;
  barcode?: string;
  pageNum?: number;
  pageSize?: number;
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

/**
 * 作用：分页查询工单列表（含 viewScope、主状态、转单筛选等）。
 * @param params - 列表查询参数
 * @returns 分页结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listWorkOrder(params?: WorkOrderQuery) {
  return request<WorkOrderPageResult>({ url: '/system/work-order/list', method: 'get', params });
}

/**
 * 作用：按当前筛选条件统计各主状态数量（不含 mainStatus，供 Segmented 角标）。
 * @param params - 与列表共用的筛选字段子集
 * @returns 各状态数量列表 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function countWorkOrderStatus(params?: WorkOrderStatusCountQuery) {
  return request<WorkOrderStatusCountVO[]>({ url: '/system/work-order/status-count', method: 'get', params });
}

/**
 * 作用：查询总部网点工单汇总（按承修方公司聚合，供总部看板图表使用）。
 * 说明：数据范围由服务端按当前登录总部上下文注入；`HQ SELF` 可能返回空列表。
 * @param params - 网点名称等筛选
 * @returns 汇总列表 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listHqSiteSummary(params?: WorkOrderHqSiteSummaryQuery) {
  return request<WorkOrderHqSiteSummaryVO[]>({ url: '/system/work-order/hq-site-summary', method: 'get', params });
}

/**
 * 作用：总部查看指定承修方网点的只读工单分页列表。
 * 说明：数据范围由服务端按当前总部登录上下文注入。
 * @param params - 网点 id、展示状态、分页与搜索字段
 * @returns 分页结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listHqSiteOrders(params: WorkOrderHqSiteOrdersQuery) {
  return request<WorkOrderPageResult>({ url: '/system/work-order/hq-site-orders', method: 'get', params });
}

/**
 * 作用：查询工单详情（含 availableActions、流转与附件等）。
 * @param workOrderId - 工单 ID
 * @returns 详情 VO Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getWorkOrder(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}`, method: 'get' });
}

/**
 * 作用：总部代客建单页下拉选项（品牌、服务方式、故障项等）。
 * @returns 建单选项 VO Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listCreateHqOptions() {
  return request<WorkOrderCreateOptionsVO>({ url: '/system/work-order/create-hq-options', method: 'get' });
}

/**
 * 作用：总部代客建单按条码拉取商品与故障选项。
 * @param params - 条码、目标网点等
 * @returns 条码信息 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getProxyCreateBarcodeInfo(params?: Query) {
  return request({ url: '/system/work-order/create/proxy/barcode-info', method: 'get', params });
}

/**
 * 作用：上游一级网点建单按条码拉取商品与受理信息。
 * @param params - 条码、目标网点等
 * @returns 条码信息 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getUpstreamFirstCreateBarcodeInfo(params?: Query) {
  return request({ url: '/system/work-order/create/upstream-first/barcode-info', method: 'get', params });
}

/**
 * 作用：上游一级建单可选目标网点列表。
 * @returns 网点选项 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listUpstreamFirstCreateTargetOptions() {
  return request({ url: '/system/work-order/create/upstream-first/target-options', method: 'get' });
}

/**
 * 作用：上游报修佳士总部建单按条码拉取商品信息。
 * @param params - 条码等
 * @returns 条码信息 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function getUpstreamHqCreateBarcodeInfo(params?: Query) {
  return request({ url: '/system/work-order/create/upstream-hq/barcode-info', method: 'get', params });
}

/**
 * 作用：派单弹窗可选维修人员列表。
 * @param workOrderId - 工单 ID
 * @returns 人员选项 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listAssignUserOptions(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}/assign-user-options`, method: 'get' });
}

/**
 * 作用：转单弹窗可选目标网点列表。
 * @param workOrderId - 工单 ID
 * @returns 网点选项 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listTransferTargetOptions(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}/transfer-target-options`, method: 'get' });
}

/**
 * 作用：维修/复检表单故障项下拉选项。
 * @param workOrderId - 工单 ID
 * @returns 故障选项 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listRepairFaultOptions(workOrderId: IdLike) {
  return request({ url: `/system/work-order/${workOrderId}/repair-fault-options`, method: 'get' });
}

/**
 * 作用：佳士无机型时补录机型下拉（支持关键字搜索）。
 * @param workOrderId - 工单 ID
 * @param params - 搜索关键字等
 * @returns 机型选项 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function listRepairProductModelOptions(workOrderId: IdLike, params?: Query) {
  return request({ url: `/system/work-order/${workOrderId}/repair-product-model-options`, method: 'get', params });
}

/**
 * 作用：总部代客创建工单。
 * @param data - 建单 DTO
 * @returns 创建结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function createProxyWorkOrder(data: WorkOrderProxyCreateDTO) {
  return request({ url: '/system/work-order/create/proxy', method: 'post', data });
}

/**
 * 作用：上游一级网点创建工单。
 * @param data - 建单请求体
 * @returns 创建结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function createUpstreamFirstWorkOrder(data: Query) {
  return request({ url: '/system/work-order/create/upstream-first', method: 'post', data });
}

/**
 * 作用：上游报修佳士总部创建工单。
 * @param data - 建单请求体
 * @returns 创建结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function createUpstreamHqWorkOrder(data: Query) {
  return request({ url: '/system/work-order/create/upstream-hq', method: 'post', data });
}

/**
 * 作用：派单（指定维修人员）。
 * @param data - 派单 DTO
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function assignWorkOrder(data: WorkOrderAssignDTO) {
  return request({ url: '/system/work-order/assign', method: 'put', data });
}

/**
 * 作用：技术员接单（含无故障接单等扩展字段）。
 * @param data - 接单请求体
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function techAcceptWorkOrder(data: Query) {
  return request({ url: '/system/work-order/tech-accept', method: 'put', data });
}

/**
 * 作用：转单至其它网点。
 * @param data - 转单 DTO
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function transferWorkOrder(data: WorkOrderTransferDTO) {
  return request({ url: '/system/work-order/transfer', method: 'put', data });
}

/**
 * 作用：提交维修结果（报价、故障、配件、五类图等）。
 * @param data - 维修 DTO
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function repairWorkOrder(data: WorkOrderRepairDTO) {
  return request({ url: '/system/work-order/repair', method: 'post', data });
}

/**
 * 作用：总部复检通过（可修正维修说明与附件）。
 * @param data - 复检 DTO
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function reviewWorkOrder(data: WorkOrderReviewDTO) {
  return request({ url: '/system/work-order/review', method: 'post', data });
}

/**
 * 作用：补录维修机型（佳士条码无机型场景）。
 * @param data - 含 workOrderId、productModel 等
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function updateRepairProductModel(data: Query) {
  return request({ url: '/system/work-order/repair-product-model', method: 'put', data });
}

/**
 * 作用：补录或更新寄件快递单号与凭证。
 * @param data - 含 workOrderId、sendExpressNo、凭证 fileIds 等
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function updateWorkOrderSendExpress(data: Query) {
  return request({ url: '/system/work-order/send-express', method: 'put', data });
}

/**
 * 作用：关闭工单（返还方式、快递单、关闭原因等）。
 * @param data - 关闭 DTO
 * @returns 操作结果 Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function closeWorkOrder(data: WorkOrderCloseDTO) {
  return request({ url: '/system/work-order/close', method: 'put', data });
}
