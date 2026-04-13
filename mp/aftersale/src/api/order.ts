import { http } from '@/utils/http'
// 故障点图片项
export interface FaultPointImageItem {
  url: string
  label: string
}

// 故障点配件项
export interface FaultPointPartItem {
  name: string
  count: number
}

// 故障点维修记录
export interface FaultPointMaintenanceRecord {
  description: string
  images: FaultPointImageItem[]
  parts?: FaultPointPartItem[]
  specialInfo?: string
  location: string
  date: string
}

// 工单详情
export interface OrderDetailDTO {
  status: string
  /** 是否可评价（来自 `/api/customer/work-order/{id}` 的 canEvaluate） */
  canEvaluate?: boolean
  base: {
    orderNo: string
    orderTypeName: string
    submitTime: string
  }
  product: {
    barcode: string
    model: string
    serialNo: string
    /** 非佳士报修时填写的品牌名称 */
    brandName?: string
  }
  service: {
    sitePhone: string
    repairMethod: string
    /** 维修方式展示文案 */
    serviceModeLabel?: string
    senderInfo: string
    senderVoucherImg?: string
  }
  acceptor: {
    sitePhone: string
    acceptorName: string
  }
  fault: {
    desc: string
    remark?: string
    /** 展示用时长文案，如 0:32 或与录音页一致的秒数 */
    voiceDuration?: string
    /** 单条语音地址（与报修页上传后返回的 URL 一致） */
    voiceUrl?: string
    /** 多条语音（与报修页 voiceList 一致，duration 为毫秒） */
    voiceList?: { url: string; duration?: number }[]
    images: string[]
    videoThumb?: string
    /** 故障视频可播放地址 */
    videoUrl?: string
  }
  repair: {
    faultJudge?: string
    quoteAmount?: string
    quoteDesc?: string
    repairTime?: string
    returnMethod?: string
    returnReceiverTitle?: string
    returnAddress?: string
    returnExpressNo?: string
    returnExpressVoucherImg?: string
  }
  faultPoint: {
    current: {
      date?: string
      desc?: string
    }
    /** 故障点维修历史（与历史记录页列表项一致） */
    records?: FaultPointMaintenanceRecord[]
  }
  contact: {
    phone?: string
  }
  evaluate?: {
    timeliness: number
    quality: number
    satisfaction: number
    comment?: string
  }
}

// ========== 工单详情（新接口） ==========

/** 工单附件（故障图/视频/语音、寄回执等），兼容 fileId / fileID 等字段名 */
export interface CustomerWorkOrderFileDTO {
  bizId?: number
  bizID?: number
  bizType?: string
  contentType?: string
  fileExt?: string
  fileId?: number
  fileID?: number
  fileSize?: number
  isPrimary?: number
  originalName?: string
  previewUrl?: string
  sortNum?: number
}

// 客户侧工单详情
export interface CustomerWorkOrderDetailDTO {
  assignedUserName: string
  barcode: string
  brandCode: string
  /** 品牌名称（接口扩展） */
  brandName?: string
  canEditSendInfo: boolean
  canEvaluate: boolean
  closeReason: string
  closedTime: string
  completedTime: string
  createTime: string
  currentAcceptCompanyName: string
  customerId: number
  customerMobile: string
  customerName: string
  displayStatus: string
  evaluateStatus: string
  evaluateStatusLabel: string
  evaluation?: {
    companyId: number
    content: string
    createTime: string
    customerId: number
    id: number
    qualityScore: number
    satisfactionScore: number
    tags: string
    timelinessScore: number
  }
  faultDesc: string
  faultRemark: string
  faultImageFiles?: CustomerWorkOrderFileDTO[]
  faultVideoFiles?: CustomerWorkOrderFileDTO[]
  faultVoiceFiles?: CustomerWorkOrderFileDTO[]
  hqCompanyId: number
  id: number
  machineNo: string
  mainStatus: string
  orderNo: string
  productCode: string
  productModel: string
  productName: string
  /** 报修业务类型编码，如 JASIC_BARCODE / NON_JASIC / NO_BARCODE */
  brandType?: string
  /** 报修业务类型名称，对应详情页「工单类型」 */
  brandTypeLabel?: string
  quotes?: Array<{
    companyId: number
    companyName: string
    createTime: string
    faultJudge: string
    id: number
    isCurrentValid: number
    quoteAmount: number
    quoteDesc: string
    quotedBy: number
    quotedByName: string
  }>
  repairs?: Array<{
    companyId: number
    companyName: string
    createTime: string
    faults?: Array<{
      companyId: number
      createTime: string
      createdBy: number
      createdByName: string
      faultDesc: string
      id: number
      imageUrls: string
      otherDesc: string
      partDesc: string
      repairDesc: string
      sortNum: number
    }>
    finishedTime: string
    id: number
    isFinished: number
    repairUserId: number
    repairUserName: string
  }>
  returnExpressNo: string
  returnMethod: string
  returnVoucherFiles?: CustomerWorkOrderFileDTO[]
  reviews?: Array<{
    companyId: number
    companyName: string
    createTime: string
    id: number
    isContinueRepair: number
    reviewDesc: string
    reviewResult: string
    reviewUserId: number
    reviewUserName: string
  }>
  sendExpressNo: string
  senderAddress: string
  senderMobile: string
  senderName: string
  senderVoucherFiles?: CustomerWorkOrderFileDTO[]
  /** 受理网点联系电话（与列表 sitePhone 一致，优先于客户手机号展示为网点电话） */
  sitePhone?: string
  serviceMode: string
  serviceModeLabel?: string
  warrantyStatus: string
}

/**
 * 格式化报价金额
 * @param v - 报价金额
 * @returns - 格式化后的报价金额
 */
function formatQuoteAmount(v: unknown): string {
  const n = typeof v === 'number' ? v : Number(v)
  if (Number.isFinite(n)) return n.toFixed(2)
  const s = String(v ?? '').trim()
  return s || '0.00'
}

/**
 * 分割逗号URL
 * @param raw - 原始URL
 * @returns - 分割后的URL
 */
function splitCommaUrls(raw: unknown): string[] {
  const s = String(raw ?? '').trim()
  if (!s) return []
  return s
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
}

function sortWorkOrderFiles(files: CustomerWorkOrderFileDTO[] | undefined): CustomerWorkOrderFileDTO[] {
  return (files ?? [])
    .slice()
    .sort((a, b) => (Number(a.sortNum) || 0) - (Number(b.sortNum) || 0))
}

function filePreviewUrl(f: CustomerWorkOrderFileDTO | undefined): string {
  if (!f) return ''
  return String(f.previewUrl ?? '').trim()
}

function firstFilePreviewUrl(files: CustomerWorkOrderFileDTO[] | undefined): string {
  const sorted = sortWorkOrderFiles(files)
  for (const f of sorted) {
    const u = filePreviewUrl(f)
    if (u) return u
  }
  return ''
}

function previewUrlsFromFiles(files: CustomerWorkOrderFileDTO[] | undefined): string[] {
  return sortWorkOrderFiles(files).map(filePreviewUrl).filter(Boolean)
}

function mergeUniqueUrls(primary: string[], extra: string[]): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const u of [...primary, ...extra]) {
    const x = String(u ?? '').trim()
    if (!x || seen.has(x)) continue
    seen.add(x)
    out.push(x)
  }
  return out
}

/**
 * 将工单详情转为工单列表项状态
 * @param r - 工单详情
 * @returns - 工单列表项状态
 */
function workOrderDetailToUiStatus(r: CustomerWorkOrderDetailDTO): OrderListItemDTO['status'] {
  const d = r.displayStatus?.trim()
  if (d && (UI_ORDER_STATUSES as readonly string[]).includes(d)) {
    return d as OrderListItemDTO['status']
  }
  const key = String(r.mainStatus || '')
    .trim()
    .toUpperCase()
  if (key && MAIN_STATUS_TO_UI[key]) return MAIN_STATUS_TO_UI[key]
  return '待接单'
}

/**
 * 将客户侧工单详情转为工单详情
 * @param r - 客户侧工单详情
 * @returns - 工单详情
 */
export function mapCustomerWorkOrderDetailToOrderDetailDTO(r: CustomerWorkOrderDetailDTO): OrderDetailDTO {
  const uiStatus = workOrderDetailToUiStatus(r)
  const quote =
    r.quotes?.find((x) => Number(x.isCurrentValid) === 1) ??
    r.quotes?.[0]

  const senderInfoParts = [
    [r.senderName, r.senderMobile].filter(Boolean).join(' '),
    r.senderAddress,
    r.sendExpressNo ? `快递单号：${r.sendExpressNo}` : '',
  ]
    .map((x) => String(x ?? '').trim())
    .filter(Boolean)

  const acceptorName = [r.currentAcceptCompanyName, r.assignedUserName]
    .map((x) => String(x ?? '').trim())
    .filter(Boolean)
    .join(' ')

  const faultImagesFromFiles = previewUrlsFromFiles(r.faultImageFiles)
  const faultImagesFromRepairs = (r.repairs ?? [])
    .flatMap((rep) => (rep.faults ?? []).flatMap((f) => splitCommaUrls(f.imageUrls)))
    .filter(Boolean)
  const faultImages = mergeUniqueUrls(faultImagesFromFiles, faultImagesFromRepairs)

  const videoUrls = previewUrlsFromFiles(r.faultVideoFiles)
  const videoThumb = videoUrls[0] ?? ''
  const videoUrl = videoUrls[0] ?? ''

  const voiceListFromApi = sortWorkOrderFiles(r.faultVoiceFiles)
    .map((f) => ({ url: filePreviewUrl(f) }))
    .filter((x) => x.url)

  const outletPhone = String(r.sitePhone ?? '').trim()
  const returnReceiverTitle = [r.senderName, r.senderMobile].filter(Boolean).join(' ').trim()

  const faultPointRecords: FaultPointMaintenanceRecord[] = (r.repairs ?? [])
    .flatMap((rep) => {
      const repDate = String(rep.finishedTime || rep.createTime || '').trim()
      const repLocation = String(rep.companyName || r.currentAcceptCompanyName || '').trim()
      const faults = rep.faults ?? []
      if (faults.length === 0) {
        return []
      }
      return faults.map((f) => {
        const imgs = splitCommaUrls(f.imageUrls).map((url, idx) => ({
          url,
          label: `图片${idx + 1}`,
        }))
        return {
          description: String(f.repairDesc || f.faultDesc || '').trim(),
          images: imgs,
          specialInfo: String(f.otherDesc || '').trim() || undefined,
          location: repLocation,
          date: String(f.createTime || repDate || '').trim(),
        }
      })
    })
    .filter((x) => x.description || (x.images?.length ?? 0) > 0)

  const repairsSorted = [...(r.repairs ?? [])].sort((a, b) => {
    const ta = Date.parse(String(a.createTime || '')) || 0
    const tb = Date.parse(String(b.createTime || '')) || 0
    if (tb !== ta) return tb - ta
    return (Number(b.id) || 0) - (Number(a.id) || 0)
  })
  const latestRepair = repairsSorted[0]
  let faultPointCurrentDate = ''
  let faultPointCurrentDesc = ''
  if (latestRepair) {
    faultPointCurrentDate = String(latestRepair.finishedTime || latestRepair.createTime || '').trim()
    const faultLines = (latestRepair.faults ?? [])
      .slice()
      .sort((a, b) => (Number(a.sortNum) || 0) - (Number(b.sortNum) || 0))
      .map((f) => String(f.repairDesc || f.faultDesc || '').trim())
      .filter(Boolean)
    faultPointCurrentDesc = faultLines.join('；')
  }

  const orderTypeName = String(
    r.brandTypeLabel || r.brandType || r.productName || r.serviceMode || ''
  ).trim()

  return {
    status: uiStatus,
    canEvaluate: r.canEvaluate ?? (uiStatus === '已关闭'),
    base: {
      orderNo: String(r.orderNo ?? '').trim(),
      orderTypeName,
      submitTime: String(r.createTime ?? '').trim(),
    },
    product: {
      barcode: String(r.barcode ?? '').trim(),
      model: String(r.productModel ?? '').trim(),
      serialNo: String(r.machineNo ?? '').trim(),
      brandName: String(r.brandName ?? '').trim() || undefined,
    },
    service: {
      sitePhone: outletPhone,
      repairMethod: String(r.serviceModeLabel || r.serviceMode || '').trim(),
      serviceModeLabel: String(r.serviceModeLabel ?? '').trim() || undefined,
      senderInfo: senderInfoParts.join('\n'),
      senderVoucherImg: firstFilePreviewUrl(r.senderVoucherFiles) || undefined,
    },
    acceptor: {
      sitePhone: outletPhone,
      acceptorName,
    },
    fault: {
      desc: String(r.faultDesc ?? '').trim(),
      remark: String(r.faultRemark ?? '').trim(),
      voiceDuration: '',
      voiceUrl: '',
      voiceList: voiceListFromApi.length ? voiceListFromApi : undefined,
      images: faultImages,
      videoThumb,
      videoUrl,
    },
    repair: {
      faultJudge: String(quote?.faultJudge ?? '').trim(),
      quoteAmount: formatQuoteAmount(quote?.quoteAmount),
      quoteDesc: String(quote?.quoteDesc ?? '').trim(),
      repairTime: String(r.completedTime || r.closedTime || '').trim(),
      returnMethod: String(r.returnMethod ?? '').trim(),
      returnReceiverTitle,
      returnAddress: String(r.senderAddress ?? '').trim(),
      returnExpressNo: String(r.returnExpressNo ?? '').trim(),
      returnExpressVoucherImg: firstFilePreviewUrl(r.returnVoucherFiles) || '',
    },
    faultPoint: {
      current: { date: faultPointCurrentDate, desc: faultPointCurrentDesc },
      records: faultPointRecords,
    },
    contact: {
      phone: outletPhone,
    },
    evaluate: r.evaluation
      ? {
          timeliness: Number(r.evaluation.timelinessScore ?? 0) || 0,
          quality: Number(r.evaluation.qualityScore ?? 0) || 0,
          satisfaction: Number(r.evaluation.satisfactionScore ?? 0) || 0,
          comment: String(r.evaluation.content ?? '').trim(),
        }
      : undefined,
  }
}
/**
 * 获取工单详情
 * @param data - 工单ID
 * @returns - 工单详情
 */
export const getOrderDetailAPI = (data: { id: string }) => {
  return http<CustomerWorkOrderDetailDTO>({
    url: `/api/customer/work-order/${encodeURIComponent(String(data.id))}`,
    method: 'GET',
  }).then((res) => ({
    ...res,
    result: mapCustomerWorkOrderDetailToOrderDetailDTO(res.result),
  }))
}

/**
 * 获取工单评价元数据
 * @param data - 工单ID
 * @returns - 工单评价元数据
 */
export interface OrderEvaluationMetaDTO {
  technician: {
    name: string
    level?: string
    avatar?: string
  }
  orderId: string
  tags: string[]
}

/**
 * 获取工单评价元数据
 * @param data - 工单ID
 * @returns - 工单评价元数据
 */
export const getOrderEvaluationMetaAPI = (data: { id: string }) => {
  return http<OrderEvaluationMetaDTO>({
    url: '/order/evaluation/meta',
    method: 'POST',
    data,
  })
}

/**
 * 获取工单统计
 * @returns - 工单统计
 */
export interface MyOrderCountsDTO {
  pending: number
  repairing: number
  completed: number
  closed: number
}

/**
 * 获取工单统计
 * @returns - 工单统计
 */
  export const getMyOrderCountsAPI = () => {
  return http<MyOrderCountsDTO>({
    url: '/order/my/counts',
    method: 'GET',
  })
}

/**
 * 获取工单统计
 * @returns - 工单统计
 */
export interface WorkOrderStatusCountDTO {
  allCount: number
  closedCount: number
  completedCount: number
  inProgressCount: number
  waitAcceptCount: number
}

/**
 * 获取工单统计
 * @returns - 工单统计
 */
export const getWorkOrderStatusCountAPI = () => {
  return http<WorkOrderStatusCountDTO>({
    url: '/api/customer/work-order/status-count',
    method: 'GET',
  })
}

/**
 * 获取最新工单
 * @returns - 最新工单
 */
export interface LatestOrderDTO {
  id: string
  description: string
  orderNo: string
  statusText: string
  timelineTitle: string
  timelineSub: string
  status: string
}

/** `/api/customer/work-order/latest-summary` 响应 data */
export interface CustomerWorkOrderLatestSummaryVO {
  brandType?: string
  brandTypeLabel?: string
  createTime?: string
  displayStatus?: string
  faultDesc?: string
  id?: number
  orderNo?: string
  productModel?: string
  productName?: string
  serviceMode?: string
  serviceModeLabel?: string
}

function formatLatestSummaryTime(raw: string | undefined): string {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  return s.replace('T', ' ').replace(/\.\d{3}Z?$/, '').replace(/Z$/, '').trim()
}

/**
 * 将「最近一条工单摘要」转为首页报修进度卡片用结构
 */
export function mapLatestSummaryToLatestOrderDTO(
  vo: CustomerWorkOrderLatestSummaryVO | null | undefined
): LatestOrderDTO {
  const idNum = vo?.id
  if (vo == null || idNum == null || Number(idNum) === 0) {
    return {
      id: '',
      description: '',
      orderNo: '',
      statusText: '',
      timelineTitle: '',
      timelineSub: '',
      status: '',
    }
  }
  const faultDesc = String(vo.faultDesc ?? '').trim()
  const productName = String(vo.productName ?? '').trim()
  const productModel = String(vo.productModel ?? '').trim()
  const description =
    faultDesc || [productName, productModel].filter(Boolean).join(' · ') || '-'
  const displayStatus = String(vo.displayStatus ?? '').trim()
  const timeStr = formatLatestSummaryTime(vo.createTime)
  const modeLabel = String(vo.serviceModeLabel ?? '').trim()
  const timelineSub =
    [timeStr, modeLabel].filter(Boolean).join(' · ') || '-'

  return {
    id: String(idNum),
    description,
    orderNo: String(vo.orderNo ?? '').trim() || '-',
    statusText: displayStatus || '-',
    timelineTitle: displayStatus || '-',
    timelineSub,
    status: displayStatus,
  }
}

/**
 * 查询最近一条工单摘要（首页「报修进度」）
 */
export const getLatestOrderAPI = () => {
  return http<CustomerWorkOrderLatestSummaryVO | null>({
    url: '/api/customer/work-order/latest-summary',
    method: 'GET',
  }).then((res) => ({
    ...res,
    result: mapLatestSummaryToLatestOrderDTO(res.result ?? undefined),
  }))
}

// ========== 工单列表 ==========

/**
 * 工单列表项
 * @returns - 工单列表项
 */
export interface OrderListItemDTO {
  id: string
  status: '待接单' | '维修中' | '已完成' | '已关闭'
  description: string
  time: string
  orderNo: string
  /** 与接口 `barcode` 一致，用于条码展示与搜索 */
  qrCode: string
  isJasic: boolean
  modelName: string
  centerName: string
  phone: string
  repairType: string
  price: string
  /** 是否可评价（来自 `/api/customer/work-order/list` 的 canEvaluate） */
  canEvaluate?: boolean
  /** 是否允许上传寄件凭证（来自 `/api/customer/work-order/list` 的 canUploadSendExpress） */
  canUploadSendExpress?: boolean
  /** ========== 接口 `/api/customer/work-order/list` 原始字段（列表完整赋值） ========== */
  barcode: string
  createTime: string
  closedTime: string
  customerMobile: string
  customerName: string
  assignedUserName: string
  displayStatus: string
  evaluateStatus: string
  evaluateStatusLabel: string
  hasTransfer: number
  mainStatus: string
  productModel: string
}

/**
 * `/api/customer/work-order/list` 单条 `records` 项（与后端字段一致）
 */
export interface WorkOrderListRecordDTO {
  id: number
  orderNo: string
  barcode: string
  productModel: string
  displayStatus: string
  mainStatus: string
  createTime: string
  closedTime: string
  currentAcceptCompanyName: string
  customerMobile: string
  customerName: string
  assignedUserName: string
  canEvaluate?: boolean
  canUploadSendExpress?: boolean
  evaluateStatus: string
  evaluateStatusLabel: string
  hasTransfer: number
  /** 故障描述（若后端扩展返回） */
  faultDesc?: string
  /** 服务/维修方式编码值（若后端扩展返回） */
  serviceMode?: string
  /** 服务/维修方式展示文案（若后端扩展返回） */
  serviceModeLabel?: string
  /** 报价金额展示（若后端扩展返回） */
  quoteAmount?: string
  /** 网点联系电话（若后端扩展返回，优先于 customerMobile 展示为网点电话） */
  sitePhone?: string
  /** 是否佳士产品（若后端扩展返回） */
  isJasicProduct?: boolean
  /** 工单品牌类型展示文案（如：佳士/非佳士） */
  brandTypeLabel?: string
}

/**
 * 工单列表页
 * @returns - 工单列表页
 */
export interface WorkOrderListPageDTO {
  pageNum: number
  pageSize: number
  total: number
  records: WorkOrderListRecordDTO[]
}
// 工单状态
const UI_ORDER_STATUSES: OrderListItemDTO['status'][] = ['待接单', '维修中', '已完成', '已关闭']
// 工单状态映射
const MAIN_STATUS_TO_UI: Record<string, OrderListItemDTO['status']> = {
  PENDING: '待接单',
  PENDING_ASSIGN: '待接单',
  WAIT_ACCEPT: '待接单',
  WAITING: '待接单',
  REPAIRING: '维修中',
  IN_REPAIR: '维修中',
  IN_PROGRESS: '维修中',
  PROCESSING: '维修中',
  COMPLETED: '已完成',
  DONE: '已完成',
  FINISHED: '已完成',
  CLOSED: '已关闭',
  CLOSED_EVAL: '已关闭',
}

/**
 * 将工单列表记录转为工单列表项状态
 * @param r - 工单列表记录
 * @returns - 工单列表项状态
 */
function workOrderRecordToUiStatus(r: WorkOrderListRecordDTO): OrderListItemDTO['status'] {
  const d = r.displayStatus?.trim()
  if (d && UI_ORDER_STATUSES.includes(d as OrderListItemDTO['status'])) {
    return d as OrderListItemDTO['status']
  }
  const key = String(r.mainStatus || '')
    .trim()
    .toUpperCase()
  if (key && MAIN_STATUS_TO_UI[key]) {
    return MAIN_STATUS_TO_UI[key]
  }
  return '待接单'
}

/**
 * 将 `/api/customer/work-order/list` 单条记录转为列表页展示结构（与 `list.vue` 字段对应）
 *
 * - 状态：`displayStatus` 中文优先，否则按 `mainStatus` 映射为 Tab 用状态
 * - 时间：`createTime`，缺省用 `closedTime`
 * - 条码 / 型号：`barcode`、`productModel`
 * - 网点：`currentAcceptCompanyName`；电话：`sitePhone`（若有）否则 `customerMobile`
 * - 维修方式 / 价格：扩展字段 `serviceModeLabel`、`quoteAmount`，无则展示「—」
 * - 故障描述：扩展字段 `faultDesc`，无则空串
 * - 佳士：优先使用 `brandTypeLabel` 判断，缺失时回退 `isJasicProduct`
 */
export function mapWorkOrderListRecordToItem(r: WorkOrderListRecordDTO): OrderListItemDTO {
  const status = workOrderRecordToUiStatus(r)
  const barcode = String(r.barcode ?? '').trim()
  const serviceModeLabel = r.serviceModeLabel?.trim()
  const serviceMode = r.serviceMode?.trim()
  const quoteAmount = r.quoteAmount?.trim()
  const brandTypeLabel = String(r.brandTypeLabel ?? '').trim()
  const isJasicByBrandLabel = brandTypeLabel
    ? !brandTypeLabel.includes('非佳士') && brandTypeLabel.includes('佳士')
    : null
  const canEvaluate =
    r.canEvaluate !== undefined && r.canEvaluate !== null
      ? Boolean(r.canEvaluate)
      : status === '已关闭'
  const canUploadSendExpress = Boolean(r.canUploadSendExpress)

  return {
    id: String(r.id ?? ''),
    status,
    description: (r.faultDesc ?? '').trim(),
    time: String(r.createTime || r.closedTime || '').trim(),
    orderNo: String(r.orderNo ?? '').trim(),
    qrCode: barcode,
    barcode,
    isJasic: isJasicByBrandLabel ?? Boolean(r.isJasicProduct),
    modelName: String(r.productModel ?? '').trim(),
    productModel: String(r.productModel ?? '').trim(),
    centerName: String(r.currentAcceptCompanyName ?? '').trim(),
    phone: String((r.sitePhone ?? r.customerMobile ?? '').trim()),
    repairType: serviceModeLabel || serviceMode || '—',
    price: quoteAmount || '—',
    canEvaluate,
    canUploadSendExpress,
    createTime: String(r.createTime ?? '').trim(),
    closedTime: String(r.closedTime ?? '').trim(),
    customerMobile: String(r.customerMobile ?? '').trim(),
    customerName: String(r.customerName ?? '').trim(),
    assignedUserName: String(r.assignedUserName ?? '').trim(),
    displayStatus: String(r.displayStatus ?? '').trim(),
    evaluateStatus: String(r.evaluateStatus ?? '').trim(),
    evaluateStatusLabel: String(r.evaluateStatusLabel ?? '').trim(),
    hasTransfer: Number(r.hasTransfer ?? 0),
    mainStatus: String(r.mainStatus ?? '').trim(),
  }
}

/**
 * 工单列表查询
 * @returns - 工单列表查询
 */
export interface WorkOrderListQuery {
  /** 排序字段 */
  orderByColumn?: string
  pageNum?: number
  pageSize?: number
  /** 列表 Tab 状态（如：待接单/维修中/已完成/已关闭）；为空表示全部 */
  tabStatus?: string
  /** 是否升序：后端若非必填，可不传 */
  isAsc?: boolean
}

/**
 * 获取工单列表
 * @param data - 工单列表查询
 * @returns - 工单列表
 */
export const getOrderListAPI = (data?: WorkOrderListQuery) => {
  return http<WorkOrderListPageDTO>({
    url: '/api/customer/work-order/list',
    method: 'GET',
    data: {
      orderByColumn: data?.orderByColumn ?? 'createTime',
      pageNum: data?.pageNum ?? 1,
      pageSize: data?.pageSize ?? 500,
      tabStatus: data?.tabStatus,
      ...(data?.isAsc == null ? null : { isAsc: data.isAsc }),
    },
  })
}

// ========== 提交报修 ==========

/**
 * 提交报修
 * @returns - 提交报修
 */
export interface SubmitRepairDTO {
  isJasic: boolean
  warrantyCode?: string
  centerId?: string
  faultDescription: string
  repairType: string
  shippingInfo?: string
  brandName?: string
  modelName?: string
}

/**
 * 提交报修结果
 * @returns - 提交报修结果
 */
export interface SubmitRepairResultDTO {
  orderId: string
  orderNo: string
}

/**
 * 提交报修
 * @param data - 提交报修
 * @returns - 提交报修结果
 */
export const submitRepairAPI = (data: SubmitRepairDTO) => {
  return http<SubmitRepairResultDTO>({
    url: '/repair/submit',
    method: 'POST',
    data,
  })
}

/**
 * 创建工单
 * @returns - 创建工单
 */
export interface CreateCustomerWorkOrderDTO {
  barcode: string
  brandCode: string
  /** 品牌名称（条码查询接口可能返回） */
  brandName?: string
  customerName: string
  faultDesc: string
  /** 故障项编码列表，如与 faultDesc 对应的选项 value */
  faultItems: string[]
  faultRemark: string
  /** 故障图片文件 ID（int64） */
  faultImageFileIds?: number[]
  /** 故障视频文件 ID（int64） */
  faultVideoFileIds?: number[]
  /** 故障语音文件 ID（int64） */
  faultVoiceFileIds?: number[]
  productCode: string
  productModel: string
  /** 报修业务类型：JASIC / NON_JASIC */
  brandType?: string
  /** 寄件快递单号（文本） */
  sendExpressNo: string
  senderAddress: string
  senderMobile: string
  senderName: string
  /** 寄件凭证（面单等）文件 ID（int64） */
  senderVoucherFileIds?: number[]
  serviceCompanyId: number
  /** 服务方式，与前端 repairType 对应，由页面映射为后端约定值 */
  serviceMode: string
  /** 保修状态，建议与后端枚举一致，如 IN_WARRANTY / OUT_OF_WARRANTY */
  warrantyStatus: string
}

/**
 * 创建工单
 * @param data - 创建工单
 * @returns - 创建工单结果
 */
export const createCustomerWorkOrderAPI = (data: CreateCustomerWorkOrderDTO) => {
  return http<SubmitRepairResultDTO>({
    url: '/api/customer/work-order',
    method: 'POST',
    data,
  })
}

// ========== 提交评价 ==========

/**
 * 提交评价
 * @returns - 提交评价
 */
export interface SubmitEvaluationDTO {
  orderId: string
  timeliness: number
  quality: number
  satisfaction: number
  comment?: string
  tags?: string[]
}

/**
 * 提交评价
 * @param data - 提交评价
 * @returns - 提交评价结果
 */
export const submitEvaluationAPI = (data: SubmitEvaluationDTO) => {
  return http<{ success: boolean }>({
    url: '/order/evaluation/submit',
    method: 'POST',
    data,
  })
}

/**
 * 客户侧工单评价
 * @param data - 客户侧工单评价
 * @returns - 客户侧工单评价结果
 */
export interface CustomerWorkOrderEvaluateDTO {
  /** 评价内容，非必填 */
  content?: string
  qualityScore: number
  satisfactionScore: number
  /** 标签，逗号分隔，非必填 */
  tags?: string
  timelinessScore: number
  workOrderId: number
}

/**
 * 客户侧工单评价
 * @param data - 客户侧工单评价
 * @returns - 客户侧工单评价结果
 */
export const evaluateCustomerWorkOrderAPI = (data: CustomerWorkOrderEvaluateDTO) => {
  return http<unknown>({
    url: '/api/customer/work-order/evaluate',
    method: 'POST',
    data,
  })
}

// ========== 上传寄件凭证 ==========

/**
 * C端工单寄件凭证参数
 */
export interface CustomerWorkOrderSenderVoucherDTO {
  /** 寄件凭证文件ID */
  senderVoucherFileIds: number[]
  /** 工单ID */
  workOrderId: number
}

/**
 * 上传寄件凭证
 * @param data - 上传寄件凭证参数
 * @returns - 上传结果
 */
export const uploadLogisticsAPI = (data: CustomerWorkOrderSenderVoucherDTO) => {
  return http<void>({
    url: '/api/customer/work-order/sender-voucher',
    method: 'PUT',
    data,
  })
}

// ========== 商品条码查询（保修信息） ==========

/** uni-data-select 所需的 { text, value } 项 */
export type BarcodeFaultOptionItem = { text: string; value: string }

/**
 * 条码查询结果
 * @returns - 条码查询结果
 */
export interface BarcodeInfoDTO {
  /** 兼容旧字段：布尔在保 */
  inWarranty?: boolean
  expiryDate?: string
  productModel?: string
  productCode?: string
  barcode?: string
  brandCode?: string
  brandName?: string
  /** 保修状态字符串，如 IN_WARRANTY / OUT_OF_WARRANTY */
  warrantyStatus?: string
  /** 条码关联的故障描述（有非空值则前端展示故障描述下拉） */
  faultDescription?: string
  /** 与 faultDescription 二选一，兼容后端字段名 */
  faultDesc?: string
  /** 可选故障项编码列表，非空则展示故障描述下拉 */
  faultItems?: string[]
  /** 故障描述下拉选项（接口主要以此为准） */
  faultOptions?: unknown[]
  otherFaultLabel?: string
}

/**
 * 将接口 faultOptions 转为 uni-data-select 的 localdata
 */
export function mapBarcodeFaultOptions(raw: unknown): BarcodeFaultOptionItem[] {
  if (!Array.isArray(raw) || raw.length === 0) return []
  const out: BarcodeFaultOptionItem[] = []
  for (const item of raw) {
    if (typeof item === 'string') {
      const s = item.trim()
      if (s) out.push({ text: s, value: s })
      continue
    }
    if (item && typeof item === 'object') {
      const o = item as Record<string, unknown>
      const value = String(o.value ?? o.code ?? o.id ?? '').trim()
      const text = String(o.text ?? o.label ?? o.name ?? value).trim()
      if (!value && !text) continue
      out.push({ text: text || value, value: value || text })
    }
  }
  return out
}

/**
 * 条码查询结果是否包含故障描述（有 faultOptions 则展示故障描述下拉）
 * @param info - 条码查询结果
 * @returns - 条码查询结果是否包含故障描述
 */
export function barcodeInfoHasFaultDescription(info: BarcodeInfoDTO): boolean {
  return mapBarcodeFaultOptions(info.faultOptions).length > 0
}

/**
 * 获取条码查询结果
 * @param params - 条码查询结果
 * @returns - 条码查询结果
 */
export const getBarcodeInfoAPI = (params: { barcode: string }) => {
  return http<BarcodeInfoDTO>({
    url: '/api/customer/work-order/barcode-info',
    method: 'GET',
    data: params,
  })
}
