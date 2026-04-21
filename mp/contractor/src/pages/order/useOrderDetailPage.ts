import { computed, type Ref } from 'vue'
import type { OrderDetail, WorkOrderMainStatus } from '@/models/order'
import { useUserStore } from '@/stores'
import { isPendingMainStatus } from '@/utils/orderStatus'
import { canCurrentSiteOperateTransferredOrder } from '@/utils/orderTransfer'
import { hasVal } from '@/utils/value'

export type DetailEntryAction = 'accept' | 'repair' | 'recheck' | ''

/** 维修过程 Tab 下方 Extra Cards 三段布局（互斥） */
export type OrderDetailRepairExtrasLayout = 'pending' | 'active_repair' | 'readonly_summary'

export function isDetailEntryAction(value: unknown): value is DetailEntryAction {
  return value === 'accept' || value === 'repair' || value === 'recheck' || value === ''
}

export function shouldOpenRepairTab(action: DetailEntryAction): boolean {
  return action === 'accept' || action === 'repair' || action === 'recheck'
}

export function useOrderDetailPage(options: {
  order: Ref<OrderDetail>
  orderStatus: Ref<WorkOrderMainStatus>
  detailEntryAction: Ref<DetailEntryAction>
  currentTab: Ref<number>
  faultJudgeSelect: Ref<string>
  /** 无故障「维修完成」依赖关单权限；无权限时不应占底部操作栏 */
  canCompleteNoFaultRepair?: Ref<boolean>
}) {
  const { order, orderStatus, detailEntryAction, currentTab, faultJudgeSelect, canCompleteNoFaultRepair } =
    options
  const userStore = useUserStore()

  const isPending = computed(() => isPendingMainStatus(orderStatus.value))
  const isProcessing = computed(() => orderStatus.value === 'IN_PROGRESS')
  const isCompleted = computed(() => orderStatus.value === 'COMPLETED')
  const isClosed = computed(() => orderStatus.value === 'CLOSED')

  // 转单权限
  const canOperateTransferredOrder = computed(() =>
    canCurrentSiteOperateTransferredOrder(
      order.value.transferred,
      order.value.base.transferFromSite,
      userStore.currentNetworkName
    )
  )

  const isTransferredOutViewer = computed(() => {
    if (!order.value.transferred) return false
    const from = (order.value.base.transferFromSite || '').trim()
    const me = userStore.currentNetworkName
    if (!from || !me) return false
    return me === from
  })

  const canEditFaultJudge = computed(
    () =>
      isPending.value && detailEntryAction.value === 'accept' && canOperateTransferredOrder.value
  )

  const canEditFaultPoint = computed(
    () =>
      canOperateTransferredOrder.value &&
      ((isProcessing.value && detailEntryAction.value === 'repair') ||
        (isCompleted.value && detailEntryAction.value === 'recheck'))
  )

  const hasBottomActionBar = computed(() => {
    if (currentTab.value !== 1) return false
    const fj = faultJudgeSelect.value
    const noFaultOk = fj === '无故障' && (canCompleteNoFaultRepair?.value ?? true)
    if (isPending.value && canEditFaultJudge.value && (fj === '有故障' || noFaultOk)) {
      return true
    }
    if (
      (isProcessing.value || (isCompleted.value && detailEntryAction.value === 'recheck')) &&
      canEditFaultPoint.value
    ) {
      return true
    }
    return false
  })

  const showEvaluateTab = computed(() => isClosed.value)

  /** 仅当详情接口返回 repairs 下 faults（映射为 currentFaults）时展示故障点区块 */
  const hasFaultPoint = computed(
    () => (order.value.faultPoint?.currentFaults?.length ?? 0) > 0
  )

  const hasRepairProcessContent = computed(
    () =>
      (isPending.value && (canEditFaultJudge.value || hasFaultPoint.value)) ||
      canEditFaultPoint.value ||
      ((isCompleted.value || isClosed.value) &&
        hasFaultPoint.value &&
        detailEntryAction.value !== 'recheck')
  )

  const hasOrderBaseInfo = computed(
    () =>
      hasVal(order.value.base.orderNo) ||
      hasVal(order.value.base.brandTypeLabel) ||
      hasVal(order.value.base.submitTime)
  )

  const repairExtrasLayout = computed<OrderDetailRepairExtrasLayout>(() => {
    const s = orderStatus.value
    const action = detailEntryAction.value
    if (isPendingMainStatus(s)) return 'pending'
    if (s === 'IN_PROGRESS' || (s === 'COMPLETED' && action === 'recheck')) return 'active_repair'
    return 'readonly_summary'
  })

  return {
    isPending,
    isProcessing,
    isCompleted,
    isClosed,
    canOperateTransferredOrder,
    isTransferredOutViewer,
    canEditFaultJudge,
    canEditFaultPoint,
    hasBottomActionBar,
    showEvaluateTab,
    hasFaultPoint,
    hasRepairProcessContent,
    hasOrderBaseInfo,
    repairExtrasLayout
  }
}
