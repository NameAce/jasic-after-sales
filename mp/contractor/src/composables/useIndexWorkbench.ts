import { computed, ref } from 'vue'
import { useUserStore } from '@/stores'
import type { OrderListItem, WorkOrderMainStatus } from '@/models/order'
import { WORK_ORDER_MAIN_STATUS } from '@/models/order'
import {
  fetchOrderList,
  countWorkOrderStatus,
  mapMainStatusToOrderStatus,
  type OrderListQuery,
} from '@/api/workOrder'
import {
  canCurrentSiteOperateTransferredOrder,
  hasInboundTransferFromSite
} from '@/utils/orderTransfer'
import { ORDER_STATUS_TEXT_MAP, isPendingMainStatus } from '@/utils/orderStatus'
import {
  getPendingDisplayLabel,
  isMainStatusPendingAssign,
  normalizeWorkOrderMainStatus,
} from '@/utils/workOrderMainStatus'
import { Perms } from '@/utils/permissions'

/**
 * 首页工作台：未转单列表、总部统计等
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
      isAsc: 'desc',
      companyId: userStore.userInfo?.currentCompanyId
    }
    if (canAssign && !canAccept) query.mainStatus = 'PENDING_ASSIGN'
    if (!canAssign && canAccept) query.mainStatus = 'PENDING_TECH_ACCEPT'
    return query
  }

  /** 首页统计卡桶（pending 合并 PENDING_ASSIGN + PENDING_TECH_ACCEPT，保留 UI 四卡分组） */
  type WorkbenchStats = { pending: number; processing: number; completed: number; closed: number }

  const emptyStats = (): WorkbenchStats => ({
    pending: 0,
    processing: 0,
    completed: 0,
    closed: 0,
  })

  // 接口对接：网点工作台列表采用后端分页接口（暂取第一页）
  const orderList = ref<OrderListItem[]>([])
  const siteStatusStats = ref<WorkbenchStats>(emptyStats())
  const siteWorkbenchStats = computed(() => siteStatusStats.value)

  // 总部统计/网点负荷
  const hqStatusStats = ref<WorkbenchStats>(emptyStats())
  const hqNetworkStats = computed(() => hqStatusStats.value)
  const hqTransferredCount = ref(0)

  function toSafeCount(n: unknown) {
    const v = Number(n)
    return Number.isFinite(v) && v > 0 ? v : 0
  }

  function addWorkbenchCount(stats: WorkbenchStats, s: WorkOrderMainStatus, count: number) {
    if (isPendingMainStatus(s)) stats.pending += count
    else if (s === WORK_ORDER_MAIN_STATUS.IN_PROGRESS) stats.processing += count
    else if (s === WORK_ORDER_MAIN_STATUS.COMPLETED) stats.completed += count
    else if (s === WORK_ORDER_MAIN_STATUS.CLOSED) stats.closed += count
  }

  let siteRefreshInFlight: Promise<void> | null = null
  const doRefreshSiteWorkbench = async () => {
    const [list, rows] = await Promise.all([
      fetchOrderList(buildSiteWorkbenchListQuery()),
      countWorkOrderStatus({
        viewScope: 'CURRENT',
        companyId: userStore.userInfo?.currentCompanyId
      }),
    ])
    orderList.value = list

    const stats = emptyStats()
    rows.forEach((r) => {
      const mainStatus = (r.mainStatus ?? '').toString()
      const count = toSafeCount(r.countNum)
      if (!mainStatus) return
      addWorkbenchCount(stats, mapMainStatusToOrderStatus(mainStatus), count)
    })
    siteStatusStats.value = stats
  }

  /**
   * 刷新网点工作台
   * @param options 刷新选项
   * @param options.force 是否强制刷新
   * @returns Promise<void>
   */
  const refreshSiteWorkbench = async (options?: { force?: boolean }) => {
    const force = options?.force === true
    if (siteRefreshInFlight) {
      if (force) await siteRefreshInFlight
      else return siteRefreshInFlight
    }
    siteRefreshInFlight = doRefreshSiteWorkbench().finally(() => {
      siteRefreshInFlight = null
    })
    return siteRefreshInFlight
  }

  let hqRefreshInFlight: Promise<void> | null = null
  const doRefreshHqWorkbench = async () => {
    const companyId = userStore.userInfo?.currentCompanyId
    const [rows, transferredRows] = await Promise.all([
      countWorkOrderStatus({ viewScope: 'CURRENT', companyId }),
      countWorkOrderStatus({ viewScope: 'CURRENT', hasTransfer: 1, companyId }),
    ])

    const stats = emptyStats()
    rows.forEach((r) => {
      const mainStatus = (r.mainStatus ?? '').toString()
      const count = toSafeCount(r.countNum)
      if (!mainStatus) return
      addWorkbenchCount(stats, mapMainStatusToOrderStatus(mainStatus), count)
    })
    hqStatusStats.value = stats

    hqTransferredCount.value = transferredRows.reduce((sum, r) => sum + toSafeCount(r.countNum), 0)
  }

  /**
   * 刷新总部工作台
   * @returns Promise<void>
   */
  const refreshHqWorkbench = async () => {
    if (hqRefreshInFlight) return hqRefreshInFlight
    hqRefreshInFlight = doRefreshHqWorkbench().finally(() => {
      hqRefreshInFlight = null
    })
    return hqRefreshInFlight
  }

  /**
   * 是否可以接单
   * @param order 订单
   * @returns boolean
   */
  const canEngineerAcceptOrder = (order: OrderListItem) =>
    canCurrentSiteOperateTransferredOrder(
      !!order.transferred,
      order.transferFromSite,
      userStore.currentNetworkName
    )

  /**
   * 状态文本映射（按派单权限覆盖 PENDING_ASSIGN / PENDING_TECH_ACCEPT 文案）
   * @returns Record<WorkOrderMainStatus, string>
   */
  const statusTextMap = computed<Record<WorkOrderMainStatus, string>>(() => {
    const pendingLabel = userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '待派单' : '待接单'
    return {
      ...ORDER_STATUS_TEXT_MAP,
      PENDING_ASSIGN: pendingLabel,
      PENDING_TECH_ACCEPT: pendingLabel,
    }
  })

  /**
   * 是否显示派单按钮：mainStatus 为 PENDING_ASSIGN（与工单列表一致）
   * @param order 订单
   * @returns boolean
   */
  const showDispatchOrderButton = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    if (!isPendingMainStatus(order.status)) return false
    if (order.dispatcherPendingSubState === 'await_self_accept') return false
    return isMainStatusPendingAssign(order)
  }

  /**
   * 是否显示接单按钮：mainStatus 为 PENDING_TECH_ACCEPT（派单员且已派给自己时与本人待接本地态）
   * @param order 订单
   * @returns boolean
   */
  const showAcceptOrderButton = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ACCEPT)) return false
    if (!canEngineerAcceptOrder(order)) return false
    if (!isPendingMainStatus(order.status)) return false
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) {
      if (order.dispatcherPendingSubState === 'await_self_accept') return true
      const uid = userStore.userInfo?.id
      return (
        normalizeWorkOrderMainStatus(order.mainStatus) === 'PENDING_TECH_ACCEPT' &&
        uid != null &&
        order.assignedUserId != null &&
        Number(order.assignedUserId) === Number(uid)
      )
    }
    return normalizeWorkOrderMainStatus(order.mainStatus) === 'PENDING_TECH_ACCEPT'
  }

  /**
   * 获取订单列表状态文本
   * @param order 订单
   * @returns string
   */
  const getOrderListStatusText = (order: OrderListItem) => {
    if (!isPendingMainStatus(order.status)) return statusTextMap.value[order.status]
    return getPendingDisplayLabel(
      order,
      userStore.hasPermission(Perms.WORKORDER_ASSIGN),
      order.dispatcherPendingSubState === 'await_self_accept'
    )
  }

  /**
   * 工作台列表标题
   * @returns string
   */
  const workbenchListTitle = computed(() => {
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '待派工单'
    return '待接工单'
  })

  /**
   * 工作台空列表标题
   * @returns string
   */
  const workbenchEmptyTitle = computed(() => {
    if (userStore.canAll([Perms.WORKORDER_ASSIGN, Perms.WORKORDER_ACCEPT])) return '暂无待派工单'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '暂无待派单工单'
    return '暂无待接单工单'
  })

  /**
   * 工作台空列表描述
   * @returns string
   */
  const workbenchEmptyDesc = computed(() => {
    if (userStore.canAll([Perms.WORKORDER_ASSIGN, Perms.WORKORDER_ACCEPT]))
      return '当前没有待派单或待接单的工单'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '当前没有待派单的工单'
    return '当前没有待接单的工单'
  })

  /**
   * 待派单/待接单标签
   * @returns string
   */
  const pendingStatLabel = computed(() =>
    userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '待派单' : '待接单'
  )

  /**
   * 是否显示 inbound 转单标签
   * @param order 订单
   * @returns boolean
   */
  const showInboundTransferTag = (order: OrderListItem) =>
    hasInboundTransferFromSite(order.transferFromSite)

  /**
   * 是否显示 transferred 转单标签
   * @param order 订单
   * @returns boolean
   */
  const showTransferredTag = (order: OrderListItem) => !!order.transferred

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
    showTransferredTag
  }
}
