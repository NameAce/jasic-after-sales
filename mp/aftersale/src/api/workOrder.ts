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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface OrderDetail {
  status: string
  /**
 * 主状态编码（PENDING_ASSIGN / …），与后端 `mainStatus` 对齐
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  mainStatus?: string
  /**
 * 是否可评价（来自 `/customer/work-order/{id}` 的 canEvaluate）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canEvaluate?: boolean
  /**
   * 是否佳士品牌工单（与列表 `isJasic` 规则一致，用于「工单类型」标签配色）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  isJasic: boolean
  /**
 * 客户姓名（与详情接口 `customerName` 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  customerName?: string
  /**
 * 客户手机号（与详情接口 `customerMobile` 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  customerMobile?: string
  base: {
    orderNo: string
    orderTypeName: string
    submitTime: string
  }
  product: {
    barcode: string
    model: string
    serialNo: string
    /**
 * 最后出库日期（同源 `CustomerWorkOrderDetailVO.lastOutDate` 等，映射层兼容别名）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    lastOutDate?: string
    /**
 * 非佳士报修时填写的品牌名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    brandName?: string
    /**
 * 商品/产品线名称（详情接口 productName，无品牌名时可用于「品牌」展示兜底）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    productName?: string
    /**
 * 质保判定展示文案，如 保内 / 保外
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    warrantyClass?: string
  }
  service: {
    sitePhone: string
    repairMethod: string
    /**
 * 维修方式展示文案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    serviceModeLabel?: string
    /**
 * 申请来源展示（与接口 `applicationSourceName` 等同源）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    applySourceLabel?: string
    /**
 * 受理方：当前受理网点名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    acceptingParty?: string
    senderInfo: string
    senderVoucherImg?: string
  }
  acceptor: {
    sitePhone: string
    acceptorName: string
    /**
 * 与详情接口 `currentAcceptCompanyPhone` 一致：当前受理网点电话
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    currentAcceptCompanyPhone?: string
  }
  /**
 * 指派/维修人员（与详情受理信息同源，供评价页等使用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  technician?: {
    name: string
    /**
 * 如受理网点名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    orgLabel?: string
    avatar?: string
  }
  /**
 * 与后端 `faultDesc` 同源，故障描述原文
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultDesc: string
  fault: {
    desc: string
    remark?: string
    /**
 * 展示用时长文案，如 0:32 或与录音页一致的秒数
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    voiceDuration?: string
    /**
 * 单条语音地址（与报修页上传后返回的 URL 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    voiceUrl?: string
    /**
 * 多条语音（与报修页 voiceList 一致，duration 为毫秒）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    voiceList?: { url: string; duration?: number }[]
    images: string[]
    videoThumb?: string
    /**
 * 故障视频可播放地址
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
    /**
 * 故障点维修历史（与历史记录页列表项一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    records?: FaultPointMaintenanceRecord[]
  }
  evaluate?: {
    timeliness: number
    quality: number
    satisfaction: number
    comment?: string
  }
  /**
 * 故障图片附件（与接口 `faultImageFiles` 一致，详情页优先用于回显）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultImageFiles?: CustomerWorkOrderFileDTO[]
}

// ========== 工单详情（新接口） ==========

/**
 * 工单附件（故障图/视频/语音、寄回执等），兼容 fileId / fileID 等字段名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
 * 客户侧工单详情 VO
 *
 * 真源：后端 `jasic-customer/.../CustomerWorkOrderDetailVO.java`，本接口字段一一对齐。
 *
 * 口径：
 * - 后端 `canEvaluate / canEditSendInfo` 为 `Boolean`，此处直接以 `boolean` 接收
 *   （JSON 原生 `true/false`），在映射到 UI Model 时不再需要 `Boolean(0/1)` 转换。
 * - 所有 `FileDTO` 字段指向后端 `SysFileItemVO`，保持与列表 `faultImageFiles` 等形同。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CustomerWorkOrderDetailVO {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  customerMobile: string
  barcode: string
  productCode: string
  productName: string
  productModel: string
  machineNo: string
  /**
 * 最后出库日期（条码档案等来源，ISO 时间）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  lastOutDate?: string
  brandCode: string
  brandName?: string
  /**
 * 报修业务类型编码：JASIC / NON_JASIC
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandType?: string
  /**
 * 报修业务类型名称，对应详情页「工单类型」
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandTypeLabel?: string
  /**
 * 服务方式编码：MAIL / STORE
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceMode: string
  /**
 * 服务方式名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceModeLabel?: string
  /**
 * 质保状态：IN_WARRANTY / OUT_OF_WARRANTY
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  warrantyStatus: string
  faultDesc: string
  faultRemark: string
  faultImageFiles?: CustomerWorkOrderFileDTO[]
  faultVideoFiles?: CustomerWorkOrderFileDTO[]
  faultVoiceFiles?: CustomerWorkOrderFileDTO[]
  senderName: string
  senderMobile: string
  senderAddress: string
  sendExpressNo: string
  senderVoucherFiles?: CustomerWorkOrderFileDTO[]
  /**
 * 主状态编码：PENDING_ASSIGN / PENDING_TECH_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  mainStatus: string
  /**
 * 展示状态名称（后端已本地化）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  displayStatus: string
  /**
 * 评价状态编码：NOT_OPEN / PENDING_EVALUATE / EVALUATED
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  evaluateStatus: string
  evaluateStatusLabel: string
  /**
 * 当前受理网点名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  currentAcceptCompanyName: string
  /**
 * 当前受理网点电话
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  currentAcceptCompanyPhone?: string
  /**
 * 当前维修员姓名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  assignedUserName: string
  hqCompanyId: number
  /**
 * 回寄方式（如 `回寄 / 自提`）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnMethod: string
  /**
 * 回寄快递单号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  returnExpressNo: string
  returnVoucherFiles?: CustomerWorkOrderFileDTO[]
  closeReason: string
  /**
 * 是否允许评价
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canEvaluate?: boolean
  /**
 * 是否允许修改寄件信息
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canEditSendInfo?: boolean
  completedTime: string
  closedTime: string
  createTime: string
  /**
 * 申请来源（后端展示名，优先于字典推断）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  applicationSourceName?: string
  /**
 * 报修业务类型名称，用于申请来源兜底
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  reportBizTypeLabel?: string
  /**
 * 建单入口：PROXY_SELF / UPSTREAM_FIRST / UPSTREAM_HQ 等
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  createEntryType?: string
  /**
 * 建单来源公司名，申请来源末级兜底
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  createCompanyName?: string
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
  /**
   * 维修登记列表
   *
   * 真源：`jasic-system/.../WorkOrderRepairVO.java`。
   * 注意：后端 `repair` 级只有 `registerStage / registerStageLabel / isFinished / finishedTime`，
   * 维修说明都在 `faults[*].repairDesc`，前端映射时以 `faults` 聚合为维修摘要。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  repairs?: Array<{
    id: number
    companyId: number
    companyName: string
    repairUserId: number
    repairUserName: string
    /**
 * 登记阶段：REPAIR / RECHECK
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    registerStage?: string
    /**
 * 登记阶段名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    registerStageLabel?: string
    /**
 * 是否维修完成（后端 Integer 0/1）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    isFinished: number
    finishedTime: string
    createTime: string
    faults?: Array<{
      id: number
      companyId: number
      /**
 * 故障描述
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      faultDesc: string
      /**
 * 其它故障说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      faultRemark?: string
      /**
 * 维修说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      repairDesc: string
      /**
 * 其他维修说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      otherDesc: string
      /**
 * 结构化配件明细
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      partList?: Array<{ id?: number; partName?: string; partQty?: number; sortNum?: number }>
      sortNum: number
      createdBy?: number
      createdByName?: string
      createTime?: string
    }>
    /**
 * 维修登记附件（与 contractor 同源）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
    faultOldImageFiles?: CustomerWorkOrderFileDTO[]
    faultNewImageFiles?: CustomerWorkOrderFileDTO[]
    machineImageFiles?: CustomerWorkOrderFileDTO[]
    machineBarcodeImageFiles?: CustomerWorkOrderFileDTO[]
    otherImageFiles?: CustomerWorkOrderFileDTO[]
  }>
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
}

/**
 * 将详情接口 warrantyStatus 转为「质保判定」展示文案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
 * 作用：接口封装：sortWorkOrderFiles。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function sortWorkOrderFiles(files: CustomerWorkOrderFileDTO[] | undefined): CustomerWorkOrderFileDTO[] {
  return (files ?? [])
    .slice()
    .sort((a, b) => (Number(a.sortNum) || 0) - (Number(b.sortNum) || 0))
}

/**
 * 作用：接口封装：filePreviewUrl。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function filePreviewUrl(f: CustomerWorkOrderFileDTO | undefined): string {
  if (!f) return ''
  return String(f.previewUrl ?? '').trim()
}

/**
 * 作用：接口封装：firstFilePreviewUrl。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function firstFilePreviewUrl(files: CustomerWorkOrderFileDTO[] | undefined): string {
  const sorted = sortWorkOrderFiles(files)
  for (const f of sorted) {
    const u = filePreviewUrl(f)
    if (u) return u
  }
  return ''
}

/**
 * 作用：接口封装：previewUrlsFromFiles。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function previewUrlsFromFiles(files: CustomerWorkOrderFileDTO[] | undefined): string[] {
  return sortWorkOrderFiles(files).map(filePreviewUrl).filter(Boolean)
}

/**
 * 作用：转换/构造：normalizeMainStatusKey。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizeMainStatusKey(raw: unknown): string {
  return String(raw ?? '')
    .trim()
    .toUpperCase()
    .replace(/-/g, '_')
}

/**
 * 将工单详情转为工单列表项状态
 * @param r - 工单详情
 * @returns - 工单列表项状态
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 申请来源：与施工端 `mapOrderTypeNameFromDetail` 一致，优先 `applicationSourceName`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mapApplySourceLabelFromDetail(r: CustomerWorkOrderDetailVO): string {
  const fromApi = String(r.applicationSourceName ?? '').trim()
  if (fromApi) return fromApi
  const label = String(r.reportBizTypeLabel ?? '').trim()
  if (label) return label
  const code = String(r.createEntryType ?? '')
    .trim()
    .toUpperCase()
    .replace(/-/g, '_')
  const map: Record<string, string> = {
    PROXY_SELF: '代客填写',
    UPSTREAM_FIRST: '二级报修',
    UPSTREAM_HQ: '一级报修',
  }
  if (code && map[code]) return map[code]
  if (code) return code
  return String(r.createCompanyName ?? '').trim()
}

/**
 * 详情 VO 上可能出现的「最后出库日期」字段（与施工端 `pickWorkOrderOutDateFromRecord` 口径一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickCustomerWorkOrderOutDateFromRecord(rec: Record<string, unknown>): string {
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
    if (t) {
      const formatted = formatIsoDateTime(t)
      return formatted || t
    }
  }
  return ''
}

/**
 * 将客户侧工单详情 VO 转为 UI 展示用的 Model
 *
 * - 入参：`CustomerWorkOrderDetailVO`（后端原样，布尔 0/1）
 * - 返回：`OrderDetail`（UI Model，布尔 boolean）
 *
 * @param r - 客户侧工单详情 VO
 * @returns - 工单详情 Model
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
  const acceptorName = companyNameRaw

  const technician: OrderDetail['technician'] =
    assignedUserRaw || companyNameRaw
      ? {
          name: assignedUserRaw || companyNameRaw || acceptorName,
          orgLabel: assignedUserRaw && companyNameRaw ? companyNameRaw : undefined,
          avatar: undefined,
        }
      : undefined

  // 故障图片以客户提交的 `faultImageFiles` 为准；维修登记的 `faultOldImageFiles /
  // faultNewImageFiles / ...` 在 `faultPoint.records` 里按故障点维修历史展示，不合并到此处。
  const faultImages = previewUrlsFromFiles(r.faultImageFiles)

  const videoUrls = previewUrlsFromFiles(r.faultVideoFiles)
  const videoThumb = videoUrls[0] ?? ''
  const videoUrl = videoUrls[0] ?? ''

  const voiceListFromApi = sortWorkOrderFiles(r.faultVoiceFiles)
    .map((f) => ({ url: filePreviewUrl(f) }))
    .filter((x) => x.url)

  const outletPhone = String(r.currentAcceptCompanyPhone ?? '').trim()
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
    // 后端 `WorkOrderRepairVO` 无 `repairSummary / repairDesc` 字段，维修说明聚合取自
    // `faults[*].repairDesc`（与后端 `WorkOrderFaultVO` 一致）。
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

  // 判定口径与列表一致：后端 `brandType` 为权威枚举（JASIC / NON_JASIC），
  // `brandTypeLabel` 为名称兜底（用于后端新增非标枚举时的保守展示）。
  const brandTypeLabelForJasic = String(r.brandTypeLabel ?? '').trim()
  const isJasicByBrandLabel = brandTypeLabelForJasic
    ? !brandTypeLabelForJasic.includes('非佳士') && brandTypeLabelForJasic.includes('佳士')
    : null
  const brandTypeCode = String(r.brandType ?? '').trim().toUpperCase().replace(/-/g, '_')
  const isJasic =
    brandTypeCode === 'JASIC'
      ? true
      : brandTypeCode === 'NON_JASIC'
        ? false
        : isJasicByBrandLabel ?? false

  const faultDescRaw = String(r.faultDesc ?? '').trim()

  return {
    status: uiStatus,
    mainStatus: normalizeMainStatusKey(r.mainStatus),
    canEvaluate: Boolean(r.canEvaluate),
    isJasic,
    faultDesc: faultDescRaw,
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
      lastOutDate: (() => {
        const s = pickCustomerWorkOrderOutDateFromRecord(r as unknown as Record<string, unknown>).trim()
        return s || undefined
      })(),
      brandName: String(r.brandName ?? '').trim() || undefined,
      productName: String(r.productName ?? '').trim() || undefined,
      warrantyClass: formatWarrantyJudgeLabel(r.warrantyStatus) || undefined,
    },
    service: {
      sitePhone: outletPhone,
      repairMethod: String(r.serviceModeLabel || r.serviceMode || '').trim(),
      serviceModeLabel: String(r.serviceModeLabel ?? '').trim() || undefined,
      applySourceLabel: mapApplySourceLabelFromDetail(r) || undefined,
      acceptingParty: companyNameRaw || undefined,
      senderInfo: senderInfoParts.join('\n'),
      senderVoucherImg: firstFilePreviewUrl(r.senderVoucherFiles) || undefined,
    },
    acceptor: {
      sitePhone: outletPhone,
      acceptorName,
      currentAcceptCompanyPhone: outletPhone || undefined,
    },
    technician,
    fault: {
      desc: faultDescRaw,
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
    customerName: String(r.customerName ?? '').trim() || undefined,
    customerMobile: String(r.customerMobile ?? '').trim() || undefined,
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 拉取网点电话（列表接口无该字段时用于补齐；与详情 `acceptor.currentAcceptCompanyPhone` / `service.sitePhone` 同源）。
 *
 * @param workOrderId - 工单 id
 * @returns 号码文案，失败或为空时返回 `''`（不抛错，避免列表批量补齐时刷屏）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export async function fetchCustomerWorkOrderOutletPhone(workOrderId: string): Promise<string> {
  const wid = String(workOrderId ?? '').trim()
  if (!wid) return ''
  try {
    const res = await getCustomerWorkOrder({ id: wid })
    const d = res.data
    return String(
      d?.acceptor?.currentAcceptCompanyPhone ?? d?.service?.sitePhone ?? d?.acceptor?.sitePhone ?? ''
    ).trim()
  } catch {
    return ''
  }
}

/**
 * 仅拉取详情中的故障点历史列表（与 `mapCustomerWorkOrderDetailToOrderDetail` 的 `faultPoint.records` 同源；历史页先读 storage 再请求覆盖，与 contractor 一致）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 「我的」页四区块工单统计 UI 模型（对 `countCustomerWorkOrderStatus` 返回值做语义映射）
 *
 * 与后端 `WorkOrderStatusCountDTO` 区别：
 *   - 本 DTO 为 UI 侧四区块（待处理/维修中/已完成/已关闭）的聚合展示字段
 *   - 具体映射规则见 `pages/my/index.vue` `loadCounts()`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface MyOrderCountsDTO {
  pending: number
  repairing: number
  completed: number
  closed: number
}

/**
 * 工单状态计数（后端返回）
 * @returns - 工单状态计数
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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

/**
 * `/customer/work-order/latest-summary` 响应 data
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface OrderListItem {
  id: string
  /**
 * UI 显示桶；契约层枚举见 `utils/orderStatus.ts` 的 `WORK_ORDER_MAIN_STATUS`
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  status: '待接单' | '维修中' | '已完成' | '已关闭'
  description: string
  time: string
  orderNo: string
  /**
 * 与接口 `barcode` 一致，用于条码展示与搜索
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  qrCode: string
  isJasic: boolean
  modelName: string
  centerName: string
  phone: string
  repairType: string
  price: string
  /**
 * 是否可评价（来自 `/customer/work-order/list` 的 canEvaluate）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canEvaluate?: boolean
  /**
 * 是否允许上传寄件凭证（来自 `/customer/work-order/list` 的 canUploadSendExpress）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canUploadSendExpress?: boolean
  /**
 * ========== 接口 `/customer/work-order/list` 原始字段（列表完整赋值） ==========
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
  /**
 * 接口 `brandTypeLabel`（如：佳士品牌 / 非佳士品牌）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandTypeLabel: string
}

/**
 * `/customer/work-order/list` 单条 `records` 项 VO
 *
 * 真源：后端 `jasic-customer/.../CustomerWorkOrderListVO.java`，本接口字段严格一一对齐。
 *
 * 口径：
 * - `canEvaluate / canUploadSendExpress` 后端为 `Boolean`，此处以 `boolean` 接收。
 * - 列表 VO 不含网点电话（与 `CustomerWorkOrderListVO.java` 一致）；列表页展示号码由小程序按需请求详情补齐。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CustomerWorkOrderListVO {
  id: number
  orderNo: string
  customerName: string
  customerMobile: string
  barcode: string
  productModel: string
  /**
 * 品牌类型编码：JASIC / NON_JASIC
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandType?: string
  /**
 * 品牌类型名称（如：佳士品牌 / 非佳士品牌）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandTypeLabel?: string
  /**
 * 服务方式编码：MAIL / STORE
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceMode?: string
  /**
 * 服务方式名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceModeLabel?: string
  /**
 * 主状态编码：PENDING_ASSIGN / PENDING_TECH_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  mainStatus: string
  /**
 * 展示状态名称（后端已本地化）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  displayStatus: string
  /**
 * 评价状态编码：NOT_OPEN / PENDING_EVALUATE / EVALUATED
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  evaluateStatus: string
  evaluateStatusLabel: string
  /**
 * 当前受理网点名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  currentAcceptCompanyName: string
  /**
 * 当前维修员姓名
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  assignedUserName: string
  /**
 * 是否发生过转单（后端为 Integer 0/1）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  hasTransfer: number
  /**
 * 是否允许评价
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canEvaluate?: boolean
  /**
 * 是否允许上传寄件凭证
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  canUploadSendExpress?: boolean
  /**
 * 当前有效报价金额（BigDecimal，前端兼容 number / string）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  quoteAmount?: number | string
  createTime: string
  closedTime: string
}

/**
 * 工单列表页
 * @returns - 工单列表页
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface WorkOrderListPageDTO {
  pageNum: number
  pageSize: number
  total: number
  records: CustomerWorkOrderListVO[]
}
// 工单状态
const UI_ORDER_STATUSES: OrderListItem['status'][] = ['待接单', '维修中', '已完成', '已关闭']
/**
 * 主状态 → Tab 用中文状态映射
 *
 * 真源：`jasic-common/.../WorkOrderStatusConstants.java`
 *   - `MainStatus`   : `PENDING_ASSIGN / PENDING_TECH_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED`
 *   - `DisplayStatus`: `WAIT_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED`
 *     （`WAIT_ACCEPT` 为 `PENDING_ASSIGN + PENDING_TECH_ACCEPT` 的聚合展示态，后端正式字段）
 *
 * 本 map 键覆盖：主状态枚举 + `WAIT_ACCEPT` 展示态，不再兜底前端自造别名
 * （`PENDING / WAITING / REPAIRING / IN_REPAIR / PROCESSING / DONE / FINISHED / CLOSED_EVAL`
 * 已于契约统一阶段回收）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
const MAIN_STATUS_TO_UI: Record<string, OrderListItem['status']> = {
  PENDING_ASSIGN: '待接单',
  PENDING_TECH_ACCEPT: '待接单',
  WAIT_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
}

/**
 * 将工单列表记录转为工单列表项状态
 * @param r - 工单列表记录
 * @returns - 工单列表项状态
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * - 网点：`currentAcceptCompanyName` → `centerName`；`phone` 预留网点电话（列表 VO 无网点电话时多为空，可由详情补齐）
 * - 客户：`customerMobile` / `customerName` 与接口一致
 * - 维修方式 / 价格：`serviceModeLabel`、`quoteAmount`（number 会格式化为展示字符串）
 * - 故障描述：列表 VO 不返回，`description` 置空，依赖详情页补齐
 * - 佳士：以 `brandType` 为准，缺失时退化用 `brandTypeLabel` 兜底
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function workOrderListQuoteToDisplay(v: unknown): string {
  if (v == null || v === '') return ''
  if (typeof v === 'number') return Number.isFinite(v) ? String(v) : ''
  if (typeof v === 'string') return v.trim()
  return String(v).trim()
}

/**
 * 作用：转换/构造：mapWorkOrderListRecordToItem。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
  const isJasic =
    brandTypeCode === 'JASIC'
      ? true
      : brandTypeCode === 'NON_JASIC'
        ? false
        : isJasicByBrandLabel ?? false
  const canEvaluate =
    r.canEvaluate !== undefined && r.canEvaluate !== null
      ? Boolean(r.canEvaluate)
      : status === '已关闭'
  const canUploadSendExpress = Boolean(r.canUploadSendExpress)

  return {
    id: String(r.id ?? ''),
    status,
    description: '',
    time: String(r.createTime || r.closedTime || '').trim(),
    orderNo: String(r.orderNo ?? '').trim(),
    qrCode: barcode,
    barcode,
    isJasic,
    brandTypeLabel,
    modelName: String(r.productModel ?? '').trim(),
    productModel: String(r.productModel ?? '').trim(),
    centerName: String(r.currentAcceptCompanyName ?? '').trim(),
    phone: '',
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
 *
 * 与后端 `CustomerWorkOrderQuery` 对齐：筛选只走 `tabStatus`
 * （`WAIT_ACCEPT / IN_PROGRESS / COMPLETED / CLOSED`），分页沿用 `PageQuery`
 * （`pageNum / pageSize`）。
 *
 * 注：后端 `CustomerWorkOrderServiceImpl#listPage` 已写死 `orderByDesc(createTime)`，
 * 前端不再上送 `orderByColumn / isAsc`，避免误以为可排序。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface WorkOrderListQuery {
  pageNum?: number
  pageSize?: number
  /**
 * 页签状态：WAIT_ACCEPT | IN_PROGRESS | COMPLETED | CLOSED
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  tabStatus?: string
}

/**
 * 获取工单列表
 *
 * 对应 jasic-ui `listWorkOrder`（C 端专属接口 `/customer/work-order/list`）
 *
 * @param data - 工单列表查询
 * @returns - 工单列表
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const listCustomerWorkOrder = (data?: WorkOrderListQuery) => {
  const tabStatus = String(data?.tabStatus ?? '').trim()
  return http<WorkOrderListPageDTO>({
    url: '/customer/work-order/list',
    method: 'GET',
    data: {
      pageNum: data?.pageNum ?? 1,
      pageSize: data?.pageSize ?? 10,
      ...(tabStatus ? { tabStatus } : {}),
    },
  })
}

// ========== 创建工单（提交报修） ==========

/**
 * 创建工单
 * @returns - 创建工单
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CreateCustomerWorkOrderDTO {
  barcode: string
  brandCode: string
  /**
 * 品牌名称（条码查询接口可能返回）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandName?: string
  customerName: string
  faultDesc: string
  /**
 * 故障项编码列表，如与 faultDesc 对应的选项 value
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultItems: string[]
  faultRemark: string
  /**
 * 故障图片文件 ID（int64）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultImageFileIds?: number[]
  /**
 * 故障视频文件 ID（int64）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultVideoFileIds?: number[]
  /**
 * 故障语音文件 ID（int64）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultVoiceFileIds?: number[]
  productCode: string
  productModel: string
  /**
 * 报修业务类型：JASIC / NON_JASIC
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  brandType?: string
  /**
 * 寄件快递单号（文本）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  sendExpressNo: string
  senderAddress: string
  senderMobile: string
  senderName: string
  /**
 * 寄件凭证（面单等）文件 ID（int64）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderVoucherFileIds?: number[]
  serviceCompanyId: number
  /**
 * 服务方式，与前端 repairType 对应，由页面映射为后端约定值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  serviceMode: string
  /**
 * 保修状态，建议与后端枚举一致，如 IN_WARRANTY / OUT_OF_WARRANTY
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  warrantyStatus: string
}

/**
 * 创建工单
 *
 * 对应 jasic-ui `createWorkOrder`（C 端专属接口 `POST /customer/work-order`）。
 * 真源：后端 `CustomerWorkOrderController#create` 返回 `Result<Long>`，
 * 即 `res.data` 为新创建工单 ID（number），如需订单号请再走详情接口。
 *
 * @param data - 创建工单
 * @returns - 创建工单结果（`data` 为工单 ID）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const createCustomerWorkOrder = (data: CreateCustomerWorkOrderDTO) => {
  return http<number>({
    url: '/customer/work-order',
    method: 'POST',
    data,
  })
}

// ========== 提交评价 ==========

/**
 * 客户侧工单评价
 * @param data - 客户侧工单评价
 * @returns - 客户侧工单评价结果
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CustomerWorkOrderEvaluateDTO {
  /**
 * 评价内容，非必填
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  content?: string
  qualityScore: number
  satisfactionScore: number
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface CustomerWorkOrderSenderVoucherDTO {
  /**
 * 寄件凭证文件ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  senderVoucherFileIds: number[]
  /**
 * 工单ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  workOrderId: number
}

/**
 * 上传寄件凭证
 *
 * 对应 jasic-ui `updateWorkOrderSenderVoucher`（C 端专属接口 `PUT /customer/work-order/sender-voucher`）
 *
 * @param data - 上传寄件凭证参数
 * @returns - 上传结果
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const updateCustomerWorkOrderSenderVoucher = (data: CustomerWorkOrderSenderVoucherDTO) => {
  return http<void>({
    url: '/customer/work-order/sender-voucher',
    method: 'PUT',
    data,
  })
}

// ========== 商品条码查询（保修信息） ==========

/**
 * uni-data-select 所需的 { text, value } 项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type BarcodeFaultOptionItem = { text: string; value: string }

/**
 * 条码查询结果
 *
 * 真源：后端 `jasic-customer/.../CustomerBarcodeInfoVO.java`，本接口字段一一对齐。
 * （已下线历史遗留的 `inWarranty / expiryDate / faultDescription / faultDesc /
 * faultItems / brandName` 兼容字段，统一以 `warrantyStatus` 字符串枚举 + `faultOptions`
 * 下拉为准。）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export interface BarcodeInfoDTO {
  barcode?: string
  productCode?: string
  productName?: string
  productModel?: string
  /**
 * 机器小号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  machineNo?: string
  brandCode?: string
  /**
 * 保修状态字符串：IN_WARRANTY / OUT_OF_WARRANTY
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  warrantyStatus?: string
  /**
 * 归属总部 ID
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  hqCompanyId?: number
  /**
 * 归属总部名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  hqCompanyName?: string
  /**
 * 故障描述下拉选项（有值时页面展示故障描述下拉）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  faultOptions?: unknown[]
  /**
 * 其它故障文案
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  otherFaultLabel?: string
}

/**
 * 故障描述：接口仅返回「其它」「其他」时，展示与 faultDesc 拼接统一为「其它故障」「其他故障」
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizeFaultDescDisplayText(raw: string): string {
  const t = String(raw ?? '').trim()
  if (t === '其它') return '其它故障'
  if (t === '其他') return '其他故障'
  return t
}

/**
 * 将接口 faultOptions 转为 uni-data-select 的 localdata
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function mapBarcodeFaultOptions(raw: unknown): BarcodeFaultOptionItem[] {
  if (!Array.isArray(raw) || raw.length === 0) return []
  const out: BarcodeFaultOptionItem[] = []
  for (const item of raw) {
    if (typeof item === 'string') {
      const s = item.trim()
      if (s) out.push({ text: normalizeFaultDescDisplayText(s), value: s })
      continue
    }
    if (item && typeof item === 'object') {
      const o = item as Record<string, unknown>
      const value = String(o.value ?? o.code ?? o.id ?? '').trim()
      const text = String(o.text ?? o.label ?? o.name ?? value).trim()
      if (!value && !text) continue
      const v = value || text
      const display = normalizeFaultDescDisplayText(text || value)
      out.push({ text: display || v, value: v })
    }
  }
  return out
}

/**
 * 获取条码查询结果
 *
 * 对应 jasic-ui `getWorkOrderBarcodeInfo`（C 端专属接口 `/customer/work-order/barcode-info`）
 *
 * @param params - 条码查询结果
 * @returns - 条码查询结果
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const getCustomerWorkOrderBarcodeInfo = (params: { barcode: string }) => {
  return http<BarcodeInfoDTO>({
    url: '/customer/work-order/barcode-info',
    method: 'GET',
    data: params,
    /** 由报修页在 hideLoading 后统一 toast 接口 msg，避免 loading 遮挡 */
    skipErrorToast: true,
  })
}

// ========== 网点侧工单操作（需 system 权限；与 contractor 路径一致）==========

export type WorkOrderUserOptionVO = {
  id: number
  phone?: string
  realName?: string
}

export type WorkOrderAssignDTO = {
  assignedUserId: number
  workOrderId: number
}

/**
 * 作用：加载/请求：listAssignUserOptions。
 * @returns Promise
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
 * 作用：接口封装：assignWorkOrder。
 * @returns Promise
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

