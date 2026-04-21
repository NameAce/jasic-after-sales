import { http } from '@/utils/http'
import type { FaultPointRecord } from '@/models/order'
import {
  mapCustomerRepairsToAllFaultPointRecords,
  type CustomerRepairForHistory,
} from '@/api/mapRepairsToFaultPointRecords'
import { formatAmount, formatIsoDateTime } from '@/utils/format'

export type FaultPointMaintenanceRecord = FaultPointRecord

/**
 * 工单详情（UI 展示模型，Model 层）
 *
 * 三层化口径（对齐 `MIRROR_FILE_PAIRS.md` 与 jasic-ui 契约）：
 * - `VO`：后端原样返回，见 `CustomerWorkOrderDetailVO`（布尔字段保持 number `0/1`）
 * - `DTO`：请求体（见 `CreateCustomerWorkOrderDTO` 等）
 * - `Model`：UI 展示模型（本类型；布尔字段保持 boolean）
 */
export interface OrderDetail {
  status: string
  /** 是否可评价（来自 `/customer/work-order/{id}` 的 canEvaluate） */
  canEvaluate?: boolean
  /**
   * 是否佳士品牌工单（与列表 `isJasic` 规则一致，用于「工单类型」标签配色）
   */
  isJasic: boolean
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
    /** 商品/产品线名称（详情接口 productName，无品牌名时可用于「品牌」展示兜底） */
    productName?: string
    /** 质保判定展示文案，如 保内 / 保外 */
    warrantyClass?: string
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
  /** 指派/维修人员（与详情受理信息同源，供评价页等使用） */
  technician?: {
    name: string
    /** 如受理网点名称 */
    orgLabel?: string
    avatar?: string
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
  /** 故障图片附件（与接口 `faultImageFiles` 一致，详情页优先用于回显） */
  faultImageFiles?: CustomerWorkOrderFileDTO[]
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

/**
 * 客户侧工单详情 VO（后端 `/customer/work-order/{id}` 返回原样）
 *
 * 三层化口径：VO 侧布尔字段（`canEditSendInfo / canEvaluate / isJasicProduct`）一律保持 `number`（0/1），
 * UI 展示侧 Model（`OrderDetail`）再转为 `boolean`。
 */
export interface CustomerWorkOrderDetailVO {
  assignedUserName: string
  barcode: string
  brandCode: string
  /** 品牌名称（接口扩展） */
  brandName?: string
  /** 是否允许编辑寄件信息（后端 0/1；阶段 4.5：DTO 布尔统一为 number） */
  canEditSendInfo: number
  /** 是否可评价（后端 0/1；阶段 4.5：DTO 布尔统一为 number） */
  canEvaluate: number
  closeReason: string
  closedTime: string
  completedTime: string
  createTime: string
  /** 建单公司（用于详情页「受理方」显示） */
  createCompanyName?: string
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
  /** 是否佳士产品线（与列表同源字段，缺省时用 brandType 推断；后端 0/1） */
  isJasicProduct?: number
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
      /** 与 contractor 详情一致：结构化配件（若有则优先于 partDesc） */
      partList?: Array<{ id?: number; partName?: string; partQty?: number; sortNum?: number }>
    }>
    finishedTime: string
    id: number
    isFinished: number
    otherDesc: string
    repairDesc: string
    repairSummary: string
    repairUserId: number
    repairUserName: string
    /** 与 contractor 维修登记附件一致（客户详情若返回则并入历史图） */
    faultOldImageFiles?: CustomerWorkOrderFileDTO[]
    faultNewImageFiles?: CustomerWorkOrderFileDTO[]
    machineImageFiles?: CustomerWorkOrderFileDTO[]
    machineBarcodeImageFiles?: CustomerWorkOrderFileDTO[]
    otherImageFiles?: CustomerWorkOrderFileDTO[]
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

/** 将详情接口 warrantyStatus 转为「质保判定」展示文案 */
function formatWarrantyJudgeLabel(raw: unknown): string {
  const s = String(raw ?? '').trim()
  if (!s) return ''
  if (/保外/.test(s)) return '保外'
  if (/保内/.test(s)) return '保内'
  const u = s.toUpperCase().replace(/-/g, '_')
  if (u.includes('OUT_OF_WARRANTY') || u === 'OUT') return '保外'
  if (u.includes('IN_WARRANTY') || u === 'IN') return '保内'
  return s
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
function workOrderDetailToUiStatus(r: CustomerWorkOrderDetailVO): OrderListItem['status'] {
  const d = r.displayStatus?.trim()
  if (d && (UI_ORDER_STATUSES as readonly string[]).includes(d)) {
    return d as OrderListItem['status']
  }
  const key = String(r.mainStatus || '')
    .trim()
    .toUpperCase()
  if (key && MAIN_STATUS_TO_UI[key]) return MAIN_STATUS_TO_UI[key]
  return '待接单'
}

/**
 * 将客户侧工单详情 VO 转为 UI 展示用的 Model
 *
 * - 入参：`CustomerWorkOrderDetailVO`（后端原样，布尔 0/1）
 * - 返回：`OrderDetail`（UI Model，布尔 boolean）
 *
 * @param r - 客户侧工单详情 VO
 * @returns - 工单详情 Model
 */
export function mapCustomerWorkOrderDetailToOrderDetail(r: CustomerWorkOrderDetailVO): OrderDetail {
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

  const companyNameRaw = String(r.currentAcceptCompanyName ?? '').trim()
  const assignedUserRaw = String(r.assignedUserName ?? '').trim()
  const acceptorName = String(r.createCompanyName ?? '').trim() || companyNameRaw

  const technician: OrderDetail['technician'] =
    assignedUserRaw || companyNameRaw
      ? {
          name: assignedUserRaw || companyNameRaw || acceptorName,
          orgLabel: assignedUserRaw && companyNameRaw ? companyNameRaw : undefined,
          avatar: undefined,
        }
      : undefined

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

  const faultPointRecords: FaultPointMaintenanceRecord[] = mapCustomerRepairsToAllFaultPointRecords(
    r.repairs as CustomerRepairForHistory[] | undefined,
  )

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
    faultPointCurrentDesc =
      String(latestRepair.repairSummary || latestRepair.repairDesc || '').trim() ||
      faultLines.join('；')
  }

  const orderTypeName = String(
    r.brandTypeLabel || r.brandType || r.productName || r.serviceMode || ''
  ).trim()

  const brandTypeLabelForJasic = String(r.brandTypeLabel ?? '').trim()
  const isJasicByBrandLabel = brandTypeLabelForJasic
    ? !brandTypeLabelForJasic.includes('非佳士') && brandTypeLabelForJasic.includes('佳士')
    : null
  const brandTypeCode = String(r.brandType ?? '').trim().toUpperCase().replace(/-/g, '_')
  const isJasic =
    isJasicByBrandLabel !== null
      ? isJasicByBrandLabel
      : brandTypeCode === 'NON_JASIC'
        ? false
        : brandTypeCode === 'JASIC' || brandTypeCode.includes('JASIC')
          ? true
          : Boolean(r.isJasicProduct)

  return {
    status: uiStatus,
    canEvaluate: r.canEvaluate != null ? Boolean(r.canEvaluate) : uiStatus === '已关闭',
    isJasic,
    faultImageFiles: sortWorkOrderFiles(r.faultImageFiles),
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
      productName: String(r.productName ?? '').trim() || undefined,
      warrantyClass: formatWarrantyJudgeLabel(r.warrantyStatus) || undefined,
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
    technician,
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
      quoteAmount: formatAmount(quote?.quoteAmount),
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
 *
 * 对应 jasic-ui `getWorkOrder`（C 端专属接口 `/customer/work-order/{id}`）
 *
 * @param data - 工单ID
 * @returns - 工单详情
 */
export const getCustomerWorkOrder = (data: { id: string }) => {
  return http<CustomerWorkOrderDetailVO>({
    url: `/customer/work-order/${encodeURIComponent(String(data.id))}`,
    method: 'GET',
  }).then((res) => ({
    ...res,
    data: mapCustomerWorkOrderDetailToOrderDetail(res.data),
  }))
}

/**
 * 仅拉取详情中的故障点历史列表（与 `mapCustomerWorkOrderDetailToOrderDetail` 的 `faultPoint.records` 同源；历史页先读 storage 再请求覆盖，与 contractor 一致）。
 */
export async function fetchOrderRepairFaultRecords(id: string): Promise<FaultPointMaintenanceRecord[]> {
  const wid = String(id || '').trim()
  if (!wid) {
    uni.showToast({ title: '工单ID无效', icon: 'none' })
    throw new Error('工单ID无效')
  }
  const res = await getCustomerWorkOrder({ id: wid })
  return res.data?.faultPoint?.records ?? []
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
export const getOrderEvaluationMeta = (data: { id: string }) => {
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
  export const getMyOrderCounts = () => {
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
 *
 * 对应 jasic-ui `countWorkOrderStatus`（C 端专属接口 `/customer/work-order/status-count`）
 *
 * @returns - 工单统计
 */
export const countCustomerWorkOrderStatus = () => {
  return http<WorkOrderStatusCountDTO>({
    url: '/customer/work-order/status-count',
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

/** `/customer/work-order/latest-summary` 响应 data */
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
  const timeStr = formatIsoDateTime(vo.createTime)
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
 *
 * 对应 jasic-ui `getWorkOrderLatestSummary`（C 端专属接口 `/customer/work-order/latest-summary`）
 */
export const getCustomerWorkOrderLatestSummary = () => {
  return http<CustomerWorkOrderLatestSummaryVO | null>({
    url: '/customer/work-order/latest-summary',
    method: 'GET',
  }).then((res) => ({
    ...res,
    data: mapLatestSummaryToLatestOrderDTO(res.data ?? undefined),
  }))
}

// ========== 工单列表 ==========

/**
 * 工单列表项（UI 展示模型，Model 层）
 *
 * 三层化口径：
 * - `VO`：后端原样，见 `CustomerWorkOrderListVO`（布尔字段保持 number `0/1`）
 * - `Model`：UI 展示（本类型；布尔字段保持 boolean）
 *
 * 注：`status` 为中文 UI 显示桶；契约层枚举见 `utils/orderStatus.ts` 的 `WORK_ORDER_MAIN_STATUS`。
 */
export interface OrderListItem {
  id: string
  /** UI 显示桶；契约层枚举见 `utils/orderStatus.ts` 的 `WORK_ORDER_MAIN_STATUS` */
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
  /** 是否可评价（来自 `/customer/work-order/list` 的 canEvaluate） */
  canEvaluate?: boolean
  /** 是否允许上传寄件凭证（来自 `/customer/work-order/list` 的 canUploadSendExpress） */
  canUploadSendExpress?: boolean
  /** ========== 接口 `/customer/work-order/list` 原始字段（列表完整赋值） ========== */
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
  /** 接口 `brandTypeLabel`（如：佳士品牌 / 非佳士品牌） */
  brandTypeLabel: string
}

/**
 * `/customer/work-order/list` 单条 `records` 项 VO（后端原样）
 *
 * 三层化口径：VO 侧布尔字段（`canEvaluate / canUploadSendExpress / isJasicProduct`）一律保持 `number`（0/1），
 * UI 展示 Model（`OrderListItem`）再转为 `boolean`。
 */
export interface CustomerWorkOrderListVO {
  id: number
  orderNo: string
  barcode: string
  productModel: string
  displayStatus: string
  mainStatus: string
  createTime: string
  closedTime: string
  currentAcceptCompanyName: string
  /** 当前受理网点联系电话（列表「网点电话」优先使用） */
  currentAcceptCompanyMobile?: string
  customerMobile: string
  customerName: string
  assignedUserName: string
  /** 是否可评价（后端 0/1；阶段 4.5：DTO 布尔统一为 number） */
  canEvaluate?: number
  /** 是否允许上传寄件凭证（后端 0/1；阶段 4.5：DTO 布尔统一为 number） */
  canUploadSendExpress?: number
  evaluateStatus: string
  evaluateStatusLabel: string
  hasTransfer: number
  /** 故障描述（若后端扩展返回） */
  faultDesc?: string
  /** 服务/维修方式编码值（若后端扩展返回） */
  serviceMode?: string
  /** 服务/维修方式展示文案（若后端扩展返回） */
  serviceModeLabel?: string
  /** 当前有效报价金额（接口为 number，亦兼容字符串） */
  quoteAmount?: number | string
  /** 网点联系电话（若后端扩展返回，优先于 customerMobile 展示为网点电话） */
  sitePhone?: string
  /** 是否佳士产品（若后端扩展返回；后端 0/1） */
  isJasicProduct?: number
  /** 工单品牌类型展示文案（如：佳士/非佳士） */
  brandTypeLabel?: string
  /** 品牌类型编码（如 JASIC / NON_JASIC），与后端 `brandType` 一致 */
  brandType?: string
}

/**
 * 工单列表页
 * @returns - 工单列表页
 */
export interface WorkOrderListPageDTO {
  pageNum: number
  pageSize: number
  total: number
  records: CustomerWorkOrderListVO[]
}
// 工单状态
const UI_ORDER_STATUSES: OrderListItem['status'][] = ['待接单', '维修中', '已完成', '已关闭']
// 工单状态映射
const MAIN_STATUS_TO_UI: Record<string, OrderListItem['status']> = {
  PENDING: '待接单',
  PENDING_ASSIGN: '待接单',
  PENDING_TECH_ACCEPT: '待接单',
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
function workOrderRecordToUiStatus(r: CustomerWorkOrderListVO): OrderListItem['status'] {
  const d = r.displayStatus?.trim()
  if (d && UI_ORDER_STATUSES.includes(d as OrderListItem['status'])) {
    return d as OrderListItem['status']
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
 * 将 `/customer/work-order/list` 单条记录转为列表页展示结构（与 `list.vue` 字段对应）
 *
 * - 状态：`displayStatus` 中文优先，否则按 `mainStatus` 映射为 Tab 用状态
 * - 时间：`createTime`，缺省用 `closedTime`
 * - 条码 / 型号：`barcode`、`productModel`
 * - 网点：`currentAcceptCompanyName`；电话：`currentAcceptCompanyMobile`，缺省再 `sitePhone`、`customerMobile`
 * - 维修方式 / 价格：`serviceModeLabel`、`quoteAmount`（number 会格式化为展示字符串）
 * - 故障描述：扩展字段 `faultDesc`，无则空串
 * - 佳士：优先使用 `brandTypeLabel` 判断，缺失时回退 `isJasicProduct`
 */
function workOrderListQuoteToDisplay(v: unknown): string {
  if (v == null || v === '') return ''
  if (typeof v === 'number') return Number.isFinite(v) ? String(v) : ''
  if (typeof v === 'string') return v.trim()
  return String(v).trim()
}

export function mapWorkOrderListRecordToItem(r: CustomerWorkOrderListVO): OrderListItem {
  const status = workOrderRecordToUiStatus(r)
  const barcode = String(r.barcode ?? '').trim()
  const serviceModeLabel = r.serviceModeLabel?.trim()
  const serviceMode = r.serviceMode?.trim()
  const quoteAmount = workOrderListQuoteToDisplay(r.quoteAmount)
  const brandTypeLabel = String(r.brandTypeLabel ?? '').trim()
  const brandTypeCode = String(r.brandType ?? '')
    .trim()
    .toUpperCase()
  const isJasicByBrandLabel = brandTypeLabel
    ? !brandTypeLabel.includes('非佳士') && brandTypeLabel.includes('佳士')
    : null
  const isJasicByBrandTypeCode =
    brandTypeCode === 'JASIC' ? true : brandTypeCode === 'NON_JASIC' ? false : null
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
    isJasic: isJasicByBrandLabel ?? isJasicByBrandTypeCode ?? Boolean(r.isJasicProduct),
    brandTypeLabel,
    modelName: String(r.productModel ?? '').trim(),
    productModel: String(r.productModel ?? '').trim(),
    centerName: String(r.currentAcceptCompanyName ?? '').trim(),
    phone: String(
      (r.currentAcceptCompanyMobile ?? r.sitePhone ?? r.customerMobile ?? '').trim()
    ),
    repairType: serviceModeLabel || serviceMode || '',
    price: quoteAmount,
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
 *
 * 对应 jasic-ui `listWorkOrder`（C 端专属接口 `/customer/work-order/list`）
 *
 * @param data - 工单列表查询
 * @returns - 工单列表
 */
export const listCustomerWorkOrder = (data?: WorkOrderListQuery) => {
  const tabStatus = String(data?.tabStatus ?? '').trim()
  return http<WorkOrderListPageDTO>({
    url: '/customer/work-order/list',
    method: 'GET',
    data: {
      orderByColumn: data?.orderByColumn ?? 'createTime',
      pageNum: data?.pageNum ?? 1,
      pageSize: data?.pageSize ?? 500,
      ...(tabStatus ? { tabStatus } : {}),
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
export const submitRepair = (data: SubmitRepairDTO) => {
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
 *
 * 对应 jasic-ui `createWorkOrder`（C 端专属接口 `POST /customer/work-order`）
 *
 * @param data - 创建工单
 * @returns - 创建工单结果
 */
export const createCustomerWorkOrder = (data: CreateCustomerWorkOrderDTO) => {
  return http<SubmitRepairResultDTO>({
    url: '/customer/work-order',
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
export const submitEvaluation = (data: SubmitEvaluationDTO) => {
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
 *
 * 对应 jasic-ui `evaluateWorkOrder`（C 端专属接口 `POST /customer/work-order/evaluate`）
 *
 * @param data - 客户侧工单评价
 * @returns - 客户侧工单评价结果
 */
export const evaluateCustomerWorkOrder = (data: CustomerWorkOrderEvaluateDTO) => {
  return http<unknown>({
    url: '/customer/work-order/evaluate',
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
 *
 * 对应 jasic-ui `updateWorkOrderSenderVoucher`（C 端专属接口 `PUT /customer/work-order/sender-voucher`）
 *
 * @param data - 上传寄件凭证参数
 * @returns - 上传结果
 */
export const updateCustomerWorkOrderSenderVoucher = (data: CustomerWorkOrderSenderVoucherDTO) => {
  return http<void>({
    url: '/customer/work-order/sender-voucher',
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
 *
 * 对应 jasic-ui `getWorkOrderBarcodeInfo`（C 端专属接口 `/customer/work-order/barcode-info`）
 *
 * @param params - 条码查询结果
 * @returns - 条码查询结果
 */
export const getCustomerWorkOrderBarcodeInfo = (params: { barcode: string }) => {
  return http<BarcodeInfoDTO>({
    url: '/customer/work-order/barcode-info',
    method: 'GET',
    data: params,
  })
}

// ========== 不带 Customer 前缀的 re-export 桥 ==========
//
// 便于三端 grep 按 jasic-ui 契约层函数名（`getWorkOrder / listWorkOrder / ...`）
// 定位 C 端实现。原 `Customer*` 函数名保留不改（C 端 `/api/customer/*` 命名空间白名单），
// 此处仅追加 alias re-export，不改变原函数签名与运行时语义。
export {
  getCustomerWorkOrder as getWorkOrder,
  listCustomerWorkOrder as listWorkOrder,
  countCustomerWorkOrderStatus as countWorkOrderStatus,
  createCustomerWorkOrder as createWorkOrder,
  evaluateCustomerWorkOrder as evaluateWorkOrder,
  getCustomerWorkOrderBarcodeInfo as getWorkOrderBarcodeInfo,
  getCustomerWorkOrderLatestSummary as getWorkOrderLatestSummary,
  updateCustomerWorkOrderSenderVoucher as updateWorkOrderSenderVoucher,
}

