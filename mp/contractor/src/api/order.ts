import { getApiMessage, http, unwrap } from '@/utils/http'
import type {
  BranchItem,
  FaultPointRecord,
  OrderDetail,
  OrderDetailProcessFlowItem,
  OrderListItem,
  OrderRepairRegistrationEcho,
  OrderStatus,
  SysFileItemVO,
  WorkOrderDetailVO,
  WorkOrderFaultVO,
  WorkOrderListPageResult,
  WorkOrderListVO,
  WorkOrderRepairVO,
} from '@/models/order'
import { isOrderStatus } from '@/utils/orderStatus'

/**
 * 列表等接口：成功码 00000（文档）或与现有 PUT 接口一致的 200
 *
*/
function isOkCode(code: unknown) {
  const n = Number(code)
  return Number.isFinite(n) ? n === 200 : String(code) === '200'
}

/** 列表等接口：成功码 00000（文档）或与现有 PUT 接口一致的 200 */
function isBizSuccess(code: unknown) {
  if (String(code) === '00000') return true
  return isOkCode(code)
}

/** 查询可派单人员：`/api/system/work-order/{workOrderId}/assign-user-options` */
export type WorkOrderUserOptionVO = {
  /** 用户ID */
  id: number
  /** 手机号 */
  phone?: string
  /** 真实姓名 */
  realName?: string
}

/** 可转单目标：后端 SysCompanySimpleVO */
export type SysCompanySimpleVO = {
  companyCode?: string
  companyName?: string
  id: number
  typeCode?: string
  typeName?: string
}

/** 维修登记故障与维修说明选项：`/api/system/work-order/{workOrderId}/repair-fault-options` */
export type WorkOrderRepairFaultOptionVO = {
  /** 故障描述 */
  faultDesc: string
  /** 维修说明选项 */
  repairOptions: string[]
}

/** GET `/api/system/work-order/list` 查询参数（与后端文档一致；服务端注入字段一般不必传） */
export type OrderListQuery = {
  barcode?: string
  companyId?: number
  /** 服务端可注入；特殊场景也可显式传 */
  currentUserId?: number
  customerMobile?: string
  customerName?: string
  dataScope?: string
  hasTransfer?: number
  isAsc?: string
  /** 主状态：如 PENDING_ASSIGN（待派单）、PENDING_TECH_ACCEPT（待接单）、IN_PROGRESS、COMPLETED、CLOSED */
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

/**
 * 追加查询参数
 * @param out 形如 ["a=1","b=2"] 的片段数组
 * @param key 参数名
 * @param value 参数值
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
 */
function buildWorkOrderQueryString(params: OrderListQuery): string {
  const parts: string[] = []
  appendQueryParam(parts, 'barcode', params.barcode)
  appendQueryParam(parts, 'companyId', params.companyId)
  appendQueryParam(parts, 'currentUserId', params.currentUserId)
  appendQueryParam(parts, 'customerMobile', params.customerMobile)
  appendQueryParam(parts, 'customerName', params.customerName)
  appendQueryParam(parts, 'dataScope', params.dataScope)
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
 * 将搜索框关键词映射为列表接口的单一模糊条件（MyBatis 中 orderNo / customerName / barcode 为 AND，不可同时传）
 */
export function applyWorkOrderListSearchKeyword(query: OrderListQuery, keyword: string) {
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
 * 将后端 mainStatus 规范为前端 OrderStatus（兼容常见枚举写法）
 * @param mainStatus 后端 mainStatus
 * @returns 前端 OrderStatus
 */
export function mapMainStatusToOrderStatus(mainStatus: string | undefined): OrderStatus {
  const raw = (mainStatus ?? '').trim()
  if (!raw) return 'pending'
  const s = raw.toUpperCase().replace(/-/g, '_')
  const map: Record<string, OrderStatus> = {
    PENDING: 'pending',
    PENDING_ASSIGN: 'pending', // 待派单
    PENDING_TECH_ACCEPT: 'pending', // 待接单
    WAIT: 'pending',
    WAIT_ACCEPT: 'pending',
    DISPATCH: 'pending',
    PROCESSING: 'processing',
    REPAIRING: 'processing',
    IN_PROGRESS: 'processing',
    COMPLETED: 'completed',
    DONE: 'completed',
    FINISH: 'completed',
    CLOSED: 'closed',
    CLOSE: 'closed',
    CANCELLED: 'closed',
  }
  if (map[s]) return map[s]
  const lower = raw.toLowerCase()
  if (isOrderStatus(lower)) return lower
  return 'pending'
}

/** 详情接口 `displayStatus`（WAIT_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED）→ 前端 OrderStatus */
function mapDisplayStatusToOrderStatus(displayStatus: string | undefined): OrderStatus | undefined {
  const raw = (displayStatus ?? '').trim()
  if (!raw) return undefined
  const s = raw.toUpperCase().replace(/-/g, '_')
  const map: Record<string, OrderStatus> = {
    WAIT_ACCEPT: 'pending',
    IN_PROGRESS: 'processing',
    COMPLETED: 'completed',
    CLOSED: 'closed',
  }
  return map[s]
}

function mapWarrantyStatusToLabel(status: string | undefined): string {
  const s = (status ?? '').trim().toUpperCase().replace(/-/g, '_')
  if (s === 'IN_WARRANTY') return '保内'
  if (s === 'OUT_OF_WARRANTY') return '保外'
  return String(status ?? '')
}

/** 列表卡片质保角标样式（与 OrderListItem.warrantyClass 一致） */
function mapWarrantyStatusToListTagClass(
  status: string | undefined
): 'tag-in-warranty' | 'tag-out-warranty' | undefined {
  const s = (status ?? '').trim().toUpperCase().replace(/-/g, '_')
  if (s === 'IN_WARRANTY') return 'tag-in-warranty'
  if (s === 'OUT_OF_WARRANTY') return 'tag-out-warranty'
  return undefined
}

function inferWarrantyTagClassFromLabel(label: string): 'tag-in-warranty' | 'tag-out-warranty' | undefined {
  const t = label.trim()
  if (/保内/.test(t)) return 'tag-in-warranty'
  if (/保外/.test(t)) return 'tag-out-warranty'
  return undefined
}

/** 列表 VO 上可能出现的「最后出库日期」字段（兼容多别名） */
/** 归一化接口 brandType（与详情映射一致） */
function normalizeWorkOrderBrandTypeCode(brandType?: string): string {
  return (brandType ?? '').trim().toUpperCase().replace(/-/g, '_')
}

/**
 * 根据接口 brandType 判断是否佳士品牌工单（与详情 mapWorkOrderDetailToOrderDetail 一致）
 * - 有 brandType 且为 JASIC → 佳士
 * - 有 brandType 且非 JASIC → 非佳士
 * - 无 brandType → 默认按佳士
 */
function mapBrandTypeToIsJiashi(brandType?: string): boolean {
  const norm = normalizeWorkOrderBrandTypeCode(brandType)
  return norm ? norm === 'JASIC' : true
}

function pickWorkOrderListOutDate(vo: WorkOrderListVO): string {
  const extra = vo as Record<string, unknown>
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
    const v = extra[k]
    const t = v != null ? String(v).trim() : ''
    if (t) return t
  }
  return ''
}

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

/** 列表 VO：维修方式展示（与详情 mapServiceModeToRepairMethodLabel 逻辑一致） */
function mapListVoRepairMethodLabel(vo: Pick<WorkOrderListVO, 'serviceMode' | 'serviceModeLabel'>): string {
  const label = String(vo.serviceModeLabel ?? '').trim()
  if (label) return label
  const mode = String(vo.serviceMode ?? '').trim().toUpperCase()
  if (mode === 'MAIL') return '邮寄维修'
  if (mode === 'STORE') return '送店维修'
  return String(vo.serviceMode ?? '').trim()
}

function formatListRepairPriceText(raw: unknown): string | undefined {
  if (raw === undefined || raw === null || raw === '') return undefined
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw.toFixed(2)
  const n = Number(raw)
  if (Number.isFinite(n)) return n.toFixed(2)
  const s = String(raw).trim()
  return s || undefined
}

/**
 * 将后端 WorkOrderListVO 规范为前端 OrderListItem
 * @param vo 后端 WorkOrderListVO
 * @returns 工单列表项
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
  const repairMethodRaw = mapListVoRepairMethodLabel(vo)
  const repairMethodLabel = repairMethodRaw || undefined
  const repairPriceText = formatListRepairPriceText(vo.quoteAmount)
  const acceptPhone = String(vo.currentAcceptCompanyPhone ?? '').trim()
  return {
    id: String(vo.id),
    orderNo: vo.orderNo,
    mainStatus: vo.mainStatus,
    assignedUserId: vo.assignedUserId,
    status,
    brandType: brandNorm || undefined,
    brandTypeLabel: brandTypeLabelRaw || undefined,
    isJiashi: mapBrandTypeToIsJiashi(vo.brandType),
    phone: vo.customerMobile ?? '',
    barcode: vo.barcode,
    model: vo.productModel,
    outDate,
    warrantyText,
    warrantyClass,
    faultDesc: faultDesc || undefined,
    transferred: vo.hasTransfer === 1,
    siteName: vo.currentAcceptCompanyName,
    repairMethodLabel,
    repairPriceText,
    acceptCompanyPhone: acceptPhone || undefined
  }
}

/**
 * 查询工单列表
 * @param params 查询参数
 * @returns 工单列表
 */
export async function fetchOrderList(params: OrderListQuery = {}) {
  const page = await fetchOrderListPage(params)
  return page.records
}

/**
 * 查询工单列表（分页）
 * @param params 查询参数
 * @returns 工单分页数据
 */
export async function fetchOrderListPage(params: OrderListQuery = {}): Promise<OrderListPage> {
  const qs = buildWorkOrderQueryString(params)
  const res = await http<WorkOrderListPageResult>({
    url: `/api/system/work-order/list?${qs}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '加载工单列表失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const page = unwrap(res)
  const records = Array.isArray(page?.records) ? page.records : []
  return {
    pageNum: Number(page?.pageNum) || Number(params.pageNum) || 1,
    pageSize: Number(page?.pageSize) || Number(params.pageSize) || 10,
    total: Number(page?.total) || 0,
    records: records.map(mapWorkOrderToListItem),
  }
}

/** 工单状态统计：`/api/system/work-order/status-count` */
export type WorkOrderStatusCountVO = {
  /** 数量（部分序列化场景为字符串） */
  countNum?: number | string
  /** 状态名称，如「待派单」 */
  displayStatus?: string
  /** 状态编码，如 PENDING_ASSIGN */
  mainStatus?: string
}

/** 统计卡片分项：待派单 / 待接单与列表 Tab 的 mainStatus 一致 */
export type WorkOrderStatusTabCounts = {
  pendingAssign: number
  pendingTechAccept: number
  processing: number
  completed: number
  closed: number
}

function parseWorkOrderStatusCountNum(v: unknown): number {
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'bigint') return Number(v)
  if (v == null || v === '') return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

/** mainStatus 缺省时用接口返回的展示文案推断枚举（与文档示例 displayStatus「待派单」等一致） */
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
    u === 'CLOSED' ||
    u === 'WAIT_ACCEPT'
  ) {
    return u === 'WAIT_ACCEPT' ? 'PENDING_TECH_ACCEPT' : u
  }
  return ''
}

/**
 * 汇总 status-count 接口返回行（待派单、待接单按 mainStatus 区分；其余走 mapMainStatusToOrderStatus）
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
    if (bucket === 'pending') {
      out.pendingAssign += count
      continue
    }
    if (bucket === 'processing') out.processing += count
    else if (bucket === 'completed') out.completed += count
    else if (bucket === 'closed') out.closed += count
  }
  return out
}

/**
 * 取 status-count 中某一 mainStatus 的展示数据：countNum 求和，displayStatus 取首条非空（与接口字段一致）。
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
 * GET `/api/system/work-order/status-count`
 */
export async function fetchWorkOrderStatusCount(params: OrderListQuery = {}) {
  const qs = buildWorkOrderQueryString({
    ...params,
    // 统计接口不需要分页字段，但后端允许传；这里不强行剔除，保持与文档字段一致
  })
  const res = await http<WorkOrderStatusCountVO[]>({
    url: `/api/system/work-order/status-count${qs ? `?${qs}` : ''}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '加载工单统计失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const list = unwrap(res)
  return Array.isArray(list) ? list : []
}

/**
 * 查询可派单人员
 * GET `/api/system/work-order/{workOrderId}/assign-user-options`
 */
export async function fetchAssignUserOptions(workOrderId: number) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return []
  const res = await http<WorkOrderUserOptionVO[]>({
    url: `/api/system/work-order/${encodeURIComponent(String(id))}/assign-user-options`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = unwrap(res)
  return Array.isArray(list) ? list : []
}

/**
 * 查询可转单目标
 * GET `/api/system/work-order/{workOrderId}/transfer-target-options`
 */
export async function fetchTransferTargetOptions(workOrderId: number) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return []
  const res = await http<SysCompanySimpleVO[]>({
    url: `/api/system/work-order/${encodeURIComponent(String(id))}/transfer-target-options`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = unwrap(res)
  return Array.isArray(list) ? list : []
}

/**
 * 查询维修登记可选故障与维修说明
 * GET `/api/system/work-order/{workOrderId}/repair-fault-options`
 */
export async function fetchRepairFaultOptions(workOrderId: number) {
  const id = Number(workOrderId)
  if (!Number.isFinite(id) || id <= 0) return []
  const res = await http<WorkOrderRepairFaultOptionVO[]>({
    url: `/api/system/work-order/${encodeURIComponent(String(id))}/repair-fault-options`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const list = unwrap(res)
  return Array.isArray(list) ? list : []
}

/** 详情页统一解析维修路径枚举（寄件相关展示以该字段为准） */
function normalizeDetailServiceMode(vo: Pick<WorkOrderDetailVO, 'serviceMode'>): 'MAIL' | 'STORE' | '' {
  const m = String(vo.serviceMode || '').trim().toUpperCase()
  if (m === 'MAIL' || m === 'STORE') return m
  return ''
}

/** 无中文标签时，用 serviceMode 枚举兜底展示文案 */
function mapServiceModeToRepairMethodLabel(vo: Pick<WorkOrderDetailVO, 'serviceModeLabel' | 'serviceMode'>): string {
  const label = String(vo.serviceModeLabel || '').trim()
  if (label) return label
  const mode = String(vo.serviceMode || '').trim().toUpperCase()
  if (mode === 'MAIL') return '邮寄维修'
  if (mode === 'STORE') return '送店维修'
  return String(vo.serviceMode || '').trim()
}

/** 机器返回方式：兼容枚举/英文值，统一成「回寄/自提」等展示文案 */
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

/** 详情里附件项：兼容 preview_url / url 等字段及字符串数组 */
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

/** 维修登记单条上的 `SysFileItemVO[]` → 历史/详情故障点图列表 */
function mapSysFileItemsToLabeledImages(
  files: SysFileItemVO[] | undefined,
  labelPrefix: string,
): { url: string; label: string }[] {
  if (!Array.isArray(files) || !files.length) return []
  return [...files]
    .sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
    .map((file, i) => {
      const url = String(resolveSysFileItemPreviewUrl(file) || '').trim()
      if (!url) return null
      return { url, label: `${labelPrefix}${i + 1}` }
    })
    .filter((x): x is { url: string; label: string } => x != null)
}

function pickFirstPreviewUrl(files: SysFileItemVO[] | undefined): string {
  if (!Array.isArray(files) || !files.length) return ''
  return resolveSysFileItemPreviewUrl(files[0])
}

function mapSenderVoucherFiles(files: SysFileItemVO[] | undefined): { previewUrl: string }[] {
  if (!Array.isArray(files) || !files.length) return []
  return files
    .map((f) => ({ previewUrl: resolveSysFileItemPreviewUrl(f) }))
    .filter((x) => x.previewUrl)
}

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

function sortWorkOrderRepairsByCreateTime(repairs: WorkOrderRepairVO[]): WorkOrderRepairVO[] {
  return repairs
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
}

function isRecheckRegisterStage(r: WorkOrderRepairVO): boolean {
  const a = String(r.registerStage || '').toUpperCase()
  const b = String(r.registerStageLabel || '')
  return a.includes('RECHECK') || a.includes('REVIEW') || b.includes('复检')
}

/** 复检表单回显：取「最后一次非复检」的维修登记；若均为复检则退回末条 */
function pickRepairForRecheckFormEcho(sortedRepairs: WorkOrderRepairVO[]): WorkOrderRepairVO | undefined {
  for (let i = sortedRepairs.length - 1; i >= 0; i--) {
    const r = sortedRepairs[i]!
    if (!isRecheckRegisterStage(r)) return r
  }
  return sortedRepairs.length ? sortedRepairs[sortedRepairs.length - 1] : undefined
}

function inferRepairItemsFromFaults(faults: WorkOrderFaultVO[]): string[] {
  const out: string[] = []
  const seen = new Set<string>()
  const sorted = [...faults].sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
  for (const f of sorted) {
    const rd = String(f.repairDesc || '').trim()
    if (!rd) continue
    for (const seg of rd.split(/[、,，]/)) {
      const s = seg.trim()
      if (!s || seen.has(s)) continue
      seen.add(s)
      out.push(s)
    }
  }
  return out
}

function inferRepairItemsFromRepairVo(r: WorkOrderRepairVO, faults: WorkOrderFaultVO[]): string[] {
  const ri = r.repairItems
  if (Array.isArray(ri) && ri.length) {
    return ri.map((x) => String(x || '').trim()).filter(Boolean)
  }
  const fromFaults = inferRepairItemsFromFaults(faults)
  if (fromFaults.length) return fromFaults
  const levelRd = String(r.repairDesc || '').trim()
  if (!levelRd) return []
  return levelRd.split(/[、,，]/).map((s) => s.trim()).filter(Boolean)
}

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

/** 从详情 repairs 构建复检登记表单预填 */
function buildRepairRegistrationEcho(
  repairs: WorkOrderRepairVO[] | undefined | null,
): OrderRepairRegistrationEcho | undefined {
  const sorted = sortWorkOrderRepairsByCreateTime(Array.isArray(repairs) ? repairs : [])
  const r = pickRepairForRecheckFormEcho(sorted)
  if (!r) return undefined
  const faults = Array.isArray(r.faults) ? r.faults : []
  return {
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

/** 单条维修登记 `WorkOrderRepairVO` → 若干条故障点历史记录 */
function mapOneWorkOrderRepairToFaultRecords(r: WorkOrderRepairVO): FaultPointRecord[] {
  const faults = Array.isArray(r.faults) ? r.faults : []
  const when = String(r.createTime || '')
  const site = String(r.companyName || '').trim()
  const repairLevelImages = [
    ...mapSysFileItemsToLabeledImages(r.faultOldImageFiles, '旧件图'),
    ...mapSysFileItemsToLabeledImages(r.faultNewImageFiles, '新件图'),
    ...mapSysFileItemsToLabeledImages(r.machineImageFiles, '整机图'),
    ...mapSysFileItemsToLabeledImages(r.machineBarcodeImageFiles, '条码图'),
    ...mapSysFileItemsToLabeledImages(r.otherImageFiles, '其它图'),
  ]
  return faults.map((f) => {
    const faultDesc = String(f.faultDesc || '').trim()
    const repairDesc = String(f.repairDesc || '').trim()
    const otherDesc = String(f.otherDesc || '').trim()
    const repairMain = repairDesc === '其它维修说明' ? otherDesc : repairDesc
    const description = [faultDesc, repairMain].filter(Boolean).join(' · ')
    const specialInfo =
      otherDesc && repairDesc !== '其它维修说明' ? otherDesc : undefined
    const fromLegacyUrls = (String(f.imageUrls || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean) as string[]).map((url, i) => ({ url, label: `图${i + 1}` }))
    const partsFromList = faultPartsFromWorkOrderFault(f)
    const parts =
      partsFromList.length > 0 ? partsFromList : parseRepairPartDesc(String(f.partDesc || ''))
    return {
      description,
      faultDesc,
      repairDesc,
      otherDesc,
      images: [...fromLegacyUrls, ...repairLevelImages],
      parts,
      specialInfo,
      location: site,
      date: String(f.createTime || when || '').trim(),
    }
  })
}

/**
 * 将详情接口 `data.repairs`（工单维修登记列表）映射为故障点历史列表。
 * 与详情页 `faultPoint.allRepairsFaultRecords` 同源逻辑。
 */
export function mapWorkOrderRepairsToAllFaultPointRecords(
  repairs: WorkOrderRepairVO[] | undefined | null,
): FaultPointRecord[] {
  const list = Array.isArray(repairs) ? repairs : []
  return sortWorkOrderRepairsByCreateTime(list).flatMap(mapOneWorkOrderRepairToFaultRecords)
}

function mapWorkOrderDetailToOrderDetail(vo: WorkOrderDetailVO): OrderDetail {
  const status =
    mapDisplayStatusToOrderStatus(vo.displayStatus) ?? mapMainStatusToOrderStatus(vo.mainStatus)
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

  const allRepairsFaultRecords = sortedRepairs.flatMap(mapOneWorkOrderRepairToFaultRecords)

  const history =
    sortedRepairs.length && latestRepair
      ? sortedRepairs.slice(0, -1).flatMap(mapOneWorkOrderRepairToFaultRecords)
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

  return {
    id: String(vo.id ?? ''),
    status,
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
      outDate: '',
      warrantyClass: mapWarrantyStatusToLabel(vo.warrantyStatus),
      repairStatus: String(vo.mainStatusLabel || vo.displayStatus || ''),
    },
    service: {
      sitePhone: '',
      serviceMode: normalizeDetailServiceMode(vo),
      repairMethod: mapServiceModeToRepairMethodLabel(vo),
      source: String(vo.createCompanyName || ''),
      senderInfo: [
        quote?.senderName || vo.senderName,
        quote?.senderMobile || vo.senderMobile,
        quote?.senderAddress || vo.senderAddress
      ]
        .filter(Boolean)
        .join(' / '),
      senderName: String(quote?.senderName || vo.senderName || ''),
      senderMobile: String(quote?.senderMobile || vo.senderMobile || ''),
      senderAddress: String(quote?.senderAddress || vo.senderAddress || ''),
      sendExpressNo: String(quote?.sendExpressNo || vo.sendExpressNo || ''),
      senderVoucherImg: pickFirstPreviewUrl(vo.senderVoucherFiles),
      senderVoucherFiles: mapSenderVoucherFiles(vo.senderVoucherFiles),
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
    acceptor: {
      currentAcceptCompanyName: String(vo.currentAcceptCompanyName || '').trim(),
      sitePhone: '',
    },
    fault: {
      desc: String(vo.faultDesc || ''),
      faultExplain: String(vo.faultRemark || ''),
      voiceDuration: '',
      images: (Array.isArray(vo.faultImageFiles) ? vo.faultImageFiles : [])
        .map((f) => String(f.previewUrl || '').trim())
        .filter(Boolean),
      videos: (Array.isArray(vo.faultVideoFiles) ? vo.faultVideoFiles : [])
        .map((f) => String(f.previewUrl || '').trim())
        .filter(Boolean),
      videoThumb: pickFirstPreviewUrl(vo.faultVideoFiles),
    },
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
 */
export async function fetchOrderDetail(id: string) {
  const wid = String(id || '').trim()
  if (!wid) {
    const msg = '工单ID无效'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const res = await http<WorkOrderDetailVO>({
    url: `/api/system/work-order/${encodeURIComponent(wid)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '加载工单详情失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const vo = unwrap(res)
  if (vo == null || typeof vo !== 'object') {
    const msg = '工单详情数据为空'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  return mapWorkOrderDetailToOrderDetail(vo)
}

/**
 * GET 工单详情，仅将 `data.repairs` 映射为故障点历史列表（历史维修记录页使用）。
 */
export async function fetchOrderRepairFaultRecords(id: string): Promise<FaultPointRecord[]> {
  const wid = String(id || '').trim()
  if (!wid) {
    const msg = '工单ID无效'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const res = await http<WorkOrderDetailVO>({
    url: `/api/system/work-order/${encodeURIComponent(wid)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '加载维修记录失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  const vo = unwrap(res)
  if (vo == null || typeof vo !== 'object') {
    const msg = '工单详情数据为空'
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }
  return mapWorkOrderRepairsToAllFaultPointRecords(vo.repairs)
}

/**
 * 查询网点列表
 * @returns 网点列表
 */
export async function fetchBranchList() {
  const res = await http<BranchItem[]>({
    url: '/order/branch/list',
    method: 'GET',
  })
  return unwrap(res)
}

/**
 * 查询网点工单列表
 * @param branchName 网点名称
 * @returns 网点工单列表
 */
export async function fetchOrdersByBranch(branchName: string) {
  const res = await http<OrderListItem[]>({
    url: '/order/branch/orders',
    method: 'GET',
    data: { branchName },
  })
  return unwrap(res)
}

export type WorkOrderAssignDTO = {
  /** 维修员ID */
  assignedUserId: number
  /** 工单ID */
  workOrderId: number
}

export type WorkOrderTransferDTO = {
  /** 转单备注 */
  remark?: string
  /** 目标公司ID */
  targetCompanyId: number
  /** 工单ID */
  workOrderId: number
}

export type WorkOrderCloseDTO = {
  /** 关闭原因 */
  closeReason: string
  /** 回寄快递单号 */
  returnExpressNo?: string
  /** 机器返回方式（与后端一致：自提 | 回寄） */
  returnMethod: string
  /** 回寄凭证文件ID */
  returnVoucherFileIds?: number[]
  /** 工单ID */
  workOrderId: number
}

/** 机器返回方式弹窗 `confirm` 载荷（关闭工单等场景复用） */
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
 */
export const WORK_ORDER_FAULT_CLOSE_REASON = '有故障维修完成'

export type WorkOrderTechAcceptDTO = {
  /** 工单ID */
  workOrderId: number
  /** 故障判定（与详情页一致：有故障 | 无故障） */
  faultJudge?: string
  /** 维修报价金额 */
  quoteAmount?: number
  /** 报价说明 */
  quoteDesc?: string
}

/** 维修登记 / 复检 body 内配件明细（WorkOrderFaultPartItemDTO） */
export type WorkOrderFaultPartItemDTO = {
  partName?: string
  partQty?: number
}

/**
 * POST `/api/system/work-order/repair` 请求体（WorkOrderRepairDTO）
 * @see workOrderId 必填；其余字段按后端文档均为可选
 */
export type WorkOrderRepairDTO = {
  /** 故障处新图片文件 ID */
  faultNewImageFileIds?: number[]
  /** 故障处旧图片文件 ID */
  faultOldImageFileIds?: number[]
  /** 机器条码照片文件 ID */
  machineBarcodeImageFileIds?: number[]
  /** 机器正面照片文件 ID */
  machineImageFileIds?: number[]
  /** 其他维修说明 */
  otherDesc?: string
  /** 其他图片文件 ID */
  otherImageFileIds?: number[]
  /** 工单故障点配件明细 */
  partList?: WorkOrderFaultPartItemDTO[]
  /** 调整后的报价金额 */
  quoteAmount?: number
  /** 调整后的报价说明 */
  quoteDesc?: string
  /** 手工填写的维修说明 */
  repairDesc?: string
  /** 维修说明选项 */
  repairItems?: string[]
  /** 工单 ID */
  workOrderId: number
}

/**
 * POST `/api/system/work-order/review` 保存复检记录（WorkOrderReviewDTO）
 * workOrderId 必填；其余字段可选，与接口文档一致
 */
export type WorkOrderReviewDTO = {
  /** 故障处新图片文件 ID */
  faultNewImageFileIds?: number[]
  /** 故障处旧图片文件 ID */
  faultOldImageFileIds?: number[]
  /** 机器条码照片文件 ID */
  machineBarcodeImageFileIds?: number[]
  /** 机器正面照片文件 ID */
  machineImageFileIds?: number[]
  /** 其他维修说明 */
  otherDesc?: string
  /** 其他图片文件 ID */
  otherImageFileIds?: number[]
  /** 工单故障点配件明细 */
  partList?: WorkOrderFaultPartItemDTO[]
  /** 手工填写的维修说明 */
  repairDesc?: string
  /** 维修说明选项 */
  repairItems?: string[]
  /** 工单 ID */
  workOrderId: number
}

/**
 * 代客户填写创建工单请求体（WorkOrderProxyCreateDTO）
 * POST `/api/system/work-order/create/proxy`，Content-Type: application/json
 * 接口约定：customerMobile、serviceMode 必填；其余字段按文档均为可选。
 */
export type WorkOrderProxyCreateDTO = {
  /** 机器条码 */
  barcode?: string
  /** 客户手机号（必填） */
  customerMobile: string
  /** 客户姓名 */
  customerName?: string
  /** 故障图片文件ID */
  faultImageFileIds?: number[]
  /** 故障描述选项 */
  faultItems?: string[]
  /** 故障备注 */
  faultRemark?: string
  /** 故障视频文件ID */
  faultVideoFileIds?: number[]
  /** 故障语音文件ID */
  faultVoiceFileIds?: number[]
  /** 寄件快递单号 */
  sendExpressNo?: string
  /** 寄件地址 */
  senderAddress?: string
  /** 寄件人手机号 */
  senderMobile?: string
  /** 寄件人姓名 */
  senderName?: string
  /** 寄件凭证文件ID */
  senderVoucherFileIds?: number[]
  /** 服务方式编码 MAIL | STORE（必填） */
  serviceMode: 'MAIL' | 'STORE'
}

/** 二级报修一级创建工单 DTO */
export type WorkOrderUpstreamCreateDTO = {
  /** 机器条码 */
  barcode: string
  /** 客户手机号 */
  customerMobile: string
  /** 客户姓名 */
  customerName: string
  /** 故障图片文件ID */
  faultImageFileIds?: number[]
  /** 故障描述选项 */
  faultItems?: string[]
  /** 故障备注 */
  faultRemark?: string
  /** 故障视频文件ID */
  faultVideoFileIds?: number[]
  /** 故障语音文件ID */
  faultVoiceFileIds?: number[]
  /** 寄件快递单号 */
  sendExpressNo?: string
  /** 寄件地址 */
  senderAddress?: string
  /** 寄件人手机号 */
  senderMobile?: string
  /** 寄件人姓名 */
  senderName?: string
  /** 寄件凭证文件ID */
  senderVoucherFileIds?: number[]
  /** 服务方式编码 */
  serviceMode: 'MAIL' | 'STORE'
  /** 目标受理公司ID */
  targetCompanyId?: number
}

/** 公司简要信息 VO（条码查询返回） */
export type BarcodeInfoCompanyOptionVO = {
  companyCode?: string
  companyName?: string
  id?: number
  typeCode?: string
  typeName?: string
}

/** 条码查询接口成功返回：业务数据 + 顶层 msg（unwrap 会丢 msg，故单独带出） */
export type BarcodeInfoFetchResult = {
  data: WorkOrderCreateBarcodeInfoVO
  msg: string
}

/** 代客户填写条码信息 VO */
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
 * POST `/api/system/work-order/create/proxy`
 */
export async function createProxyWorkOrder(dto: WorkOrderProxyCreateDTO) {
  const res = await http<number>({
    url: '/api/system/work-order/create/proxy',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '创建工单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 二级报修一级创建工单
 * POST `/api/system/work-order/create/upstream-first`
 */
export async function createUpstreamFirstWorkOrder(dto: WorkOrderUpstreamCreateDTO) {
  const res = await http<number>({
    url: '/api/system/work-order/create/upstream-first',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '创建工单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 一级报修佳士创建工单
 * POST `/api/system/work-order/create/upstream-hq`
 */
export async function createUpstreamHqWorkOrder(dto: WorkOrderUpstreamCreateDTO) {
  const res = await http<number>({
    url: '/api/system/work-order/create/upstream-hq',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '创建工单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 查询代客户填写条码信息
 * GET `/api/system/work-order/create/proxy/barcode-info`
 */
export async function fetchProxyBarcodeInfo(barcode: string): Promise<BarcodeInfoFetchResult> {
  const code = String(barcode || '').trim()
  if (!code) throw new Error('barcode is required')
  const res = await http<WorkOrderCreateBarcodeInfoVO>({
    url: `/api/system/work-order/create/proxy/barcode-info?barcode=${encodeURIComponent(code)}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '条码查询失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return {
    data: unwrap(res),
    msg: getApiMessage(res, '查询成功'),
  }
}

/**
 * 查询一级报修佳士条码信息
 * GET `/api/system/work-order/create/upstream-hq/barcode-info`
 */
export async function fetchUpstreamFirstBarcodeInfo(
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
    url: `/api/system/work-order/create/upstream-hq/barcode-info?${qs}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '条码查询失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return {
    data: unwrap(res),
    msg: getApiMessage(res, '查询成功'),
  }
}

/**
 * 派单
 * PUT `/api/system/work-order/assign`
 */
export async function assignWorkOrder(dto: WorkOrderAssignDTO) {
  const res = await http<void>({
    url: '/api/system/work-order/assign',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    // http.ts 已对非 2xx 做了 reject；这里兜底处理业务 code 非成功的情况（文档 00000 / 部分接口 200）
    const msg = getApiMessage(res, '派单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 关闭工单
 * PUT `/api/system/work-order/close`
 */
export async function closeWorkOrder(dto: WorkOrderCloseDTO) {
  const res = await http<void>({
    url: '/api/system/work-order/close',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '关闭工单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 转单
 * PUT `/api/system/work-order/transfer`
 */
export async function transferWorkOrder(dto: WorkOrderTransferDTO) {
  const res = await http<void>({
    url: '/api/system/work-order/transfer',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '转单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 维修员接单
 * PUT `/api/system/work-order/tech-accept`
 */
export async function techAcceptWorkOrder(dto: WorkOrderTechAcceptDTO) {
  const res = await http<void>({
    url: '/api/system/work-order/tech-accept',
    method: 'PUT',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '接单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 保存维修登记
 * POST `/api/system/work-order/repair`，Content-Type: application/json
 * 请求体字段与 WorkOrderRepairDTO 一致（workOrderId 必填）
 */
export async function submitWorkOrderRepair(dto: WorkOrderRepairDTO) {
  const res = await http<void>({
    url: '/api/system/work-order/repair',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '维修登记失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

/**
 * 保存复检记录
 * POST `/api/system/work-order/review`
 */
export async function submitWorkOrderReview(dto: WorkOrderReviewDTO) {
  const res = await http<void>({
    url: '/api/system/work-order/review',
    method: 'POST',
    data: dto,
    header: {
      'Content-Type': 'application/json',
    },
  })

  if (!isBizSuccess(res.code)) {
    const msg = getApiMessage(res, '复检登记失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

