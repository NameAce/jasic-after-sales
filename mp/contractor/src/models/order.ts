export type OrderStatus = 'pending' | 'processing' | 'completed' | 'closed'

/** 机器返回方式-回寄：表单回显（与弹窗字段一致） */
export type MailReturnFormEcho = {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  receiptImagePaths: string[]
}

/** 派单员自派后、待本人接单（仍为 pending） */
export type DispatcherPendingSubState = 'await_self_accept'

/** 后端 `/api/system/work-order/list` 单条记录 */
export type WorkOrderListVO = {
  assignedUserId?: number
  assignedUserName?: string
  barcode?: string
  createTime?: string
  currentAcceptCompanyId?: number
  currentAcceptCompanyName?: string
  customerMobile?: string
  customerName?: string
  displayStatus?: string
  hasTransfer?: number
  id: number
  mainStatus?: string
  mainStatusLabel?: string
  orderNo?: string
  productModel?: string
  transferCount?: number
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
  availableActions?: string[]

  assignedUserId?: number
  assignedUserName?: string

  barcode?: string
  brandCode?: string
  brandName?: string
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

  serviceMode?: string
  warrantyStatus?: string
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

  quotes?: WorkOrderQuoteVO[]
  repairs?: WorkOrderRepairVO[]
  reviews?: WorkOrderReviewVO[]
  evaluation?: WorkOrderEvaluationVO
  flows?: WorkOrderFlowVO[]
  participants?: WorkOrderParticipantVO[]
  notifyEvents?: WorkOrderNotifyEventVO[]

  completedTime?: string
  closedTime?: string
  closeReason?: string
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

/** 后端 `/api/system/work-order/notify-event` 单条记录 */
export type WorkOrderNotifyEventVO = {
  companyId?: number
  companyName?: string
  contentSnapshot?: string
  createTime?: string
  eventType?: string
  failReason?: string
  id?: number
  receiverId?: number
  receiverType?: string
  sendStatus?: string
  sendTime?: string
  titleSnapshot?: string
  triggerNode?: string
}

/** 后端 `/api/system/work-order/participant` 单条记录 */
export type WorkOrderParticipantVO = {
  companyId?: number
  companyName?: string
  firstParticipateTime?: string
  isCurrentHandler?: number
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
}

/** 后端 `/api/system/work-order/fault` 单条记录 */
export type WorkOrderFaultVO = {
  companyId?: number
  createTime?: string
  createdBy?: number
  createdByName?: string
  faultDesc?: string
  id?: number
  imageUrls?: string
  otherDesc?: string
  partDesc?: string
  repairDesc?: string
  sortNum?: number
}

/** 后端 `/api/system/work-order/repair` 单条记录 */
export type WorkOrderRepairVO = {
  companyId?: number
  companyName?: string
  createTime?: string
  finishedTime?: string
  id?: number
  isFinished?: number
  repairUserId?: number
  repairUserName?: string
  faults?: WorkOrderFaultVO[]
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
  status: OrderStatus
  isJiashi: boolean
  /** 派单员视角：已派给自己，待本人接单 */
  dispatcherPendingSubState?: DispatcherPendingSubState
  warrantyText?: string
  warrantyClass?: 'tag-in-warranty' | 'tag-out-warranty'
  phone: string
  barcode?: string
  model?: string
  outDate?: string
  desc: string
  transferred?: boolean
  source?: string
  transferNetwork?: string
  /** 转出网点，与详情 base.transferFromSite 一致 */
  transferFromSite?: string
  /** 所属网点/服务站名称 */
  siteName?: string
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

/** 故障点记录 */
export type FaultPointRecord = {
  description: string
  images: { url: string; label: string }[]
  parts?: { name: string; count: number }[]
  specialInfo?: string
  location: string
  date: string
}

/** 工单详情 */
export type OrderDetail = {
  id: string
  status: OrderStatus
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
    submitTime: string
    /** 被转单网点（接收方） */
    transferSite: string
    /** 转出网点（发起转单方，仅已转单时有值） */
    transferFromSite: string
  }
  product: {
    barcode: string
    model: string
    serialNo: string
    outDate: string
    warrantyClass: string
    repairStatus: string
  }
  service: {
    sitePhone: string
    repairMethod: string
    source: string
    senderInfo: string
    senderVoucherImg: string
    /** 回寄时弹窗内寄件信息/凭证图回显，缺省则不回显 */
    mailReturnForm?: MailReturnFormEcho
  }
  acceptor: {
    sitePhone: string
    acceptorName: string
  }
  fault: {
    desc: string
    faultExplain: string
    voiceDuration: string
    images: string[]
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
    history: FaultPointRecord[]
  }
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
  /** 派单员自派待接单 */
  dispatcherPendingSubState?: DispatcherPendingSubState
}

/**
 * 创建空工单详情
 * @returns 空工单详情
 */
export const createEmptyOrderDetail = (): OrderDetail => ({
  id: '',
  status: 'pending',
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
    submitTime: '',
    transferSite: '',
    transferFromSite: '',
  },
  product: {
    barcode: '',
    model: '',
    serialNo: '',
    outDate: '',
    warrantyClass: '',
    repairStatus: '',
  },
  service: {
    sitePhone: '',
    repairMethod: '',
    source: '',
    senderInfo: '',
    senderVoucherImg: '',
  },
  acceptor: {
    sitePhone: '',
    acceptorName: '',
  },
  fault: {
    desc: '',
    faultExplain: '',
    voiceDuration: '',
    images: [],
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
    history: [],
  },
  contact: {
    phone: '',
  },
})

/** 空寄件信息 */
const emptyMailReturnEcho: MailReturnFormEcho = {
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  receiptImagePaths: [],
}

/**
 * 获取寄件信息（用于 ReturnMethodModal 初始值）
 * @param detail 工单详情
 * @returns 寄件信息
 */
export const getReturnMethodInitialMail = (detail: OrderDetail | undefined): MailReturnFormEcho => {
  if (!detail?.service.mailReturnForm) return { ...emptyMailReturnEcho }
  const f = detail.service.mailReturnForm
  return {
    receiverName: f.receiverName ?? '',
    receiverPhone: f.receiverPhone ?? '',
    receiverAddress: f.receiverAddress ?? '',
    receiptImagePaths: f.receiptImagePaths?.length ? [...f.receiptImagePaths] : [],
  }
}

/**
 * 克隆工单详情
 * @param order 工单详情
 * @returns 克隆后的工单详情
 */
export const cloneOrderDetail = (order?: OrderDetail) =>
  JSON.parse(JSON.stringify(order ?? createEmptyOrderDetail())) as OrderDetail
