import { computed, ref } from 'vue'
import { useUserStore } from '@/stores'
import type { OrderListItem, WorkOrderMainStatus } from '@/models/order'
import {
  aggregateWorkOrderStatusTabCounts,
  listWorkOrder,
  countWorkOrderStatus,
  pickWorkOrderStatusCountForMainCode,
  type OrderListQuery,
  type WorkOrderStatusCountVO,
  type WorkOrderStatusTabCounts,
} from '@/api/workOrder'
import { hasInboundTransferFromSite } from '@/utils/orderTransfer'
import { ORDER_STATUS_TEXT_MAP, isPendingMainStatus } from '@/utils/orderStatus'
import { Perms } from '@/utils/permissions'
import { isWorkOrderPendingTechAcceptMainStatus } from '@/utils/workOrderMainStatus'

/**
 * 首页工作台：未转单列表、总部统计等
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useIndexWorkbench() {
  const userStore = useUserStore()

  /**
   * 首页网点工作台工单列表查询参数（按权限决定待派/待接）
   * - 有派单权限：只看待派单（与订单列表「待派单」二级 Tab 一致，避免待派/待接混在同一列表）
   * - 仅接单权限：看待接单
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const SITE_LIST_PAGE_SIZE = 10

  const buildSiteWorkbenchListQuery = (pageNum: number): OrderListQuery => {
    const canAssign = userStore.hasPermission(Perms.WORKORDER_ASSIGN)
    const canAccept = userStore.hasPermission(Perms.WORKORDER_ACCEPT)
    const query: OrderListQuery = {
      pageNum,
      pageSize: SITE_LIST_PAGE_SIZE,
      viewScope: 'CURRENT',
      orderByColumn: 'createTime',
      isAsc: 'desc'
    }
    if (canAssign) query.mainStatus = 'PENDING_ASSIGN'
    else if (canAccept) query.mainStatus = 'PENDING_TECH_ACCEPT'
    return query
  }

  const orderList = ref<OrderListItem[]>([])
  const siteListPageNum = ref(1)
  const siteListTotal = ref(0)
  const siteListLoadingMore = ref(false)
  const emptyTabCounts = (): WorkOrderStatusTabCounts => ({
    pendingAssign: 0,
    pendingTechAccept: 0,
    processing: 0,
    completed: 0,
    closed: 0
  })
  const siteStatusStats = ref(emptyTabCounts())
  const siteWorkbenchStats = computed(() => siteStatusStats.value)
  /**
 * 最近一次网点工作台 status-count 原始行（首卡文案与数量与接口 displayStatus / countNum 对齐）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const siteStatusCountRows = ref<WorkOrderStatusCountVO[]>([])

  // 总部统计
  const hqStatusStats = ref(emptyTabCounts())
  const hqNetworkStats = computed(() => hqStatusStats.value)
  const hqTransferredCount = ref(0)

  function toSafeCount(n: unknown) {
    const v = Number(n)
    return Number.isFinite(v) && v > 0 ? v : 0
  }

  let siteRefreshInFlight: Promise<void> | null = null
  const doRefreshSiteWorkbench = async () => {
    try {
      const [page, rows] = await Promise.all([
        listWorkOrder(buildSiteWorkbenchListQuery(1)),
        countWorkOrderStatus({
          viewScope: 'CURRENT',
        }),
      ])
      orderList.value = page.records
      siteListPageNum.value = 1
      siteListTotal.value = page.total

      siteStatusCountRows.value = rows
      siteStatusStats.value = aggregateWorkOrderStatusTabCounts(rows)
    } catch {
      orderList.value = []
      siteListPageNum.value = 1
      siteListTotal.value = 0
      siteStatusCountRows.value = []
      siteStatusStats.value = emptyTabCounts()
    }
  }

  const loadMoreSiteWorkbench = async () => {
    if (siteListLoadingMore.value) return
    if (siteListTotal.value > 0 && orderList.value.length >= siteListTotal.value) return
    if (!orderList.value.length && siteListPageNum.value === 1) return

    siteListLoadingMore.value = true
    try {
      const next = siteListPageNum.value + 1
      const page = await listWorkOrder(buildSiteWorkbenchListQuery(next))
      siteListPageNum.value = next
      siteListTotal.value = page.total
      if (page.records.length) {
        orderList.value = orderList.value.concat(page.records)
      }
    } catch {
      /* listWorkOrder 已 toast */
    } finally {
      siteListLoadingMore.value = false
    }
  }

  const showSiteWorkbenchNoMore = computed(
    () =>
      orderList.value.length > 0 &&
      siteListTotal.value > 0 &&
      orderList.value.length >= siteListTotal.value
  )

  /**
   * @param force 为 true 时先等待进行中的刷新结束再拉取，避免派单等操作后复用派单前发起的请求导致列表不更新
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const refreshSiteWorkbench = async (force = false) => {
    if (!force && siteRefreshInFlight) return siteRefreshInFlight
    if (force && siteRefreshInFlight) {
      try {
        await siteRefreshInFlight
      } catch {
        /* doRefreshSiteWorkbench 内已处理 */
      }
    }
    const p = doRefreshSiteWorkbench().finally(() => {
      if (siteRefreshInFlight === p) siteRefreshInFlight = null
    })
    siteRefreshInFlight = p
    return p
  }

  let hqRefreshInFlight: Promise<void> | null = null
  const doRefreshHqWorkbench = async () => {
    try {
      const [rows, transferredRows] = await Promise.all([
        countWorkOrderStatus({ viewScope: 'CURRENT' }),
        countWorkOrderStatus({ viewScope: 'CURRENT', hasTransfer: 1 }),
      ])

      hqStatusStats.value = aggregateWorkOrderStatusTabCounts(rows)

      hqTransferredCount.value = transferredRows.reduce((sum, r) => sum + toSafeCount(r.countNum), 0)
    } catch {
      hqStatusStats.value = emptyTabCounts()
      hqTransferredCount.value = 0
    }
  }

  const refreshHqWorkbench = async () => {
    if (hqRefreshInFlight) return hqRefreshInFlight
    hqRefreshInFlight = doRefreshHqWorkbench().finally(() => {
      hqRefreshInFlight = null
    })
    return hqRefreshInFlight
  }

  const statusTextMap = computed<Record<WorkOrderMainStatus, string>>(() => {
    const pendingLabel = userStore.hasPermission(Perms.WORKORDER_ASSIGN) ? '待派单' : '待接单'
    return {
      ...ORDER_STATUS_TEXT_MAP,
      PENDING_ASSIGN: pendingLabel,
      PENDING_TECH_ACCEPT: pendingLabel,
    }
  })

  const getOrderListStatusText = (order: OrderListItem) => {
    if (!isPendingMainStatus(order.status)) return statusTextMap.value[order.status]
    if (userStore.canAll([Perms.WORKORDER_ASSIGN, Perms.WORKORDER_ACCEPT])) {
      return isWorkOrderPendingTechAcceptMainStatus(order.mainStatus) ? '待接单' : '待派单'
    }
    return statusTextMap.value.PENDING_ASSIGN
  }

  const workbenchListTitle = computed(() => {
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '待派单工单'
    return '待接工单'
  })

  const workbenchEmptyTitle = computed(() => {
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return '暂无待派单工单'
    return '暂无待接单工单'
  })

  const workbenchEmptyDesc = computed(() => {
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN))
      return '当前没有待派单的工单，待接单请前往工单列表'
    return '当前没有待接单的工单'
  })

  /**
 * 首卡：接口返回的 displayStatus + 对应 mainStatus 行的 countNum（无则回退文案与聚合值）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const sitePrimaryPendingStat = computed(() => {
    const canAssign = userStore.hasPermission(Perms.WORKORDER_ASSIGN)
    const code = canAssign ? 'PENDING_ASSIGN' : 'PENDING_TECH_ACCEPT'
    const picked = pickWorkOrderStatusCountForMainCode(siteStatusCountRows.value, code)
    const fallbackLabel = canAssign ? '待派单' : '待接单'
    const fallbackCount = canAssign
      ? siteStatusStats.value.pendingAssign
      : siteStatusStats.value.pendingTechAccept
    return {
      label: picked.displayStatus || fallbackLabel,
      count: picked.matched ? picked.count : fallbackCount
    }
  })

  const showInboundTransferTag = (order: OrderListItem) =>
    hasInboundTransferFromSite(order.transferFromSite)

  const showTransferredTag = (order: OrderListItem) => !!order.transferred

  return {
    siteWorkbenchStats,
    orderList,
    refreshSiteWorkbench,
    loadMoreSiteWorkbench,
    showSiteWorkbenchNoMore,
    refreshHqWorkbench,
    hqNetworkStats,
    hqTransferredCount,
    getOrderListStatusText,
    workbenchListTitle,
    workbenchEmptyTitle,
    workbenchEmptyDesc,
    sitePrimaryPendingStat,
    showInboundTransferTag,
    showTransferredTag
  }
}
