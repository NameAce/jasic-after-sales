import { computed, type Ref } from 'vue'
import type { OrderDetail, OrderStatus } from '@/models/order'
import { useUserStore } from '@/stores'
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
  orderStatus: Ref<OrderStatus>
  detailEntryAction: Ref<DetailEntryAction>
  currentTab: Ref<number>
  faultJudgeSelect: Ref<string>
}) {
  const { order, orderStatus, detailEntryAction, currentTab, faultJudgeSelect } = options
  const userStore = useUserStore()

  const isPending = computed(() => orderStatus.value === 'pending')
  const isProcessing = computed(() => orderStatus.value === 'processing')
  const isCompleted = computed(() => orderStatus.value === 'completed')
  const isClosed = computed(() => orderStatus.value === 'closed')

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
    if (
      isPending.value &&
      canEditFaultJudge.value &&
      (faultJudgeSelect.value === '无故障' || faultJudgeSelect.value === '有故障')
    ) {
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

  const hasFaultPoint = computed(() => hasVal(order.value.faultPoint?.current?.date))

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
      hasVal(order.value.base.orderTypeName) ||
      hasVal(order.value.base.submitTime)
  )

  const repairExtrasLayout = computed<OrderDetailRepairExtrasLayout>(() => {
    const s = orderStatus.value
    const action = detailEntryAction.value
    if (s === 'pending') return 'pending'
    if (s === 'processing' || (s === 'completed' && action === 'recheck')) return 'active_repair'
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
