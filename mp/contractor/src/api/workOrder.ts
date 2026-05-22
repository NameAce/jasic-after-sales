import { getApiMessage, http } from '@/utils/http'
import type {
  BranchItem,
  FaultPointRecord,
  OrderDetail,
  OrderDetailProcessFlowItem,
  OrderListItem,
  OrderRepairRegistrationEcho,
  SysFileItemVO,
  WorkOrderDetailVO,
  WorkOrderFaultVO,
  WorkOrderListPageResult,
  WorkOrderListVO,
  WorkOrderMainStatus,
  WorkOrderRepairVO,
} from '@/models/order'
import { WORK_ORDER_MAIN_STATUS } from '@/models/order'
import { normalizeAvailableActions } from '@/constants/orderActions'
import { mapWorkOrderRepairsToAllFaultPointRecords } from './mapRepairsToFaultPointRecords'
import { isOrderStatus } from '@/utils/orderStatus'
import { formatAmount } from '@/utils/format'

/**
 * 查询可派单人员：`/system/work-order/{workOrderId}/assign-user-options`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderUserOptionVO = {
  /**
 * 用户ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  id: number
  /**
 * 手机号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  phone?: string
  /**
 * 真实姓名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  realName?: string
}

/**
 * 可转单目标：后端 SysCompanySimpleVO
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type SysCompanySimpleVO = {
  companyCode?: string
  companyName?: string
  id: number
  typeCode?: string
  typeName?: string
}

/**
 * 维修登记故障与维修说明选项：`/system/work-order/{workOrderId}/repair-fault-options`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderRepairFaultOptionVO = {
  /**
 * 故障描述
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultDesc: string
  /**
 * 维修说明选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  repairOptions: string[]
}

/**
 * GET `/system/work-order/list` 查询参数（DTO）
 *
 * 三层口径：
 * - DTO：本类型（请求参数）
 * - VO：`@/models/order` 中 `WorkOrderListVO / WorkOrderDetailVO`（后端原样响应）
 * - Model：`@/models/order` 中 `OrderListItem / OrderDetail`（UI 展示模型）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type OrderListQuery = {
  barcode?: string
  companyId?: number
  /**
 * 服务端可注入；特殊场景也可显式传
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  currentUserId?: number
  customerMobile?: string
  customerName?: string
  dataScope?: string
  /**
 * 展示状态：ALL | WAIT_ACCEPT | IN_PROGRESS | COMPLETED | CLOSED（与后端 WorkOrderQuery 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  displayStatus?: 'ALL' | 'WAIT_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED'
  hasTransfer?: number
  /**
 * 分页排序方向：与后端 PageQuery 对齐
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  isAsc?: 'asc' | 'desc'
  /**
 * 主状态枚举：PENDING_ASSIGN | PENDING_TECH_ACCEPT | IN_PROGRESS | COMPLETED | CLOSED
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  mainStatus?: string
  orderByColumn?: string
  orderNo?: string
  pageNum?: number
  pageSize?: number
  relatedCompanyIds?: number[]
  subjectType?: string
  viewScope?: 'CURRENT' | 'HISTORY' | 'ALL'
}

export type OrderListPage = {
  pageNum: number
  pageSize: number
  total: number
  records: OrderListItem[]
}

export type HqSiteOrdersDisplayStatus = 'ALL' | 'WAIT_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED'

/**
 * 总部网点工单分页查询参数（与 jasic-ui 侧 GET `params` 扁平字段一致，由 `buildHqSiteOrdersQueryString` 序列化）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderHqSiteOrdersQuery = {
  barcode?: string
  customerMobile?: string
  customerName?: string
  displayStatus?: HqSiteOrdersDisplayStatus
  isAsc?: 'asc' | 'desc'
  orderByColumn?: string
  orderNo?: string
  pageNum?: number
  pageSize?: number
  siteCompanyId: number
}

type WorkOrderHqSiteSummaryVO = {
  completedCount?: number | string
  inProgressCount?: number | string
  siteCompanyId?: number | string
  siteCompanyName?: string
  totalCount?: number | string
  waitAcceptCount?: number | string
}

/**
 * 追加查询参数
 * @param out 形如 ["a=1","b=2"] 的片段数组
 * @param key 参数名
 * @param value 参数值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function appendQueryParam(out: string[], key: string, value: unknown) {
  if (value === undefined || value === null || value === '') return
  const k = encodeURIComponent(key)
  if (Array.isArray(value)) {
    value.forEach((item) => {
      if (item === undefined || item === null || item === '') return
      out.push(`${k}=${encodeURIComponent(String(item))}`)
    })
    return
  }
  out.push(`${k}=${encodeURIComponent(String(value))}`)
}

/**
 * 构建工单查询字符串（列表/统计通用）
 * @param params 查询参数
 * @returns 查询字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildWorkOrderQueryString(params: OrderListQuery): string {
  const parts: string[] = []
  appendQueryParam(parts, 'barcode', params.barcode)
  appendQueryParam(parts, 'companyId', params.companyId)
  appendQueryParam(parts, 'currentUserId', params.currentUserId)
  appendQueryParam(parts, 'customerMobile', params.customerMobile)
  appendQueryParam(parts, 'customerName', params.customerName)
  appendQueryParam(parts, 'dataScope', params.dataScope)
  appendQueryParam(parts, 'displayStatus', params.displayStatus)
  appendQueryParam(parts, 'hasTransfer', params.hasTransfer)
  appendQueryParam(parts, 'isAsc', params.isAsc)
  appendQueryParam(parts, 'mainStatus', params.mainStatus)
  appendQueryParam(parts, 'orderByColumn', params.orderByColumn)
  appendQueryParam(parts, 'orderNo', params.orderNo)
  appendQueryParam(parts, 'pageNum', params.pageNum ?? 1)
  appendQueryParam(parts, 'pageSize', params.pageSize ?? 10)
  appendQueryParam(parts, 'relatedCompanyIds', params.relatedCompanyIds)
  appendQueryParam(parts, 'subjectType', params.subjectType)
  appendQueryParam(parts, 'viewScope', params.viewScope)
  return parts.join('&')
}

/**
 * 构建总部网点汇总查询串（对齐 jasic-ui `listWorkOrder`：GET + 扁平 query，由 `appendQueryParam` 序列化）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildHqSiteSummaryQueryString(params: { siteName?: string }): string {
  const parts: string[] = []
  const name = String(params.siteName ?? '').trim()
  appendQueryParam(parts, 'siteName', name || undefined)
  return parts.join('&')
}

/**
 * 构建总部网点工单分页查询串（字段顺序与 jasic-ui 工单列表常用 GET 参数一致：主体筛选 → 分页 → 检索项 → 排序）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildHqSiteOrdersQueryString(params: WorkOrderHqSiteOrdersQuery): string {
  const parts: string[] = []
  appendQueryParam(parts, 'siteCompanyId', params.siteCompanyId)
  appendQueryParam(parts, 'displayStatus', params.displayStatus ?? 'ALL')
  appendQueryParam(parts, 'pageNum', params.pageNum ?? 1)
  appendQueryParam(parts, 'pageSize', params.pageSize ?? 10)
  appendQueryParam(parts, 'orderNo', params.orderNo)
  appendQueryParam(parts, 'customerName', params.customerName)
  appendQueryParam(parts, 'customerMobile', params.customerMobile)
  appendQueryParam(parts, 'barcode', params.barcode)
  appendQueryParam(parts, 'orderByColumn', params.orderByColumn)
  appendQueryParam(parts, 'isAsc', params.isAsc)
  return parts.join('&')
}

/**
 * 将搜索框关键词映射为列表接口的单一模糊条件（MyBatis 中 orderNo / customerName / barcode 为 AND，不可同时传）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
/**
 * 仅承载列表关键词三字段，供工单列表与总部网点工单列表复用
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderListKeywordQuery = Pick<OrderListQuery, 'orderNo' | 'customerName' | 'barcode'>

/**
 * 作用：状态：applyWorkOrderListSearchKeyword。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function applyWorkOrderListSearchKeyword(query: WorkOrderListKeywordQuery, keyword: string) {
  const q = String(keyword ?? '').trim()
  if (!q) return
  if (/[\u4e00-\u9fff]/.test(q)) {
    query.customerName = q
    return
  }
  if (/^\d{8,}$/.test(q)) {
    query.barcode = q
    return
  }
  query.orderNo = q
}

/**
 * 将后端 mainStatus 规范为前端 WorkOrderMainStatus（与后端枚举字面 1:1 对齐，禁止小写桶别名）
 * @param mainStatus 后端 mainStatus
 * @returns 前端 WorkOrderMainStatus
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function mapMainStatusToOrderStatus(mainStatus: string | undefined): WorkOrderMainStatus {
  const raw = (mainStatus ?? '').trim()
  if (!raw) return WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN
  const s = raw.toUpperCase().replace(/-/g, '_')
  /**
   * 主状态真源：`jasic-common/.../WorkOrderStatusConstants.java`
   *   - `MainStatus`   : PENDING_ASSIGN / PENDING_TECH_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED
   *   - `DisplayStatus`: 额外保留聚合态 `WAIT_ACCEPT`（= PENDING_ASSIGN + PENDING_TECH_ACCEPT）
   *     `/system/work-order/list` 的 `mainStatus` 字段理论上只会是主状态，
   *     但部分存量接口与详情页可能下发展示态，故对 `WAIT_ACCEPT` 一并做一次降维兜底为 `PENDING_TECH_ACCEPT`。
   * 前端自造别名（PROCESSING / DONE / REPAIRING 等）已于契约统一阶段回收。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const map: Record<string, WorkOrderMainStatus> = {
    PENDING_ASSIGN: WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN,
    PENDING_TECH_ACCEPT: WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT,
    IN_PROGRESS: WORK_ORDER_MAIN_STATUS.IN_PROGRESS,
    COMPLETED: WORK_ORDER_MAIN_STATUS.COMPLETED,
    CLOSED: WORK_ORDER_MAIN_STATUS.CLOSED,
    WAIT_ACCEPT: WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT,
  }
  if (map[s]) return map[s]
  if (isOrderStatus(s)) return s
  return WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN
}

/**
 * 详情接口 `displayStatus` → 前端 `WorkOrderMainStatus`
 *
 * 真源：`WorkOrderStatusConstants.DisplayStatus`，取值：
 *   `WAIT_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED`；
 *   其中 `WAIT_ACCEPT` 为 `PENDING_ASSIGN + PENDING_TECH_ACCEPT` 的聚合展示态，
 *   小程序侧统一降维为 `PENDING_TECH_ACCEPT` 参与主状态流转。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapDisplayStatusToOrderStatus(displayStatus: string | undefined): WorkOrderMainStatus | undefined {
  const raw = (displayStatus ?? '').trim()
  if (!raw) return undefined
  const s = raw.toUpperCase().replace(/-/g, '_')
  const map: Record<string, WorkOrderMainStatus> = {
    PENDING_ASSIGN: WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN,
    PENDING_TECH_ACCEPT: WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT,
    IN_PROGRESS: WORK_ORDER_MAIN_STATUS.IN_PROGRESS,
    COMPLETED: WORK_ORDER_MAIN_STATUS.COMPLETED,
    CLOSED: WORK_ORDER_MAIN_STATUS.CLOSED,
    WAIT_ACCEPT: WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT,
  }
  return map[s]
}

/**
 * 作用：转换/构造：mapWarrantyStatusToLabel。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapWarrantyStatusToLabel(status: string | undefined): string {
  const s = (status ?? '').trim().toUpperCase().replace(/-/g, '_')
  if (s === 'IN_WARRANTY') return '保内'
  if (s === 'OUT_OF_WARRANTY') return '保外'
  return String(status ?? '')
}

/**
 * 列表卡片质保角标样式（与 OrderListItem.warrantyClass 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapWarrantyStatusToListTagClass(
  status: string | undefined
): 'tag-in-warranty' | 'tag-out-warranty' | undefined {
  const s = (status ?? '').trim().toUpperCase().replace(/-/g, '_')
  if (s === 'IN_WARRANTY') return 'tag-in-warranty'
  if (s === 'OUT_OF_WARRANTY') return 'tag-out-warranty'
  return undefined
}

/**
 * 作用：接口封装：inferWarrantyTagClassFromLabel。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function inferWarrantyTagClassFromLabel(label: string): 'tag-in-warranty' | 'tag-out-warranty' | undefined {
  const t = label.trim()
  if (/保内/.test(t)) return 'tag-in-warranty'
  if (/保外/.test(t)) return 'tag-out-warranty'
  return undefined
}

/**
 * 列表 VO 上可能出现的「最后出库日期」字段（兼容多别名）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
/**
 * 归一化接口 brandType（与详情映射一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizeWorkOrderBrandTypeCode(brandType?: string): string {
  return (brandType ?? '').trim().toUpperCase().replace(/-/g, '_')
}

/**
 * 根据接口 brandType 判断是否佳士品牌工单（与详情 mapWorkOrderDetailToOrderDetail 一致）
 * - 有 brandType 且为 JASIC → 佳士
 * - 有 brandType 且非 JASIC → 非佳士
 * - 无 brandType → 默认按佳士
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapBrandTypeToIsJiashi(brandType?: string): boolean {
  const norm = normalizeWorkOrderBrandTypeCode(brandType)
  return norm ? norm === 'JASIC' : true
}

/**
 * 列表/详情 VO 上可能出现的「最后出库日期」字段（兼容多别名）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickWorkOrderOutDateFromRecord(rec: Record<string, unknown>): string {
  const keys = [
    'lastOutDate',
    'outDate',
    'lastStockOutDate',
    'stockOutDate',
    'productOutDate',
    'lastDeliveryDate',
    'last_out_date',
    'out_date',
    'last_stock_out_date',
  ]
  for (const k of keys) {
    const v = rec[k]
    const t = v != null ? String(v).trim() : ''
    if (t) return t
  }
  return ''
}

/**
 * 作用：接口封装：pickWorkOrderListOutDate。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickWorkOrderListOutDate(vo: WorkOrderListVO): string {
  return pickWorkOrderOutDateFromRecord(vo as Record<string, unknown>)
}

/**
 * 作用：转换/构造：mapOrderTypeNameFromDetail。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapOrderTypeNameFromDetail(vo: WorkOrderDetailVO): string {
  const label = String(vo.reportBizTypeLabel || '').trim()
  if (label) return label
  const code = (vo.createEntryType || '').trim().toUpperCase().replace(/-/g, '_')
  const map: Record<string, string> = {
    PROXY_SELF: '代客填写',
    UPSTREAM_FIRST: '二级报修',
    UPSTREAM_HQ: '一级报修',
  }
  if (code && map[code]) return map[code]
  return String(vo.createEntryType || '')
}

/**
 * 将后端 WorkOrderListVO 规范为前端 OrderListItem
 * @param vo 后端 WorkOrderListVO
 * @returns 工单列表项
 *
 * 注：列表项维修方式优先取 `serviceModeLabel`，并兼容 `serviceMode/repairMethod` 等旧字段别名。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapWorkOrderToListItem(vo: WorkOrderListVO): OrderListItem {
  const status = mapMainStatusToOrderStatus(vo.mainStatus)
  const faultDesc = String(vo.faultDesc ?? '').trim()
  const outDateRaw = pickWorkOrderListOutDate(vo)
  const outDate = outDateRaw || undefined
  const warrantyLabel = mapWarrantyStatusToLabel(vo.warrantyStatus).trim()
  const warrantyText = warrantyLabel || undefined
  let warrantyClass = mapWarrantyStatusToListTagClass(vo.warrantyStatus)
  if (!warrantyClass && warrantyLabel) warrantyClass = inferWarrantyTagClassFromLabel(warrantyLabel)
  const brandNorm = normalizeWorkOrderBrandTypeCode(vo.brandType)
  const brandTypeLabelRaw = String(vo.brandTypeLabel ?? '').trim()
  const repairPriceText = formatAmount(vo.quoteAmount) || undefined
  const listRaw = vo as Record<string, unknown>
  const repairMethodLabel = String(
    vo.serviceModeLabel ?? vo.serviceMode ?? listRaw.repairMethod ?? ''
  ).trim()
  const assignedUserName = String(vo.assignedUserName ?? '').trim()
  const availableActions = normalizeAvailableActions(vo.availableActions)
  const readonlyReason = String(vo.readonlyReason ?? '').trim() || undefined
  const createTime = String(vo.createTime ?? '').trim()
  const transferCountRaw = vo.transferCount
  const parsedTransferCount =
    transferCountRaw != null && String(transferCountRaw).trim() !== ''
      ? Number(transferCountRaw)
      : NaN
  const transferCount = Number.isFinite(parsedTransferCount) ? parsedTransferCount : undefined
  const displayStatusVo = String(vo.displayStatus ?? '').trim()
  const mainStatusLabelVo = String(vo.mainStatusLabel ?? '').trim()
  const hasTransferNum = Number(vo.hasTransfer)
  const customerMobile = String(vo.customerMobile ?? '').trim()
  return {
    id: String(vo.id),
    orderNo: vo.orderNo,
    mainStatus: vo.mainStatus,
    assignedUserId: vo.assignedUserId,
    assignedUserName: assignedUserName || undefined,
    status,
    brandType: brandNorm || undefined,
    brandTypeLabel: brandTypeLabelRaw || undefined,
    isJiashi: mapBrandTypeToIsJiashi(vo.brandType),
    customerMobile: customerMobile || undefined,
    phone: customerMobile,
    barcode: vo.barcode,
    model: vo.productModel,
    outDate,
    warrantyText,
    warrantyClass,
    faultDesc: faultDesc || undefined,
    transferred: Number.isFinite(hasTransferNum) && hasTransferNum !== 0,
    availableActions,
    readonlyReason,
    siteName: vo.currentAcceptCompanyName,
    sitePhone: String(vo.currentAcceptCompanyPhone ?? '').trim() || undefined,
    repairPriceText,
    repairMethodLabel: repairMethodLabel || undefined,
    createTime: createTime || undefined,
    transferCount,
    displayStatus: displayStatusVo || undefined,
    mainStatusLabel: mainStatusLabelVo || undefined
  }
}

/**
 * 查询工单列表
 * @param params 查询参数
 * @returns 工单列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function fetchOrderList(params: OrderListQuery = {}) {
  const page = await listWorkOrder(params)
  return page.records
}

/**
 * 查询工单列表（分页）
 * @param params 查询参数
 * @returns 工单分页数据
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listWorkOrder(params: OrderListQuery = {}): Promise<OrderListPage> {
  const qs = buildWorkOrderQueryString(params)
  const res = await http<WorkOrderListPageResult>({
    url: `/system/work-order/list?${qs}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const page = res.data
  const records = Array.isArray(page?.records) ? page.records : []
  return {
    pageNum: Number(page?.pageNum) || Number(params.pageNum) || 1,
    pageSize: Number(page?.pageSize) || Number(params.pageSize) || 10,
    total: Number(page?.total) || 0,
    records: records.map(mapWorkOrderToListItem),
  }
}

/**
 * 工单状态统计：`/system/work-order/status-count`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderStatusCountVO = {
  /**
 * 数量（部分序列化场景为字符串）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  countNum?: number | string
  /**
 * 状态名称，如「待派单」
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  displayStatus?: string
  /**
 * 状态编码，如 PENDING_ASSIGN
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  mainStatus?: string
}

/**
 * 统计卡片分项：待派单 / 待接单与列表 Tab 的 mainStatus 一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderStatusTabCounts = {
  pendingAssign: number
  pendingTechAccept: number
  processing: number
  completed: number
  closed: number
}

/**
 * 作用：转换/构造：parseWorkOrderStatusCountNum。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function parseWorkOrderStatusCountNum(v: unknown): number {
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'bigint') return Number(v)
  if (v == null || v === '') return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/**
 * 作用：转换/构造：parseCount。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function parseCount(v: unknown): number {
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'bigint') return Number(v)
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/**
 * mainStatus 缺省时用接口返回的展示文案推断枚举（仅保留标准主枚举）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function inferMainStatusCodeFromStatusCountDisplay(displayStatus: string | undefined): string {
  const d = String(displayStatus ?? '').trim()
  if (!d) return ''
  const cn: Record<string, string> = {
    待派单: 'PENDING_ASSIGN',
    待接单: 'PENDING_TECH_ACCEPT',
    维修中: 'IN_PROGRESS',
    已完结: 'COMPLETED',
    已结束: 'COMPLETED',
    已完成: 'COMPLETED',
    已关闭: 'CLOSED',
  }
  if (cn[d]) return cn[d]
  const u = d.toUpperCase().replace(/-/g, '_')
  if (
    u === 'PENDING_ASSIGN' ||
    u === 'PENDING_TECH_ACCEPT' ||
    u === 'IN_PROGRESS' ||
    u === 'COMPLETED' ||
    u === 'CLOSED'
  ) {
    return u
  }
  /**
   * 聚合展示态 `WAIT_ACCEPT`（= PENDING_ASSIGN + PENDING_TECH_ACCEPT）是后端正式 `DisplayStatus`
   * 取值（见 `WorkOrderStatusConstants.DisplayStatus`），小程序侧统一降维为 `PENDING_TECH_ACCEPT`
   * 参与主状态流转；**非历史别名，不可回收**。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  if (u === 'WAIT_ACCEPT') return 'PENDING_TECH_ACCEPT'
  return ''
}

/**
 * 汇总 status-count 接口返回行（待派单、待接单按 mainStatus 区分；其余走 mapMainStatusToOrderStatus）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function aggregateWorkOrderStatusTabCounts(rows: WorkOrderStatusCountVO[]): WorkOrderStatusTabCounts {
  const out: WorkOrderStatusTabCounts = {
    pendingAssign: 0,
    pendingTechAccept: 0,
    processing: 0,
    completed: 0,
    closed: 0
  }
  for (const r of rows) {
    const count = parseWorkOrderStatusCountNum(r.countNum)
    let ms = String(r.mainStatus ?? '')
      .trim()
      .toUpperCase()
      .replace(/-/g, '_')
    if (!ms) ms = inferMainStatusCodeFromStatusCountDisplay(r.displayStatus)
    if (ms === 'PENDING_ASSIGN') {
      out.pendingAssign += count
      continue
    }
    if (ms === 'PENDING_TECH_ACCEPT') {
      out.pendingTechAccept += count
      continue
    }
    const bucket = mapMainStatusToOrderStatus(ms || r.mainStatus)
    if (bucket === WORK_ORDER_MAIN_STATUS.PENDING_ASSIGN) {
      out.pendingAssign += count
      continue
    }
    if (bucket === WORK_ORDER_MAIN_STATUS.PENDING_TECH_ACCEPT) {
      out.pendingTechAccept += count
      continue
    }
    if (bucket === WORK_ORDER_MAIN_STATUS.IN_PROGRESS) out.processing += count
    else if (bucket === WORK_ORDER_MAIN_STATUS.COMPLETED) out.completed += count
    else if (bucket === WORK_ORDER_MAIN_STATUS.CLOSED) out.closed += count
  }
  return out
}

/**
 * 取 status-count 中某一 mainStatus 的展示数据：countNum 求和，displayStatus 取首条非空（与接口字段一致）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function pickWorkOrderStatusCountForMainCode(
  rows: WorkOrderStatusCountVO[],
  mainCode: 'PENDING_ASSIGN' | 'PENDING_TECH_ACCEPT'
): { count: number; displayStatus: string; matched: boolean } {
  let count = 0
  let displayStatus = ''
  let matched = false
  for (const r of rows) {
    let ms = String(r.mainStatus ?? '')
      .trim()
      .toUpperCase()
      .replace(/-/g, '_')
    if (!ms) ms = inferMainStatusCodeFromStatusCountDisplay(r.displayStatus)
    if (ms !== mainCode) continue
    matched = true
    count += parseWorkOrderStatusCountNum(r.countNum)
    if (!displayStatus) {
      const d = String(r.displayStatus ?? '').trim()
      if (d) displayStatus = d
    }
  }
  return { count, displayStatus, matched }
}

/**
 * 按状态统计工单数量
 * GET `/system/work-order/status-count`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function countWorkOrderStatus(params: OrderListQuery = {}) {
  const qs = buildWorkOrderQueryString({
    ...params,
    // 统计接口不需要分页字段，但后端允许传；这里不强行剔除，保持与文档字段一致
  })
  const res = await http<WorkOrderStatusCountVO[]>({
    url: `/system/work-order/status-count${qs ? `?${qs}` : ''}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = res.data
  return Array.isArray(list) ? list : []
}

/**
 * 查询可派单人员
 * GET `/system/work-order/{workOrderId}/assign-user-options`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listAssignUserOptions(workOrderId: number) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return []
  const res = await http<WorkOrderUserOptionVO[]>({
    url: `/system/work-order/${encodeURIComponent(String(id))}/assign-user-options`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = res.data
  return Array.isArray(list) ? list : []
}

/**
 * 查询可转单目标
 * GET `/system/work-order/{workOrderId}/transfer-target-options`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listTransferTargetOptions(workOrderId: number) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return []
  const res = await http<SysCompanySimpleVO[]>({
    url: `/system/work-order/${encodeURIComponent(String(id))}/transfer-target-options`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = res.data
  return Array.isArray(list) ? list : []
}

/**
 * 查询维修登记/复检前的机型补录候选（仅佳士品牌且 productModel 为空时使用）
 * GET `/system/work-order/{workOrderId}/repair-product-model-options`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listRepairProductModelOptions(
  workOrderId: number,
  params?: { keyword?: string },
) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return [] as string[]
  const kw = String(params?.keyword ?? '').trim()
  const qs = kw ? `?keyword=${encodeURIComponent(kw)}` : ''
  const res = await http<string[]>({
    url: `/system/work-order/${encodeURIComponent(String(id))}/repair-product-model-options${qs}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = res.data
  return Array.isArray(list) ? list : []
}

/**
 * 维修前机型补录
 * PUT `/system/work-order/repair-product-model`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function updateRepairProductModel(dto: {
  workOrderId: number
  productModel: string
}) {
  return http<void>({
    url: '/system/work-order/repair-product-model',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 查询维修登记可选故障与维修说明
 * GET `/system/work-order/{workOrderId}/repair-fault-options`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listRepairFaultOptions(workOrderId: number) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return []
  const res = await http<WorkOrderRepairFaultOptionVO[]>({
    url: `/system/work-order/${encodeURIComponent(String(id))}/repair-fault-options`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = res.data
  return Array.isArray(list) ? list : []
}

/**
 * 机器返回方式：兼容枚举/英文值，统一成「回寄/自提」等展示文案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizeReturnMethodLabel(raw: unknown): string {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  const u = s.toUpperCase()
  if (u === 'MAIL' || u === 'RETURN_MAIL' || u === 'SHIP' || u === 'SHIPPING' || u === 'EXPRESS') return '回寄'
  if (u === 'SELF' || u === 'PICKUP' || u === 'SELF_PICKUP' || u === 'STORE_PICKUP') return '自提'
  if (/回寄|邮寄|快递|物流/.test(s)) return '回寄'
  if (/自提|到店|自取/.test(s)) return '自提'
  return s
}

/**
 * 详情里附件项：兼容 preview_url / url 等字段及字符串数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveSysFileItemPreviewUrl(item: unknown): string {
  if (item == null) return ''
  if (typeof item === 'string') return item.trim()
  if (typeof item !== 'object') return ''
  const o = item as Record<string, unknown>
  for (const k of ['previewUrl', 'preview_url', 'url', 'fileUrl', 'file_url'] as const) {
    const v = o[k]
    if (v != null && String(v).trim()) return String(v).trim()
  }
  return ''
}

/**
 * 客户报修故障附件（`faultImageFiles` / `faultVideoFiles` / `faultVoiceFiles`）：
 * 按 `sortNum` 升序取可预览地址，与 `WorkOrderDetailVO` 文档一致。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function sortedFaultFilePreviewUrls(files: SysFileItemVO[] | undefined | null): string[] {
  if (!Array.isArray(files) || !files.length) return []
  return [...files]
    .sort((a, b) => (Number(a.sortNum) || 0) - (Number(b.sortNum) || 0))
    .map((f) => resolveSysFileItemPreviewUrl(f))
    .filter(Boolean)
}

/**
 * 作用：接口封装：pickFirstSortedFaultPreviewUrl。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickFirstSortedFaultPreviewUrl(files: SysFileItemVO[] | undefined | null): string {
  const urls = sortedFaultFilePreviewUrls(files)
  return urls[0] || ''
}

/**
 * `faultVoiceFiles` → 详情页语音条（`VoicePlaybackList`，缺 duration 时由组件探测）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapFaultVoiceFilesToVoiceList(
  files: SysFileItemVO[] | undefined | null,
): { url: string; duration?: number }[] {
  if (!Array.isArray(files) || !files.length) return []
  return [...files]
    .sort((a, b) => (Number(a.sortNum) || 0) - (Number(b.sortNum) || 0))
    .map((f) => {
      const url = resolveSysFileItemPreviewUrl(f)
      return url ? { url } : null
    })
    .filter((x): x is { url: string; duration?: number } => x != null)
}

/**
 * 作用：转换/构造：mapSenderVoucherFiles。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapSenderVoucherFiles(files: SysFileItemVO[] | undefined): { previewUrl: string }[] {
  if (!Array.isArray(files) || !files.length) return []
  return files
    .map((f) => ({ previewUrl: resolveSysFileItemPreviewUrl(f) }))
    .filter((x) => x.previewUrl)
}

/**
 * 作用：转换/构造：mapDetailProcessFlows。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapDetailProcessFlows(vo: WorkOrderDetailVO): OrderDetailProcessFlowItem[] {
  const flows = Array.isArray(vo.flows) ? vo.flows : []
  return flows
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
    .map((f) => {
      const title =
        String(f.actionName || '').trim() ||
        String(f.afterStatusName || '').trim() ||
        String(f.actionType || '').trim() ||
        '操作'
      const route =
        f.fromCompanyName && f.toCompanyName
          ? `${String(f.fromCompanyName)} → ${String(f.toCompanyName)}`
          : String(f.toCompanyName || f.fromCompanyName || '').trim()
      const parts = [f.operatorUserName, route, f.remark].filter(
        (x) => x != null && String(x).trim() !== '',
      ) as string[]
      return {
        time: String(f.createTime || ''),
        title,
        detail: parts.join(' · '),
      }
    })
}

/**
 * 作用：接口封装：sortWorkOrderRepairsByCreateTime。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function sortWorkOrderRepairsByCreateTime(repairs: WorkOrderRepairVO[]): WorkOrderRepairVO[] {
  return repairs
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
}

/**
 * 作用：判断：isRecheckRegisterStage。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isRecheckRegisterStage(r: WorkOrderRepairVO): boolean {
  const a = String(r.registerStage || '').toUpperCase()
  const b = String(r.registerStageLabel || '')
  return a.includes('RECHECK') || a.includes('REVIEW') || b.includes('复检')
}

/**
 * 复检表单回显：取「最后一次非复检」的维修登记；若均为复检则退回末条
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickRepairForRecheckFormEcho(sortedRepairs: WorkOrderRepairVO[]): WorkOrderRepairVO | undefined {
  for (let i = sortedRepairs.length - 1; i >= 0; i--) {
    const r = sortedRepairs[i]!
    if (!isRecheckRegisterStage(r)) return r
  }
  return sortedRepairs.length ? sortedRepairs[sortedRepairs.length - 1] : undefined
}

/**
 * 作用：接口封装：inferRepairItemsFromFaults。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function inferRepairItemsFromFaults(faults: WorkOrderFaultVO[]): string[] {
  const out: string[] = []
  const seen = new Set<string>()
  const sorted = [...faults].sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
  for (const f of sorted) {
    const rd = String(f.repairDesc || '').trim()
    if (!rd) continue
    for (const seg of rd.split(/[、,，;；]+/)) {
      const s = seg.trim()
      if (!s || seen.has(s)) continue
      seen.add(s)
      out.push(s)
    }
  }
  return out
}

/**
 * 作用：接口封装：inferRepairItemsFromRepairVo。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function inferRepairItemsFromRepairVo(r: WorkOrderRepairVO, faults: WorkOrderFaultVO[]): string[] {
  const ri = r.repairItems
  if (Array.isArray(ri) && ri.length) {
    return ri.map((x) => String(x || '').trim()).filter(Boolean)
  }
  const fromFaults = inferRepairItemsFromFaults(faults)
  if (fromFaults.length) return fromFaults
  const levelRd = String(r.repairDesc || '').trim()
  if (!levelRd) return []
  return levelRd.split(/[、,，;；]+/).map((s) => s.trim()).filter(Boolean)
}

/**
 * 作用：接口封装：inferOtherDescForRecheckEcho。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function inferOtherDescForRecheckEcho(r: WorkOrderRepairVO, faults: WorkOrderFaultVO[]): string {
  const top = String(r.otherDesc || '').trim()
  if (top) return top
  const OTHER = '其它维修说明'
  for (const f of faults) {
    if (String(f.repairDesc || '').trim() === OTHER) {
      const od = String(f.otherDesc || '').trim()
      if (od) return od
    }
  }
  return ''
}

/**
 * 从维修登记 faults 汇总「维修确认故障」多值（单条 faultDesc 可能含「、」）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function collectConfirmFaultItemsFromFaults(faults: WorkOrderFaultVO[] | undefined | null): string[] {
  const sorted = [...(faults || [])].sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
  const seen = new Set<string>()
  const out: string[] = []
  for (const f of sorted) {
    const raw = String(f.faultDesc || '').trim()
    if (!raw) continue
    for (const seg of raw.split(/[、,，;；]+/)) {
      const s = seg.trim()
      if (!s || seen.has(s)) continue
      seen.add(s)
      out.push(s)
    }
  }
  return out
}

/**
 * 作用：接口封装：collectConfirmFaultOtherRemarkFromFaults。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function collectConfirmFaultOtherRemarkFromFaults(faults: WorkOrderFaultVO[] | undefined | null): string {
  const sorted = [...(faults || [])].sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
  const chunks: string[] = []
  for (const f of sorted) {
    const fd = String(f.faultDesc || '')
    const od = String(f.otherDesc || '').trim()
    if (!od) continue
    if (
      fd.includes('其它') ||
      fd.includes('其他') ||
      fd.includes('其它故障') ||
      fd.includes('其他故障')
    ) {
      chunks.push(od)
    }
  }
  return chunks.join('；')
}

/**
 * 作用：接口封装：collectPartsFromFaultsForEcho。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function collectPartsFromFaultsForEcho(faults: WorkOrderFaultVO[]): { partName: string; partQty: number }[] {
  const sorted = [...faults].sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
  const rows: { partName: string; partQty: number }[] = []
  for (const f of sorted) {
    const fromList = faultPartsFromWorkOrderFault(f)
    if (fromList.length) {
      for (const p of fromList) {
        rows.push({ partName: p.name, partQty: p.count })
      }
      continue
    }
    const parsed = parseRepairPartDesc(String(f.partDesc || ''))
    for (const p of parsed) {
      rows.push({ partName: p.name, partQty: p.count })
    }
  }
  return rows
}

/**
 * 从详情 repairs 构建复检登记表单预填
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildRepairRegistrationEcho(
  repairs: WorkOrderRepairVO[] | undefined | null,
): OrderRepairRegistrationEcho | undefined {
  const sorted = sortWorkOrderRepairsByCreateTime(Array.isArray(repairs) ? repairs : [])
  const r = pickRepairForRecheckFormEcho(sorted)
  if (!r) return undefined
  const faults = Array.isArray(r.faults) ? r.faults : []
  return {
    confirmFaultItems: collectConfirmFaultItemsFromFaults(faults),
    confirmFaultOtherRemark: collectConfirmFaultOtherRemarkFromFaults(faults),
    repairItems: inferRepairItemsFromRepairVo(r, faults),
    otherDesc: inferOtherDescForRecheckEcho(r, faults),
    parts: collectPartsFromFaultsForEcho(faults),
    faultOldImageFiles: Array.isArray(r.faultOldImageFiles) ? r.faultOldImageFiles : [],
    faultNewImageFiles: Array.isArray(r.faultNewImageFiles) ? r.faultNewImageFiles : [],
    machineImageFiles: Array.isArray(r.machineImageFiles) ? r.machineImageFiles : [],
    machineBarcodeImageFiles: Array.isArray(r.machineBarcodeImageFiles) ? r.machineBarcodeImageFiles : [],
    otherImageFiles: Array.isArray(r.otherImageFiles) ? r.otherImageFiles : [],
  }
}

/**
 * 作用：转换/构造：parseRepairPartDesc。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function parseRepairPartDesc(partDesc: string): { name: string; count: number }[] {
  const raw = String(partDesc || '').trim()
  if (!raw) return []
  const out: { name: string; count: number }[] = []
  for (const seg of raw.split(/[；;]/)) {
    const s = seg.trim()
    if (!s) continue
    const m = s.match(/^(.+?)\s*[×xX＊*]\s*(\d+)\s*$/)
    if (m) {
      const n = Number(m[2])
      out.push({ name: m[1].trim(), count: Number.isFinite(n) && n > 0 ? n : 1 })
    } else {
      out.push({ name: s, count: 1 })
    }
  }
  return out
}

/**
 * 作用：接口封装：faultPartsFromWorkOrderFault。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function faultPartsFromWorkOrderFault(f: WorkOrderFaultVO) {
  const list = Array.isArray(f.partList) ? f.partList : []
  return list
    .slice()
    .sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
    .map((p) => {
      const name = String(p.partName || '').trim()
      const n = Number(p.partQty)
      const count = Number.isFinite(n) && n > 0 ? n : 1
      return name ? { name, count } : null
    })
    .filter((x): x is { name: string; count: number } => x != null)
}

/**
 * 作用：转换/构造：mapWorkOrderDetailToOrderDetail。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapWorkOrderDetailToOrderDetail(vo: WorkOrderDetailVO): OrderDetail {
  // 有 mainStatus 时以主状态桶为准，避免 displayStatus 的 WAIT_ACCEPT 等聚合态
  // 将「待派单 PENDING_ASSIGN」误映射为 PENDING_TECH_ACCEPT，导致详情与列表/派单区不一致
  const fromDisplay = mapDisplayStatusToOrderStatus(vo.displayStatus)
  const fromMain = mapMainStatusToOrderStatus(vo.mainStatus)
  const status: WorkOrderMainStatus = (() => {
    const rawMain = (vo.mainStatus ?? '').trim()
    if (rawMain) return fromMain
    if (fromDisplay != null) return fromDisplay
    return fromMain
  })()
  const transferred = vo.hasTransfer === 1 || (vo.transferCount ?? 0) > 0
  const isJiashi = mapBrandTypeToIsJiashi(vo.brandType)

  // 尝试从 flows 中推断“转出网点”
  const currentCompany = (vo.currentAcceptCompanyName || '').trim()
  const flows = Array.isArray(vo.flows) ? vo.flows : []
  const transferLike = flows
    .filter((f) => {
      const action = (f.actionType || f.actionName || '').toString()
      const maybeTransfer = /transfer|turn|shift/i.test(action) || action.includes('转')
      const from = (f.fromCompanyName || '').trim()
      const to = (f.toCompanyName || '').trim()
      if (!maybeTransfer) return false
      if (!from || !to) return false
      return !currentCompany || to === currentCompany
    })
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
  const lastTransfer = transferLike.length ? transferLike[transferLike.length - 1] : undefined
  const transferFromSite = (lastTransfer?.fromCompanyName || '').trim()

  const quote =
    Array.isArray(vo.quotes) && vo.quotes.length
      ? vo.quotes.find((q) => q.isCurrentValid === 1) ?? vo.quotes[vo.quotes.length - 1]
      : undefined

  const returnMethodLabel = normalizeReturnMethodLabel(vo.returnMethod)
  const senderNameForEcho = String(quote?.senderName || vo.senderName || '').trim()
  const senderMobileForEcho = String(quote?.senderMobile || vo.senderMobile || '').trim()
  const senderAddressForEcho = String(quote?.senderAddress || vo.senderAddress || '').trim()

  // 故障点：取 repairs 最后一条的 faults 作为“当前维修记录”，其余作为 history
  const repairs = Array.isArray(vo.repairs) ? vo.repairs : []
  const sortedRepairs = sortWorkOrderRepairsByCreateTime(repairs)
  const latestRepair = sortedRepairs.length ? sortedRepairs[sortedRepairs.length - 1] : undefined
  const latestRepairFaultsSorted =
    latestRepair?.faults && latestRepair.faults.length
      ? [...latestRepair.faults].sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
      : []
  const latestFault =
    latestRepairFaultsSorted.length > 0
      ? latestRepairFaultsSorted[latestRepairFaultsSorted.length - 1]
      : undefined

  const allRepairsFaultRecords = mapWorkOrderRepairsToAllFaultPointRecords(sortedRepairs)

  const history =
    sortedRepairs.length && latestRepair
      ? mapWorkOrderRepairsToAllFaultPointRecords(sortedRepairs.slice(0, -1))
      : []

  const evalVO = vo.evaluation
  const evaluate =
    evalVO && (evalVO.timelinessScore || evalVO.qualityScore || evalVO.satisfactionScore || evalVO.content)
      ? {
          timeliness: evalVO.timelinessScore,
          quality: evalVO.qualityScore,
          satisfaction: evalVO.satisfactionScore,
          comment: evalVO.content || '',
        }
      : undefined

  const senderVoucherFilesMapped = mapSenderVoucherFiles(vo.senderVoucherFiles)

  return {
    id: String(vo.id ?? ''),
    availableActions: normalizeAvailableActions(vo.availableActions),
    status,
    mainStatus: vo.mainStatus,
    assignedUserId: (() => {
      const a = vo.assignedUserId
      if (a === undefined || a === null) return undefined
      const n = Number(a)
      return Number.isFinite(n) && n > 0 ? n : undefined
    })(),
    transferred,
    brand: {
      isJiashi,
    },
    customer: {
      phone: String(vo.customerMobile || ''),
    },
    base: {
      orderNo: String(vo.orderNo || ''),
      orderTypeName: mapOrderTypeNameFromDetail(vo),
      brandTypeLabel: String(vo.brandTypeLabel || '').trim(),
      submitTime: String(vo.createTime || ''),
      transferSite: String(vo.currentAcceptCompanyName || ''),
      transferFromSite,
    },
    product: {
      barcode: String(vo.barcode || ''),
      brandName: String(vo.brandName || '').trim(),
      model: String(vo.productModel || ''),
      serialNo: String(vo.machineNo || ''),
      lastOutDate: pickWorkOrderOutDateFromRecord(vo as Record<string, unknown>),
      warrantyClass: mapWarrantyStatusToLabel(vo.warrantyStatus),
      repairStatus: String(vo.mainStatusLabel || vo.displayStatus || ''),
    },
    service: {
      sitePhone: String(vo.currentAcceptCompanyPhone ?? '').trim(),
      serviceModeLabel: (() => {
        const L = String(vo.serviceModeLabel ?? '').trim()
        if (L) return L
        const c = String(vo.serviceMode ?? '').trim().toUpperCase()
        if (c === 'MAIL') return '寄修'
        if (c === 'STORE') return '到店维修'
        return ''
      })(),
      customerMobile: String(vo.customerMobile ?? '').trim(),
      applySourceLabel: (() => {
        const fromApi = String(vo.applicationSourceName ?? '').trim()
        if (fromApi) return fromApi
        const t = String(mapOrderTypeNameFromDetail(vo) ?? '').trim()
        if (t) return t
        return String(vo.createCompanyName || '').trim()
      })(),
      acceptingParty: String(vo.currentAcceptCompanyName || '').trim(),
      source: String(vo.createCompanyName || ''),
      /* 与 C 端 `mapCustomerWorkOrderDetailToOrderDetail` 的寄件信息展示一致 */
      senderInfo: (() => {
        const name = String(quote?.senderName || vo.senderName || '').trim()
        const mobile = String(quote?.senderMobile || vo.senderMobile || '').trim()
        const address = String(quote?.senderAddress || vo.senderAddress || '').trim()
        const expressNo = String(quote?.sendExpressNo || vo.sendExpressNo || '').trim()
        const parts = [
          [name, mobile].filter(Boolean).join(' '),
          address,
          expressNo ? `快递单号：${expressNo}` : '',
        ]
          .map((x) => String(x ?? '').trim())
          .filter(Boolean)
        return parts.join('\n')
      })(),
      senderName: String(quote?.senderName || vo.senderName || ''),
      senderMobile: String(quote?.senderMobile || vo.senderMobile || ''),
      senderAddress: String(quote?.senderAddress || vo.senderAddress || ''),
      sendExpressNo: String(quote?.sendExpressNo || vo.sendExpressNo || ''),
      /**
 * 与 C 端 `senderVoucherImg` 一致：寄件凭证首图
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      senderVoucherImg: String(senderVoucherFilesMapped[0]?.previewUrl ?? '').trim(),
      senderVoucherFiles: senderVoucherFilesMapped,
      returnMethod: returnMethodLabel || String(vo.returnMethod ?? '').trim(),
      returnExpressNo: String(vo.returnExpressNo ?? '').trim(),
      mailReturnForm: (() => {
        const receiverName = String(vo.returnReceiverName ?? '').trim()
        const receiverPhone = String(vo.returnReceiverMobile ?? '').trim()
        const receiverAddress = String(vo.returnReceiverAddress ?? '').trim()
        const receiptImagePaths = mapSenderVoucherFiles(vo.returnVoucherFiles).map((x) => x.previewUrl)
        const isMailReturn =
          returnMethodLabel === '回寄' ||
          /回寄|邮寄|快递|物流|^MAIL$/i.test(String(vo.returnMethod ?? '').trim())
        const fallbackName = isMailReturn ? senderNameForEcho : ''
        const fallbackPhone = isMailReturn ? senderMobileForEcho : ''
        const fallbackAddress = isMailReturn ? senderAddressForEcho : ''
        const mergedName = receiverName || fallbackName
        const mergedPhone = receiverPhone || fallbackPhone
        const mergedAddress = receiverAddress || fallbackAddress
        if (!mergedName && !mergedPhone && !mergedAddress && receiptImagePaths.length === 0) {
          return undefined
        }
        return {
          receiverName: mergedName,
          receiverPhone: mergedPhone,
          receiverAddress: mergedAddress,
          receiptImagePaths,
        }
      })(),
    },
    acceptor: (() => {
      const outletPhone = String(vo.currentAcceptCompanyPhone ?? '').trim()
      return {
        currentAcceptCompanyName: String(vo.currentAcceptCompanyName || '').trim(),
        sitePhone: outletPhone,
        currentAcceptCompanyPhone: outletPhone,
      }
    })(),
    fault: (() => {
      const faultVoiceList = mapFaultVoiceFilesToVoiceList(vo.faultVoiceFiles)
      return {
        /**
 * 客户报修描述 `faultDesc`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
        desc: String(vo.faultDesc ?? '').trim(),
        /**
 * 客户故障备注 `faultRemark`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
        faultExplain: String(vo.faultRemark ?? '').trim(),
        voiceDuration: '',
        voiceList: faultVoiceList.length ? faultVoiceList : undefined,
        /**
 * `faultImageFiles` → 预览地址，按 `sortNum`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
        images: sortedFaultFilePreviewUrls(vo.faultImageFiles),
        /**
 * `faultVideoFiles` → 预览地址，按 `sortNum`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
        videos: sortedFaultFilePreviewUrls(vo.faultVideoFiles),
        videoThumb: pickFirstSortedFaultPreviewUrl(vo.faultVideoFiles),
      }
    })(),
    repair: {
      faultJudge: String(quote?.faultJudge || ''),
      quoteAmount:
        quote?.quoteAmount === undefined || quote?.quoteAmount === null ? '0.00' : String(quote.quoteAmount),
      quoteDesc: String(quote?.quoteDesc || ''),
    },
    faultPoint: {
      current: {
        date: String(latestFault?.createTime || latestRepair?.createTime || ''),
        desc: String(latestFault?.faultDesc || latestRepair?.repairSummary || ''),
      },
      currentFaults: latestRepairFaultsSorted,
      history,
      allRepairsFaultRecords,
    },
    repairRegistrationEcho: buildRepairRegistrationEcho(repairs),
    processFlows: mapDetailProcessFlows(vo),
    contact: {
      phone: String(vo.customerMobile || ''),
    },
    evaluate,
  }
}

/**
 * 查询工单详情
 * @param id 工单ID
 * @returns 工单详情
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function getWorkOrder(id: string) {
  const wid = String(id || '').trim()
  if (!wid) {
    const msg = '工单ID无效'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const res = await http<WorkOrderDetailVO>({
    url: `/system/work-order/${encodeURIComponent(wid)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const vo = res.data
  if (vo == null || typeof vo !== 'object') {
    const msg = '工单详情数据为空'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  return mapWorkOrderDetailToOrderDetail(vo)
}

/**
 * GET 工单详情，仅将 `data.repairs` 映射为故障点历史列表（历史维修记录页使用）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function getWorkOrderRepairFaultRecords(id: string): Promise<FaultPointRecord[]> {
  const wid = String(id || '').trim()
  if (!wid) {
    const msg = '工单ID无效'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const res = await http<WorkOrderDetailVO>({
    url: `/system/work-order/${encodeURIComponent(wid)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const vo = res.data
  if (vo == null || typeof vo !== 'object') {
    const msg = '工单详情数据为空'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  return mapWorkOrderRepairsToAllFaultPointRecords(vo.repairs)
}

/**
 * 总部网点汇总（与 jasic-ui `src/api/workOrder.js` 中工单请求一致：`GET` + query 串、`http` 拼 `/api`）。
 * 路径：`/system/work-order/hq-site-summary`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listHqSiteSummary(params: { siteName?: string } = {}): Promise<BranchItem[]> {
  const qs = buildHqSiteSummaryQueryString(params)
  const url = qs ? `/system/work-order/hq-site-summary?${qs}` : '/system/work-order/hq-site-summary'
  const res = await http<WorkOrderHqSiteSummaryVO[]>({
    url,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = Array.isArray(res.data) ? res.data : []
  return list.map((item) => ({
    id: parseCount(item.siteCompanyId),
    name: String(item.siteCompanyName ?? '').trim(),
    total: parseCount(item.totalCount),
    pending: parseCount(item.waitAcceptCount),
    processing: parseCount(item.inProgressCount),
    completed: parseCount(item.completedCount),
  }))
}

/**
 * 总部按受理网点分页查工单（与 `listWorkOrder` 相同封装模式）。
 * 路径：`/system/work-order/hq-site-orders`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listHqSiteOrders(params: WorkOrderHqSiteOrdersQuery): Promise<OrderListPage> {
  const qs = buildHqSiteOrdersQueryString(params)
  const res = await http<WorkOrderListPageResult>({
    url: `/system/work-order/hq-site-orders?${qs}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const page = res.data
  const records = Array.isArray(page?.records) ? page.records : []
  return {
    pageNum: Number(page?.pageNum) || Number(params.pageNum) || 1,
    pageSize: Number(page?.pageSize) || Number(params.pageSize) || 10,
    total: Number(page?.total) || 0,
    records: records.map(mapWorkOrderToListItem),
  }
}

export type WorkOrderAssignDTO = {
  /**
 * 维修员ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  assignedUserId: number
  /**
 * 工单ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
}

export type WorkOrderTransferDTO = {
  /**
 * 转单备注
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  remark?: string
  /**
 * 目标公司ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  targetCompanyId: number
  /**
 * 工单ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
}

export type WorkOrderCloseDTO = {
  /**
 * 关闭原因
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  closeReason: string
  /**
 * 回寄快递单号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnExpressNo?: string
  /**
 * 机器返回方式（与后端一致：自提 | 回寄）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnMethod: string
  /**
 * 回寄凭证文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnVoucherFileIds?: number[]
  /**
 * 工单ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
}

/**
 * 机器返回方式弹窗 `confirm` 载荷（关闭工单等场景复用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type ReturnMethodConfirmPayload =
  | { type: 'self' }
  | {
      type: 'mail'
      mail: {
        receiverName: string
        receiverPhone: string
        receiverAddress: string
        receiptImagePaths: string[]
        returnVoucherFileIds: number[]
      }
    }

/**
 * 有故障：返回方式确认后直接关单时使用的 closeReason（接口必填，与详情/列表一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const WORK_ORDER_FAULT_CLOSE_REASON = '有故障维修完成'

export type WorkOrderTechAcceptDTO = {
  /**
 * 工单ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
  /**
 * 故障判定（与详情页一致：有故障 | 无故障）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultJudge: string
  /**
 * 维修报价金额
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  quoteAmount?: number
  /**
 * 报价说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  quoteDesc?: string
  /**
 * 关闭原因（无故障场景）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  closeReason?: string
  /**
 * 机器返回方式（与后端一致：自提 | 回寄）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnMethod?: string
  /**
 * 回寄快递单号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnExpressNo?: string
  /**
 * 回寄凭证文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnVoucherFileIds?: number[]
}

/**
 * 维修登记 / 复检 body 内配件明细（WorkOrderFaultPartItemDTO）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderFaultPartItemDTO = {
  partName?: string
  partQty?: number
}

/**
 * POST `/system/work-order/repair` 请求体（WorkOrderRepairDTO）
 * @see workOrderId 必填；其余字段按后端文档均为可选
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderRepairDTO = {
  /**
   * 维修确认故障多选（对齐后端 WorkOrderRepairDTO.faultItems）。
   *
   * 契约说明：后端未加 `@NotEmpty`，参考 jasic-ui 仅在"存在故障字典配置 + 动作为 REPAIR_FINISH"时
   * 才要求至少选一项；无字典配置场景由 `repairDesc` 兜底，因此此处保持可选。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultItems?: string[]
  /**
 * 其它故障说明（含「其它」时必填，对齐后端 WorkOrderRepairDTO.faultRemark）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultRemark?: string
  /**
 * 故障处新图片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultNewImageFileIds?: number[]
  /**
 * 故障处旧图片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultOldImageFileIds?: number[]
  /**
 * 机器条码照片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  machineBarcodeImageFileIds?: number[]
  /**
 * 机器正面照片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  machineImageFileIds?: number[]
  /**
 * 其他维修说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  otherDesc?: string
  /**
 * 其他图片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  otherImageFileIds?: number[]
  /**
 * 工单故障点配件明细
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  partList?: WorkOrderFaultPartItemDTO[]
  /**
 * 调整后的报价金额
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  quoteAmount?: number
  /**
 * 调整后的报价说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  quoteDesc?: string
  /**
 * 手工填写的维修说明（无「故障与维修配置」时兜底）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  repairDesc?: string
  /**
 * 维修说明选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  repairItems?: string[]
  /**
 * 工单 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
}

/**
 * POST `/system/work-order/review` 保存复检记录（WorkOrderReviewDTO）
 * workOrderId 必填；其余字段可选，与接口文档一致
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderReviewDTO = {
  /**
 * 故障处新图片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultNewImageFileIds?: number[]
  /**
 * 故障处旧图片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultOldImageFileIds?: number[]
  /**
 * 机器条码照片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  machineBarcodeImageFileIds?: number[]
  /**
 * 机器正面照片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  machineImageFileIds?: number[]
  /**
 * 其他维修说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  otherDesc?: string
  /**
 * 其他图片文件 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  otherImageFileIds?: number[]
  /**
 * 工单故障点配件明细
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  partList?: WorkOrderFaultPartItemDTO[]
  /**
 * 手工填写的维修说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  repairDesc?: string
  /**
 * 维修说明选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  repairItems?: string[]
  /**
 * 工单 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
}

/**
 * 代客户填写创建工单请求体（WorkOrderProxyCreateDTO）
 * POST `/system/work-order/create/proxy`，Content-Type: application/json
 * 接口约定：customerMobile、serviceMode 必填；其余字段按文档均为可选。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderProxyCreateDTO = {
  /**
 * 机器条码
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  barcode?: string
  /**
 * 客户手机号（必填）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  customerMobile: string
  /**
 * 客户姓名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  customerName?: string
  /**
 * 故障图片文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultImageFileIds?: number[]
  /**
 * 故障描述选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultItems?: string[]
  /**
 * 故障备注
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultRemark?: string
  /**
 * 故障视频文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultVideoFileIds?: number[]
  /**
 * 故障语音文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultVoiceFileIds?: number[]
  /**
 * 寄件快递单号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  sendExpressNo?: string
  /**
 * 寄件地址
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderAddress?: string
  /**
 * 寄件人手机号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderMobile?: string
  /**
 * 寄件人姓名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderName?: string
  /**
 * 寄件凭证文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderVoucherFileIds?: number[]
  /**
 * 服务方式编码 MAIL | STORE（必填）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceMode: 'MAIL' | 'STORE'
}

/**
 * 二级报修一级创建工单 DTO。
 *
 * 契约说明：参考后端 `WorkOrderUpstreamCreateDTO.java` 与 `jasic-ui` 建单校验规则，
 * 上游建单（upstream-first / upstream-hq）场景下 `barcode / customerMobile / customerName`
 * 均为可选，由页面按业务条件做必要性校验；仅 `serviceMode` 为必填。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderUpstreamCreateDTO = {
  /**
 * 机器条码（可选，无码场景允许缺省）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  barcode?: string
  /**
 * 客户手机号（可选）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  customerMobile?: string
  /**
 * 客户姓名（可选）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  customerName?: string
  /**
 * 故障图片文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultImageFileIds?: number[]
  /**
 * 故障描述选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultItems?: string[]
  /**
 * 故障备注
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultRemark?: string
  /**
 * 故障视频文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultVideoFileIds?: number[]
  /**
 * 故障语音文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultVoiceFileIds?: number[]
  /**
 * 寄件快递单号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  sendExpressNo?: string
  /**
 * 寄件地址
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderAddress?: string
  /**
 * 寄件人手机号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderMobile?: string
  /**
 * 寄件人姓名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderName?: string
  /**
 * 寄件凭证文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderVoucherFileIds?: number[]
  /**
 * 服务方式编码
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceMode: 'MAIL' | 'STORE'
  /**
 * 目标受理公司ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  targetCompanyId?: number
}

/**
 * 公司简要信息 VO（条码查询返回）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type BarcodeInfoCompanyOptionVO = {
  companyCode?: string
  companyName?: string
  id?: number
  typeCode?: string
  typeName?: string
}

/**
 * 条码查询接口成功返回：业务数据 + 顶层 msg（单独带出便于上层展示 toast）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type BarcodeInfoFetchResult = {
  data: WorkOrderCreateBarcodeInfoVO
  msg: string
}

/**
 * 代客户填写条码信息 VO
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderCreateBarcodeInfoVO = {
  barcode?: string
  brandCode?: string
  defaultTargetCompanyId?: number
  faultOptions?: string[]
  hqCompanyId?: number
  hqCompanyName?: string
  machineNo?: string
  otherFaultLabel?: string
  productCode?: string
  productModel?: string
  productName?: string
  targetCompanyOptions?: BarcodeInfoCompanyOptionVO[]
  warrantyStatus?: string
}

/**
 * 代客户填写创建工单
 * POST `/system/work-order/create/proxy`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function createProxyWorkOrder(dto: WorkOrderProxyCreateDTO) {
  return http<number>({
    url: '/system/work-order/create/proxy',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 二级报修一级创建工单
 * POST `/system/work-order/create/upstream-first`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function createUpstreamFirstWorkOrder(dto: WorkOrderUpstreamCreateDTO) {
  return http<number>({
    url: '/system/work-order/create/upstream-first',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 一级报修佳士创建工单
 * POST `/system/work-order/create/upstream-hq`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function createUpstreamHqWorkOrder(dto: WorkOrderUpstreamCreateDTO) {
  return http<number>({
    url: '/system/work-order/create/upstream-hq',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 查询代客户填写条码信息
 * GET `/system/work-order/create/proxy/barcode-info`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function getProxyCreateBarcodeInfo(barcode: string): Promise<BarcodeInfoFetchResult> {
  const code = String(barcode || '').trim()
  if (!code) throw new Error('barcode is required')
  const res = await http<WorkOrderCreateBarcodeInfoVO>({
    url: `/system/work-order/create/proxy/barcode-info?barcode=${encodeURIComponent(code)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })

  return {
    data: res.data,
    msg: getApiMessage(res, '查询成功'),
  }
}

/**
 * 查询二级报修一级条码信息
 * GET `/system/work-order/create/upstream-first/barcode-info`
 *
 * 二级经销商专用：后端 `validateCurrentCompanyType("SITE_SECOND")`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function getUpstreamFirstCreateBarcodeInfo(
  barcode: string,
): Promise<BarcodeInfoFetchResult> {
  const code = String(barcode || '').trim()
  if (!code) throw new Error('barcode is required')
  const res = await http<WorkOrderCreateBarcodeInfoVO>({
    url: `/system/work-order/create/upstream-first/barcode-info?barcode=${encodeURIComponent(code)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })

  return {
    data: res.data,
    msg: getApiMessage(res, '查询成功'),
  }
}

/**
 * 查询二级无码报修一级可选目标公司列表
 * GET `/system/work-order/create/upstream-first/target-options`
 *
 * 二级经销商无条码或条码未命中档案时，使用该接口拉取可上报的一级网点列表。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function listUpstreamFirstCreateTargetOptions() {
  const res = await http<SysCompanySimpleVO[]>({
    url: '/system/work-order/create/upstream-first/target-options',
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = res.data
  return Array.isArray(list) ? list : []
}

/**
 * 查询一级报修佳士条码信息
 * GET `/system/work-order/create/upstream-hq/barcode-info`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function getUpstreamHqCreateBarcodeInfo(
  barcode: string,
  targetCompanyId?: number,
): Promise<BarcodeInfoFetchResult> {
  const code = String(barcode || '').trim()
  if (!code) throw new Error('barcode is required')
  const targetId = Number(targetCompanyId)
  const hasTargetCompanyId = Number.isFinite(targetId) && targetId > 0
  const qs = hasTargetCompanyId
    ? `barcode=${encodeURIComponent(code)}&targetCompanyId=${encodeURIComponent(String(targetId))}`
    : `barcode=${encodeURIComponent(code)}`
  const res = await http<WorkOrderCreateBarcodeInfoVO>({
    url: `/system/work-order/create/upstream-hq/barcode-info?${qs}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })

  return {
    data: res.data,
    msg: getApiMessage(res, '查询成功'),
  }
}

/**
 * 派单
 * PUT `/system/work-order/assign`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function assignWorkOrder(dto: WorkOrderAssignDTO) {
  return http<void>({
    url: '/system/work-order/assign',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 关闭工单
 * PUT `/system/work-order/close`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function closeWorkOrder(dto: WorkOrderCloseDTO) {
  return http<void>({
    url: '/system/work-order/close',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 转单
 * PUT `/system/work-order/transfer`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function transferWorkOrder(dto: WorkOrderTransferDTO) {
  return http<void>({
    url: '/system/work-order/transfer',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 维修员接单
 * PUT `/system/work-order/tech-accept`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function techAcceptWorkOrder(dto: WorkOrderTechAcceptDTO) {
  return http<void>({
    url: '/system/work-order/tech-accept',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 保存维修登记
 * POST `/system/work-order/repair`，Content-Type: application/json
 * 请求体字段与 WorkOrderRepairDTO 一致（workOrderId 必填）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function repairWorkOrder(dto: WorkOrderRepairDTO) {
  return http<void>({
    url: '/system/work-order/repair',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 保存复检记录
 * POST `/system/work-order/review`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function reviewWorkOrder(dto: WorkOrderReviewDTO) {
  return http<void>({
    url: '/system/work-order/review',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 上传寄件快递单号参数（对齐后端 WorkOrderSendExpressDTO，sendExpressNo 必填）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderSendExpressDTO = {
  /**
 * 工单ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
  /**
 * 寄件快递单号（后端 @NotBlank）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  sendExpressNo: string
  /**
 * 寄件凭证文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderVoucherFileIds?: number[]
}

/**
 * 上传寄件快递单号（承包商端）
 * PUT `/system/work-order/send-express`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function updateWorkOrderSendExpress(dto: WorkOrderSendExpressDTO) {
  return http<void>({
    url: '/system/work-order/send-express',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })
}

