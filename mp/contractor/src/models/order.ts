/**
 * 工单主状态（mainStatus）枚举：与后端 `WorkOrderMainStatus` 字面完全一致
 *
 * 真源：后端 `sys_work_order.main_status`，以及 jasic-ui 列表字段 `mainStatus`。
 * - PENDING_ASSIGN：待派单
 * - PENDING_TECH_ACCEPT：已派单待维修员接单
 * - IN_PROGRESS：维修中
 * - COMPLETED：已完成
 * - CLOSED：已关闭
 *
 * 禁止使用 `pending / processing / completed / closed` 小写别名（阶段 4.1）。
 */
import type { WorkOrderActionKey } from '@/constants/orderActions'

export type WorkOrderMainStatus =
  | 'PENDING_ASSIGN'
  | 'PENDING_TECH_ACCEPT'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CLOSED'

/** 工单主状态常量（与 `WorkOrderMainStatus` 一一对应，供 api/utils 引用） */
export const WORK_ORDER_MAIN_STATUS = {
  PENDING_ASSIGN: 'PENDING_ASSIGN',
  PENDING_TECH_ACCEPT: 'PENDING_TECH_ACCEPT',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  CLOSED: 'CLOSED',
} as const satisfies Record<WorkOrderMainStatus, WorkOrderMainStatus>

/** @deprecated 请使用 `WorkOrderMainStatus`，保留别名以降低一次性改名成本 */
export type OrderStatus = WorkOrderMainStatus

/** 机器返回方式-回寄：表单回显（与弹窗字段一致） */
export type MailReturnFormEcho = {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  /** 详情接口寄件快递单号 `sendExpressNo`（只读展示） */
  sendExpressNo?: string
  receiptImagePaths: string[]
}

/** 后端 `/api/system/work-order/list` 单条记录 */
export type WorkOrderListVO = {
  assignedUserId?: number
  assignedUserName?: string
  barcode?: string
  createTime?: string
  currentAcceptCompanyId?: number
  currentAcceptCompanyName?: string
  /** 当前受理网点电话 */
  currentAcceptCompanyPhone?: string
  customerMobile?: string
  customerName?: string
  displayStatus?: string
  /** 故障描述（列表接口 WorkOrderListVO） */
  faultDesc?: string
  hasTransfer?: number
  /** 最后出库日期（列表接口，字段名以服务端为准，映射层兼容多种别名） */
  lastOutDate?: string
  outDate?: string
  id: number
  isReadonly?: number
  mainStatus?: string
  mainStatusLabel?: string
  orderNo?: string
  productModel?: string
  /** 维修方式编码：MAIL / STORE 等 */
  serviceMode?: string
  /** 维修方式展示文案 */
  serviceModeLabel?: string
  /** 维修报价（列表接口字段名以服务端为准，常见 quoteAmount） */
  quoteAmount?: number | string
  /** 质保状态（与详情接口一致，如 IN_WARRANTY / OUT_OF_WARRANTY） */
  warrantyStatus?: string
  relationType?: string
  transferCount?: number
  /** 品牌类型：JASIC | NON_JASIC 等（与详情接口一致） */
  brandType?: string
  brandTypeLabel?: string
  /** 后端可执行动作（原始返回，映射层会再做合法 key 过滤） */
  availableActions?: unknown[]
  /** 当前列表项只读原因（viewScope=CURRENT 且无可用动作时由后端填充） */
  readonlyReason?: string
}

/** 后端工单列表分页 */
export type WorkOrderListPageResult = {
  pageNum: number
  pageSize: number
  records: WorkOrderListVO[]
  total: number
}

/** 后端 `/api/system/work-order/{workOrderId}` 详情返回 */
export type WorkOrderDetailVO = {
  id: number
  orderNo?: string
  mainStatus?: string
  mainStatusLabel?: string
  displayStatus?: string
  evaluateStatus?: string
  evaluateStatusLabel?: string
  hasTransfer?: number
  transferCount?: number
  isReadonly?: number
  relationType?: string
  /** 后端可执行动作（原始返回，映射层会再做合法 key 过滤） */
  availableActions?: unknown[]

  assignedUserId?: number
  assignedUserName?: string

  barcode?: string
  brandCode?: string
  brandName?: string
  /** 品牌类型：JASIC | NON_JASIC */
  brandType?: string
  brandTypeLabel?: string
  machineNo?: string
  productModel?: string
  productCode?: string
  productName?: string

  createTime?: string
  createCompanyId?: number
  createCompanyName?: string
  createEntryType?: string

  currentAcceptCompanyId?: number
  currentAcceptCompanyName?: string
  /** 当前受理网点电话 */
  currentAcceptCompanyPhone?: string
  currentAcceptSubjectType?: string
  hqCompanyId?: number
  hqCompanyName?: string

  customerId?: number
  customerName?: string
  customerMobile?: string

  faultDesc?: string
  faultImageFiles?: SysFileItemVO[]
  faultRemark?: string
  faultVideoFiles?: SysFileItemVO[]
  faultVoiceFiles?: SysFileItemVO[]

  warrantyStatus?: string
  // 报修业务类型
  reportBizType?: string
  reportBizTypeLabel?: string

  senderName?: string
  senderMobile?: string
  senderAddress?: string
  sendExpressNo?: string
  senderVoucherFiles?: SysFileItemVO[]

  returnMethod?: string
  returnExpressNo?: string
  returnVoucherFiles?: SysFileItemVO[]
  /** 机器回寄收件人（若详情接口返回，用于弹窗回显） */
  returnReceiverName?: string
  returnReceiverMobile?: string
  returnReceiverAddress?: string

  quotes?: WorkOrderQuoteVO[]
  repairs?: WorkOrderRepairVO[]
  reviews?: WorkOrderReviewVO[]
  evaluation?: WorkOrderEvaluationVO
  flows?: WorkOrderFlowVO[]
  participants?: WorkOrderParticipantVO[]

  completedTime?: string
  closedTime?: string
  closeReason?: string
  /** 最后出库日期（详情接口） */
  lastOutDate?: string
}

/** 后端 `/api/system/sys-file/list` 单条记录 */
export type SysFileItemVO = {
  bizId?: number
  bizType?: string
  contentType?: string
  fileExt?: string
  fileId?: number
  fileSize?: number
  isPrimary?: number
  originalName?: string
  previewUrl?: string
  sortNum?: number
}

/** 后端 `/api/system/work-order/evaluation` 单条记录 */
export type WorkOrderEvaluationVO = {
  companyId?: number
  content?: string
  createTime?: string
  customerId?: number
  id?: number
  qualityScore?: number
  satisfactionScore?: number
  tags?: string
  timelinessScore?: number
}

/** 后端 `/api/system/work-order/flow` 单条记录 */
export type WorkOrderFlowVO = {
  actionName?: string
  actionType?: string
  afterStatus?: string
  afterStatusName?: string
  beforeStatus?: string
  beforeStatusName?: string
  createTime?: string
  fromCompanyId?: number
  fromCompanyName?: string
  id?: number
  operatorCompanyId?: number
  operatorCompanyName?: string
  operatorUserId?: number
  operatorUserName?: string
  remark?: string
  toCompanyId?: number
  toCompanyName?: string
}

/** 后端 `/api/system/work-order/participant` 单条记录 */
export type WorkOrderParticipantVO = {
  companyId?: number
  companyName?: string
  firstParticipateTime?: string
  isCurrentHandler?: number
  isReadonly?: number
  lastParticipateTime?: string
  participateType?: string
  subjectType?: string
}

/** 后端 `/api/system/work-order/quote` 单条记录 */
export type WorkOrderQuoteVO = {
  companyId?: number
  companyName?: string
  createTime?: string
  faultJudge?: string
  id?: number
  isCurrentValid?: number
  quoteAmount?: number
  quoteDesc?: string
  quotedBy?: number
  quotedByName?: string
  /** 部分环境寄件信息与报价同条返回，映射时与工单根字段互补 */
  sendExpressNo?: string
  senderAddress?: string
  senderMobile?: string
  senderName?: string
}

/** 后端工单故障点配件明细 `WorkOrderFaultVO.partList` */
export type WorkOrderFaultPartVO = {
  id?: number
  partName?: string
  partQty?: number
  sortNum?: number
}

/** 后端 `/api/system/work-order/fault` 单条记录 */
export type WorkOrderFaultVO = {
  companyId?: number
  createTime?: string
  createdBy?: number
  createdByName?: string
  faultDesc?: string
  id?: number
  /** 逗号分隔 URL，部分环境故障点图仍走本字段 */
  imageUrls?: string
  otherDesc?: string
  /** 配件说明字符串，与 `partList` 二选一或并存（优先 partList） */
  partDesc?: string
  /** 详情接口：结构化配件明细 */
  partList?: WorkOrderFaultPartVO[]
  repairDesc?: string
  sortNum?: number
}

/** 后端 `/api/system/work-order/repair` 单条记录 */
export type WorkOrderRepairVO = {
  companyId?: number
  companyName?: string
  createTime?: string
  /** 详情接口：维修登记阶段 REPAIR / RECHECK 等 */
  registerStage?: string
  registerStageLabel?: string
  finishedTime?: string
  id?: number
  isFinished?: number
  otherDesc?: string
  repairDesc?: string
  repairSummary?: string
  repairUserId?: number
  repairUserName?: string
  faults?: WorkOrderFaultVO[]
  faultNewImageFiles?: SysFileItemVO[]
  faultOldImageFiles?: SysFileItemVO[]
  machineBarcodeImageFiles?: SysFileItemVO[]
  machineImageFiles?: SysFileItemVO[]
  otherImageFiles?: SysFileItemVO[]
  /** 详情接口若返回与登记提交一致的维修说明多选项 */
  repairItems?: string[]
}

/** 后端 `/api/system/work-order/review` 单条记录 */
export type WorkOrderReviewVO = {
  companyId?: number
  companyName?: string
  createTime?: string
  id?: number
  isContinueRepair?: number
  reviewDesc?: string
  reviewResult?: string
  reviewUserId?: number
  reviewUserName?: string
}

/** 工单列表项 */
export type OrderListItem = {
  id: string
  /** 展示用工单号，有则卡片标题优先显示 */
  orderNo?: string
  /** 列表接口原始主状态（PENDING_ASSIGN 等） */
  mainStatus?: string
  /**
   * 派单员视角子态：已派给本人、待本人在小程序侧点「接单」前的前端识别（与列表按钮区一致）
   */
  dispatcherPendingSubState?: 'await_self_accept'
  assignedUserId?: number
  /** 当前处理人姓名（有值时列表可展示） */
  assignedUserName?: string
  status: OrderStatus
  /** 接口 `brandType` 归一化大写，如 JASIC、NON_JASIC */
  brandType?: string
  /** 接口 `brandTypeLabel`，列表卡片优先展示 */
  brandTypeLabel?: string
  /** 是否佳士品牌工单，由 `brandType` 推导（与详情一致：缺省 brandType 时默认 true） */
  isJiashi: boolean
  warrantyText?: string
  warrantyClass?: 'tag-in-warranty' | 'tag-out-warranty'
  phone: string
  barcode?: string
  model?: string
  outDate?: string
  /** 列表接口 `faultDesc`，卡片「故障描述」优先展示 */
  faultDesc?: string
  /** 兼容旧接口或其它列表来源的摘要文案 */
  desc?: string
  transferred?: boolean
  source?: string
  transferNetwork?: string
  /** 转出网点，与详情 base.transferFromSite 一致 */
  transferFromSite?: string
  /** 后端可执行动作（优先用于列表按钮渲染） */
  availableActions?: WorkOrderActionKey[]
  /** 无可用动作时的只读提示（与 jasic-ui-antd 列表一致） */
  readonlyReason?: string
  /** 所属网点/服务站名称 */
  siteName?: string
  /** 所属网点/服务站联系电话（受理公司 contact_phone） */
  sitePhone?: string
  /** 维修价格展示（如 128.00） */
  repairPriceText?: string
  /** 维修方式展示文案（优先 serviceModeLabel，兼容 serviceMode） */
  repairMethodLabel?: string
  /**
   * 列表接口 `createTime`；界面「提交时间」与详情 `base.submitTime` 同源
   */
  createTime?: string
}

/** 网点列表项 */
export type BranchItem = {
  id: number
  name: string
  total: number
  pending: number
  processing: number
  completed: number
}

/** 工单详情页展示的流转节点（来源：后端 flows） */
export type OrderDetailProcessFlowItem = {
  time: string
  title: string
  detail: string
}

/** 故障点记录 */
export type FaultPointRecord = {
  /** 维修说明汇总（faultDesc · 维修主文案），旧缓存可能仅有本字段 */
  description: string
  /** 结构化字段（新映射必带，便于历史页按「其它维修说明」规则展示） */
  faultDesc?: string
  repairDesc?: string
  otherDesc?: string
  images: { url: string; label: string }[]
  parts?: { name: string; count: number }[]
  /** repairDesc 非「其它维修说明」时的补充说明 */
  specialInfo?: string
  location: string
  date: string
}

/** 复检登记入口：从最近一次「维修登记」repairs 解析出的表单预填（排除复检阶段记录） */
export type OrderRepairRegistrationEcho = {
  /** 同源维修 faults 解析出的确认故障项，复检只读展示与维修说明下拉过滤用 */
  confirmFaultItems?: string[]
  /** 故障描述含其它/其他类时从 fault.otherDesc 汇总 */
  confirmFaultOtherRemark?: string
  repairItems: string[]
  otherDesc: string
  parts: { partName: string; partQty: number }[]
  faultOldImageFiles: SysFileItemVO[]
  faultNewImageFiles: SysFileItemVO[]
  machineImageFiles: SysFileItemVO[]
  machineBarcodeImageFiles: SysFileItemVO[]
  otherImageFiles: SysFileItemVO[]
}

/** 工单详情 */
export type OrderDetail = {
  id: string
  /** 详情接口可执行动作（已做合法 key 过滤与去重） */
  availableActions: WorkOrderActionKey[]
  status: OrderStatus
  /** 接口原始主状态（与列表 `mainStatus` 一致，用于区分待派单/待接单） */
  mainStatus?: string
  /** 已指派维修员 ID（派单员视角：派给他人则仅可查看） */
  assignedUserId?: number
  transferred: boolean
  brand: {
    isJiashi: boolean
  }
  customer: {
    phone: string
  }
  base: {
    orderNo: string
    orderTypeName: string
    /** 品牌类型文案，如「佳士」/「非佳士」 */
    brandTypeLabel: string
    submitTime: string
    /** 被转单网点（接收方） */
    transferSite: string
    /** 转出网点（发起转单方，仅已转单时有值） */
    transferFromSite: string
  }
  product: {
    barcode: string
    /** 商品品牌名（详情 `brandName`） */
    brandName: string
    model: string
    serialNo: string
    /** 最后出库日期（同源 `WorkOrderDetailVO.lastOutDate` 等，映射层兼容别名） */
    lastOutDate: string
    warrantyClass: string
    repairStatus: string
  }
  service: {
    sitePhone: string
    /** 维修方式展示文案（与 C 端一致：优先 `serviceModeLabel`，映射层已兜底编码） */
    serviceModeLabel: string
    source: string
    senderInfo: string
    /** 详情 `senderName`，回寄预填 */
    senderName: string
    /** 详情 `senderMobile`，回寄预填 */
    senderMobile: string
    /** 详情 `senderAddress`，回寄预填 */
    senderAddress: string
    /** 详情 `sendExpressNo`，回寄只读展示 */
    sendExpressNo: string
    /** 寄件凭证首图（与 C 端 `order.service.senderVoucherImg` 一致） */
    senderVoucherImg: string
    /** 寄件快递单/凭证附件 */
    senderVoucherFiles: { previewUrl: string }[]
    /** 机器返回方式（回寄/自提等，展示用文案，以后端为准） */
    returnMethod: string
    /** 回寄快递单号（只读展示） */
    returnExpressNo: string
    /** 回寄时弹窗内寄件信息/凭证图回显，缺省则不回显 */
    mailReturnForm?: MailReturnFormEcho
  }
  acceptor: {
    /** 详情 `currentAcceptCompanyName`，当前受理网点/单位 */
    currentAcceptCompanyName: string
    /**
     * 与 C 端 aftersale 一致：同源 `currentAcceptCompanyPhone`
     * @see mp/aftersale `order.acceptor.sitePhone`
     */
    sitePhone: string
    /** 详情 `currentAcceptCompanyPhone`（与 `sitePhone` 同值） */
    currentAcceptCompanyPhone: string
  }
  fault: {
    /** 客户报修描述，同源 `WorkOrderDetailVO.faultDesc` */
    desc: string
    /** 客户故障备注，同源 `WorkOrderDetailVO.faultRemark` */
    faultExplain: string
    voiceDuration: string
    /** `faultVoiceFiles` 映射，供详情 `VoicePlaybackList` */
    voiceList?: { url: string; duration?: number }[]
    /** `faultImageFiles` 预览地址 */
    images: string[]
    /** `faultVideoFiles` 预览地址 */
    videos: string[]
    videoThumb: string
  }
  repair: {
    faultJudge: string
    quoteAmount: string
    quoteDesc: string
  }
  faultPoint: {
    current: {
      date: string
      desc: string
    }
    /** 最新一条维修（repairs 末条）下的故障点，对应后端 `repairs[].faults` */
    currentFaults: WorkOrderFaultVO[]
    /** 除末条维修外的历史故障点（用于与「当前」对比展示） */
    history: FaultPointRecord[]
    /** 全部维修单下 `faults` 扁平列表，供「查看历史记录」页完整回显 */
    allRepairsFaultRecords: FaultPointRecord[]
  }
  /** 工单流转记录（后端 flows） */
  processFlows: OrderDetailProcessFlowItem[]
  /** 最近一次维修登记摘要（复检入口用于表单回显） */
  repairRegistrationEcho?: OrderRepairRegistrationEcho
  contact: {
    phone: string
  }
  /** 客户评价（已关闭等场景） */
  evaluate?: {
    /** 服务时效 1–5 */
    timeliness?: number
    /** 维修质量 1–5 */
    quality?: number
    /** 服务满意度 1–5 */
    satisfaction?: number
    comment?: string
  }
}

/**
 * 创建空工单详情
 * @returns 空工单详情
 */
export const createEmptyOrderDetail = (): OrderDetail => ({
  id: '',
  availableActions: [],
  status: 'PENDING_ASSIGN',
  transferred: false,
  brand: {
    isJiashi: true,
  },
  customer: {
    phone: '',
  },
  base: {
    orderNo: '',
    orderTypeName: '',
    brandTypeLabel: '',
    submitTime: '',
    transferSite: '',
    transferFromSite: '',
  },
  product: {
    barcode: '',
    brandName: '',
    model: '',
    serialNo: '',
    lastOutDate: '',
    warrantyClass: '',
    repairStatus: '',
  },
  service: {
    sitePhone: '',
    serviceModeLabel: '',
    source: '',
    senderInfo: '',
    senderName: '',
    senderMobile: '',
    senderAddress: '',
    sendExpressNo: '',
    senderVoucherImg: '',
    senderVoucherFiles: [],
    returnMethod: '',
    returnExpressNo: '',
  },
  acceptor: {
    currentAcceptCompanyName: '',
    sitePhone: '',
    currentAcceptCompanyPhone: '',
  },
  fault: {
    desc: '',
    faultExplain: '',
    voiceDuration: '',
    voiceList: undefined,
    images: [],
    videos: [],
    videoThumb: '',
  },
  repair: {
    faultJudge: '',
    quoteAmount: '0.00',
    quoteDesc: '',
  },
  faultPoint: {
    current: {
      date: '',
      desc: '',
    },
    currentFaults: [],
    history: [],
    allRepairsFaultRecords: [],
  },
  processFlows: [],
  contact: {
    phone: '',
  },
})

/** 空寄件信息 */
const emptyMailReturnEcho: MailReturnFormEcho = {
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  sendExpressNo: '',
  receiptImagePaths: [],
}

/**
 * 获取寄件信息（用于 ReturnMethodModal 初始值）
 * 姓名/电话/地址/寄件单号：优先详情寄件字段，缺省再用 `mailReturnForm` 补齐。
 * 回寄快递单号照片：始终不预填，由用户在弹窗内自行上传（不从详情 `senderVoucherFiles` / `returnVoucherFiles` 带入）。
 * @param detail 工单详情
 * @returns 寄件信息
 */
export const getReturnMethodInitialMail = (detail: OrderDetail | undefined): MailReturnFormEcho => {
  if (!detail) return { ...emptyMailReturnEcho }
  const s = detail.service
  const fromVo: MailReturnFormEcho = {
    receiverName: String(s.senderName ?? '').trim(),
    receiverPhone: String(s.senderMobile ?? '').trim(),
    receiverAddress: String(s.senderAddress ?? '').trim(),
    sendExpressNo: String(s.sendExpressNo ?? '').trim(),
    receiptImagePaths: [],
  }
  const m = s.mailReturnForm
  if (!m) {
    return { ...fromVo }
  }
  return {
    receiverName: fromVo.receiverName || String(m.receiverName ?? '').trim(),
    receiverPhone: fromVo.receiverPhone || String(m.receiverPhone ?? '').trim(),
    receiverAddress: fromVo.receiverAddress || String(m.receiverAddress ?? '').trim(),
    sendExpressNo: fromVo.sendExpressNo || String(m.sendExpressNo ?? '').trim(),
    receiptImagePaths: [],
  }
}

/**
 * 克隆工单详情
 * @param order 工单详情
 * @returns 克隆后的工单详情
 */
export const cloneOrderDetail = (order?: OrderDetail) =>
  JSON.parse(JSON.stringify(order ?? createEmptyOrderDetail())) as OrderDetail

