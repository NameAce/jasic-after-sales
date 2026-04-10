import { computed, ref } from 'vue'
import { useUserStore } from '@/stores'
import type { OrderListItem, OrderStatus } from '@/models/order'
import {
  fetchOrderList,
  fetchWorkOrderStatusCount,
  mapMainStatusToOrderStatus,
  type OrderListQuery,
} from '@/api/order'
import {
  canCurrentSiteOperateTransferredOrder,
  hasInboundTransferFromSite
} from '@/utils/orderTransfer'
import { ORDER_STATUS_TEXT_MAP } from '@/utils/orderStatus'
import { Perms } from '@/utils/permissions'

export type HqBranchRow = {
  id: number
  name: string
  load: number
  statusClass: 'high' | 'medium' | 'normal'
  statusText: string
}

/**
 * 首页工作台：未转单列表、总部统计、网点负荷等（与 mock 同源，单一路径避免重复计算）
 */
export function useIndexWorkbench() {
  const userStore = useUserStore()

  /**
   * 首页网点工作台工单列表查询参数（按权限决定待派/待接）
   * - 仅派单权限：看待派单
   * - 仅接单权限：看待接单
   * - 同时具备派单+接单：不限定 mainStatus，交由后端按数据范围返回待处理列表
   */
  const buildSiteWorkbenchListQuery = (): OrderListQuery => {
    const canAssign = userStore.hasPermission(Perms.WORKORDER_ASSIGN)
    const canAccept = userStore.hasPermission(Perms.WORKORDER_ACCEPT)
    const query: OrderListQuery = {
      pageNum: 1,
      pageSize: 20,
      viewScope: 'CURRENT',
      orderByColumn: 'createTime',
      isAsc: 'desc'
    }
    if (canAssign && !canAccept) query.mainStatus = 'PENDING_ASSIGN'
    if (!canAssign && canAccept) query.mainStatus = 'PENDING_TECH_ACCEPT'
    return query
  }

  // 接口对接：网点工作台列表采用后端分页接口（暂取第一页）
  const orderList = ref<OrderListItem[]>([])
  const siteStatusStats = ref({ pending: 0, processing: 0, completed: 0, closed: 0 })
  const siteWorkbenchStats = computed(() => siteStatusStats.value)

  // 总部统计/网点负荷
  const hqStatusStats = ref({ pending: 0, processing: 0, completed: 0, closed: 0 })
  const hqNetworkStats = computed(() => hqStatusStats.value)
  const hqTransferredCount = ref(0)

  function toSafeCount(n: unknown) {
    const v = Number(n)
    return Number.isFinite(v) && v > 0 ? v : 0
  }

  let siteRefreshInFlight: Promise<void> | null = null
  const doRefreshSiteWorkbench = async () => {
    const [list, rows] = await Promise.all([
      fetchOrderList(buildSiteWorkbenchListQuery()),
      fetchWorkOrderStatusCount({
        viewScope: 'CURRENT',
      }),
    ])
    orderList.value = list

    const stats = { pending: 0, processing: 0, completed: 0, closed: 0 }
    rows.forEach((r) => {
      const mainStatus = (r.mainStatus ?? '').toString()
      const count = toSafeCount(r.countNum)
      if (!mainStatus) return
      const s = mapMainStatusToOrderStatus(mainStatus)
      stats[s] += count
    })
    siteStatusStats.value = stats
  }

  const refreshSiteWorkbench = async () => {
    if (siteRefreshInFlight) return siteRefreshInFlight
    siteRefreshInFlight = doRefreshSiteWorkbench().finally(() => {
      siteRefreshInFlight = null
    })
    return siteRefreshInFlight
  }

  let hqRefreshInFlight: Promise<void> | null = null
  const doRefreshHqWorkbench = async () => {
    const [rows, transferredRows] = await Promise.all([
      fetchWorkOrderStatusCount({ viewScope: 'CURRENT' }),
      fetchWorkOrderStatusCount({ viewScope: 'CURRENT', hasTransfer: 1 }),
    ])

    const stats = { pending: 0, processing: 0, completed: 0, closed: 0 }
    rows.forEach((r) => {
      const mainStatus = (r.mainStatus ?? '').toString()
      const count = toSafeCount(r.countNum)
      if (!mainStatus) return
      const s = mapMainStatusToOrderStatus(mainStatus)
      stats[s] += count
    })
    hqStatusStats.value = stats

    hqTransferredCount.value = transferredRows.reduce((sum, r) => sum + toSafeCount(r.countNum), 0)
  }

  const refreshHqWorkbench = async () => {
    if (hqRefreshInFlight) return hqRefreshInFlight
    hqRefreshInFlight = doRefreshHqWorkbench().finally(() => {
      hqRefreshInFlight = null
    })
    return hqRefreshInFlight
  }

  const canEngineerAcceptOrder = (order: OrderListItem) =>
    canCurrentSiteOperateTransferredOrder(
      !!order.transferred,
      order.transferFromSite,
      userStore.currentNetworkName
    )

  const statusTextMap = computed<Record<OrderStatus, string>>(() => ({
    ...ORDER_STATUS_TEXT_MAP,
    pending: userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '待派单' : '待接单'
  }))

  const showDispatchOrderButton = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    if (
      userStore.hasPermission(Perms.WORKORDER_ACCEPT) &&
      order.dispatcherPendingSubState === 'await_self_accept'
    )
      return false
    return true
  }

  const showAcceptOrderButton = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ACCEPT)) return false
    if (!canEngineerAcceptOrder(order)) return false
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) {
      return order.dispatcherPendingSubState === 'await_self_accept'
    }
    return true
  }

  const getOrderListStatusText = (order: OrderListItem) => {
    if (order.status !== 'pending') return statusTextMap.value[order.status]
    if (userStore.canAll([Perms.WORKORDER_ASSIGN, Perms.WORKORDER_ACCEPT])) {
      return order.dispatcherPendingSubState === 'await_self_accept' ? '待接单' : '待派单'
    }
    return statusTextMap.value.pending
  }

  const workbenchListTitle = computed(() => {
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '待派工单'
    return '待接工单'
  })

  const workbenchEmptyTitle = computed(() => {
    if (userStore.canAll([Perms.WORKORDER_ASSIGN, Perms.WORKORDER_ACCEPT])) return '暂无待派工单'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '暂无待派单工单'
    return '暂无待接单工单'
  })

  const workbenchEmptyDesc = computed(() => {
    if (userStore.canAll([Perms.WORKORDER_ASSIGN, Perms.WORKORDER_ACCEPT]))
      return '当前没有待派单或待接单的工单'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '当前没有待派单的工单'
    return '当前没有待接单的工单'
  })

  const pendingStatLabel = computed(() =>
    userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '待派单' : '待接单'
  )

  const showInboundTransferTag = (order: OrderListItem) =>
    hasInboundTransferFromSite(order.transferFromSite)

  const showTransferredTag = (order: OrderListItem) => !!order.transferred

  const branchList = computed<HqBranchRow[]>(() => [])

  return {
    siteWorkbenchStats,
    orderList,
    refreshSiteWorkbench,
    refreshHqWorkbench,
    hqNetworkStats,
    hqTransferredCount,
    getOrderListStatusText,
    showDispatchOrderButton,
    showAcceptOrderButton,
    workbenchListTitle,
    workbenchEmptyTitle,
    workbenchEmptyDesc,
    pendingStatLabel,
    showInboundTransferTag,
    showTransferredTag,
    branchList
  }
}
