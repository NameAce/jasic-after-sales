import { getApiMessage, http, unwrap } from '@/utils/http'
import type {
  BranchItem,
  OrderDetail,
  OrderListItem,
  OrderStatus,
  SysFileItemVO,
  WorkOrderDetailVO,
  WorkOrderListPageResult,
  WorkOrderListVO,
} from '@/models/order'
import { isOrderStatus } from '@/utils/orderStatus'

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

/** GET `/api/system/work-order/list` 查询参数（与后端文档一致；服务端注入字段不必传） */
export type OrderListQuery = {
  barcode?: string
  companyId?: number
  customerMobile?: string
  customerName?: string
  hasTransfer?: number
  isAsc?: string
  mainStatus?: string
  orderByColumn?: string
  orderNo?: string
  pageNum?: number
  pageSize?: number
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
  appendQueryParam(parts, 'customerMobile', params.customerMobile)
  appendQueryParam(parts, 'customerName', params.customerName)
  appendQueryParam(parts, 'hasTransfer', params.hasTransfer)
  appendQueryParam(parts, 'isAsc', params.isAsc)
  appendQueryParam(parts, 'mainStatus', params.mainStatus)
  appendQueryParam(parts, 'orderByColumn', params.orderByColumn)
  appendQueryParam(parts, 'orderNo', params.orderNo)
  appendQueryParam(parts, 'pageNum', params.pageNum ?? 1)
  appendQueryParam(parts, 'pageSize', params.pageSize ?? 20)
  appendQueryParam(parts, 'viewScope', params.viewScope)
  return parts.join('&')
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
    PENDING_ASSIGN: 'pending',
    PENDING_TECH_ACCEPT: 'pending',
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

/**
 * 将后端 WorkOrderListVO 规范为前端 OrderListItem
 * @param vo 后端 WorkOrderListVO
 * @returns 工单列表项
 */
function mapWorkOrderToListItem(vo: WorkOrderListVO): OrderListItem {
  const status = mapMainStatusToOrderStatus(vo.mainStatus)
  const parts = [vo.customerName, vo.productModel, vo.displayStatus, vo.mainStatusLabel].filter(
    Boolean,
  ) as string[]
  const desc = parts.length ? parts.join(' · ') : '—'
  return {
    id: String(vo.id),
    orderNo: vo.orderNo,
    status,
    isJiashi: true,
    phone: vo.customerMobile ?? '',
    barcode: vo.barcode,
    model: vo.productModel,
    desc,
    transferred: vo.hasTransfer === 1,
    siteName: vo.currentAcceptCompanyName,
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
  const page = unwrap(res)
  const records = Array.isArray(page?.records) ? page.records : []
  return {
    pageNum: Number(page?.pageNum) || Number(params.pageNum) || 1,
    pageSize: Number(page?.pageSize) || Number(params.pageSize) || 20,
    total: Number(page?.total) || 0,
    records: records.map(mapWorkOrderToListItem),
  }
}

/** 工单状态统计：`/api/system/work-order/status-count` */
export type WorkOrderStatusCountVO = {
  /** 数量 */
  countNum: number
  /** 状态名称 */
  displayStatus?: string
  /** 状态编码 */
  mainStatus?: string
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

function pickFirstPreviewUrl(files: SysFileItemVO[] | undefined): string {
  if (!Array.isArray(files) || !files.length) return ''
  return String(files[0]?.previewUrl || '')
}

function mapWorkOrderDetailToOrderDetail(vo: WorkOrderDetailVO): OrderDetail {
  const status = mapMainStatusToOrderStatus(vo.mainStatus)
  const transferred = vo.hasTransfer === 1 || (vo.transferCount ?? 0) > 0

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

  // 故障点：取 repairs 最后一条的 faults 作为“当前维修记录”，其余作为 history
  const repairs = Array.isArray(vo.repairs) ? vo.repairs : []
  const sortedRepairs = repairs
    .slice()
    .sort((a, b) => String(a.createTime || '').localeCompare(String(b.createTime || '')))
  const latestRepair = sortedRepairs.length ? sortedRepairs[sortedRepairs.length - 1] : undefined
  const latestFault =
    latestRepair?.faults && latestRepair.faults.length
      ? latestRepair.faults
          .slice()
          .sort((a, b) => Number(a.sortNum ?? 0) - Number(b.sortNum ?? 0))
          [latestRepair.faults.length - 1]
      : undefined

  const history =
    sortedRepairs.length && latestRepair
      ? sortedRepairs
          .slice(0, -1)
          .flatMap((r) => {
            const faults = Array.isArray(r.faults) ? r.faults : []
            const when = String(r.createTime || '')
            return faults.map((f) => ({
              description: String(f.faultDesc || ''),
              images: (String(f.imageUrls || '')
                .split(',')
                .map((s) => s.trim())
                .filter(Boolean) as string[]).map((url) => ({ url, label: '' })),
              parts: [],
              specialInfo: String(f.otherDesc || ''),
              location: '',
              date: when,
            }))
          })
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
      // 当前项目默认“佳事”视图；若后续需要可根据 brandCode/brandName 精细化
      isJiashi: true,
    },
    customer: {
      phone: String(vo.customerMobile || ''),
    },
    base: {
      orderNo: String(vo.orderNo || ''),
      orderTypeName: String(vo.reportBizTypeLabel || vo.createEntryType || ''),
      submitTime: String(vo.createTime || ''),
      transferSite: String(vo.currentAcceptCompanyName || ''),
      transferFromSite,
    },
    product: {
      barcode: String(vo.barcode || ''),
      model: String(vo.productModel || ''),
      serialNo: String(vo.machineNo || ''),
      outDate: '',
      warrantyClass: String(vo.warrantyStatus || ''),
      repairStatus: String(vo.displayStatus || ''),
    },
    service: {
      sitePhone: '',
      repairMethod: String(vo.serviceMode || ''),
      source: String(vo.createCompanyName || ''),
      senderInfo: [vo.senderName, vo.senderMobile, vo.senderAddress].filter(Boolean).join(' / '),
      senderVoucherImg: pickFirstPreviewUrl(vo.senderVoucherFiles),
      // mailReturnForm: 后端详情未返回收件人表单结构，这里先不回显
    },
    acceptor: {
      sitePhone: '',
      acceptorName: String(vo.assignedUserName || ''),
    },
    fault: {
      desc: String(vo.faultDesc || ''),
      faultExplain: String(vo.faultRemark || ''),
      voiceDuration: '',
      images: (Array.isArray(vo.faultImageFiles) ? vo.faultImageFiles : [])
        .map((f) => String(f.previewUrl || ''))
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
        date: String(latestRepair?.createTime || ''),
        desc: String(latestFault?.faultDesc || latestRepair?.repairSummary || ''),
      },
      history,
    },
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
  const res = await http<WorkOrderDetailVO>({
    url: `/api/system/work-order/${encodeURIComponent(String(id || '').trim())}`,
    method: 'GET',
    header: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  const vo = unwrap(res)
  return mapWorkOrderDetailToOrderDetail(vo)
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
  /** 机器返回方式 */
  returnMethod: string
  /** 回寄凭证文件ID */
  returnVoucherFileIds?: number[]
  /** 工单ID */
  workOrderId: number
}

export type WorkOrderTechAcceptDTO = {
  /** 工单ID */
  workOrderId: number
}

/** 代客户填写创建工单 DTO */
export type WorkOrderProxyCreateDTO = {
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

function isOkCode(code: unknown) {
  const n = Number(code)
  return Number.isFinite(n) ? n === 200 : String(code) === '200'
}

function isBizSuccess(code: unknown) {
  if (String(code) === '00000') return true
  return isOkCode(code)
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
export async function fetchProxyBarcodeInfo(barcode: string) {
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

  return unwrap(res)
}

/**
 * 查询一级报修佳士条码信息
 * GET `/api/system/work-order/create/upstream-hq/barcode-info`
 */
export async function fetchUpstreamFirstBarcodeInfo(barcode: string, targetCompanyId?: number) {
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

  return unwrap(res)
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

  if (!isOkCode(res.code)) {
    // http.ts 已对非 2xx 做了 reject；这里兜底处理业务 code 非 200 的情况
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

  if (!isOkCode(res.code)) {
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

  if (!isOkCode(res.code)) {
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

  if (!isOkCode(res.code)) {
    const msg = getApiMessage(res, '接单失败')
    uni.showToast({ title: msg, icon: 'none' })
    throw new Error(msg)
  }

  return res
}

