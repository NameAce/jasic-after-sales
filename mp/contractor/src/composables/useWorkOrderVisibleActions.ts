import { type ComputedRef, type Ref } from 'vue'
import { useUserStore } from '@/stores'
import type { OrderListItem, WorkOrderMainStatus } from '@/models/order'
import {
  normalizeAvailableActions,
  sortWorkOrderActionsForDisplay,
  type WorkOrderActionKey
} from '@/constants/orderActions'
import {
  ENABLE_LEGACY_STATUS_ACTION_FALLBACK,
  auditLegacyStatusFallbackHit
} from '@/constants/orderActionFallback'
import { canCurrentSiteOperateTransferredOrder } from '@/utils/orderTransfer'
import { Perms } from '@/utils/permissions'
import { isWorkOrderPendingTechAcceptMainStatus } from '@/utils/workOrderMainStatus'

/** 列表/工作台行内操作按钮 */
export type WorkOrderVisibleAction = {
  key: WorkOrderActionKey
  label: string
  className: 'primary' | 'outline'
}

export type WorkOrderVisibleActionsPrimaryTab = 'untransferred' | 'transferred'

export type UseWorkOrderVisibleActionsOptions = {
  /** 一级 Tab：已转单不展示操作按钮 */
  primaryTab: Ref<WorkOrderVisibleActionsPrimaryTab> | ComputedRef<WorkOrderVisibleActionsPrimaryTab>
  /** 总部「总部处理」视图 */
  isHqProcessView: Ref<boolean> | ComputedRef<boolean>
  /** 受理方是否为当前登录主体公司 */
  isOrderAcceptedByCurrentCompany: (order: OrderListItem) => boolean
  /** 仅状态兜底使用：二级 Tab（审计埋点） */
  secondaryTab?: Ref<string> | ComputedRef<string>
  /** 仅状态兜底：是否走派单员状态块 */
  showDispatcherActionBlock?: (order: OrderListItem) => boolean
  /** 仅状态兜底：是否走工程师状态块 */
  showOperatorActions?: (order: OrderListItem) => boolean
  /** 仅状态兜底：按状态取工程师动作 */
  getOperatorActions?: (status: WorkOrderMainStatus) => WorkOrderVisibleAction[]
  /** 仅状态兜底：工程师条是否已展示复检（避免派单条重复） */
  operatorShowsRecheck?: (order: OrderListItem) => boolean
}

const getActionLabel = (actionKey: WorkOrderActionKey) => {
  if (actionKey === 'ASSIGN') return '派单'
  if (actionKey === 'TECH_ACCEPT') return '接单'
  if (actionKey === 'TRANSFER') return '转单'
  if (actionKey === 'REPAIR_FINISH') return '维修登记'
  if (actionKey === 'REVIEW') return '复检登记'
  if (actionKey === 'CLOSE') return '机器返回方式'
  return ''
}

const getActionClassName = (actionKey: WorkOrderActionKey): WorkOrderVisibleAction['className'] =>
  actionKey === 'CLOSE' ? 'outline' : 'primary'

/**
 * 工单列表/工作台行内按钮：优先使用接口 `availableActions`，缺失时可回退旧状态逻辑。
 */
export function useWorkOrderVisibleActions(options: UseWorkOrderVisibleActionsOptions) {
  const userStore = useUserStore()

  const isOrderPendingTechAccept = (order: OrderListItem) => order.status === 'PENDING_TECH_ACCEPT'

  const isOrderPendingAssign = (order: OrderListItem) =>
    order.status === 'PENDING_ASSIGN' && !isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)

  /**
   * 派单员：工单已指派给他人时仅可查看
   */
  const isDispatcherOrderAssignedToOther = (order: OrderListItem) => {
    if (!userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return false
    const aid = order.assignedUserId
    if (aid === undefined || aid === null) return false
    const assigned = Number(aid)
    if (!Number.isFinite(assigned) || assigned <= 0) return false
    const selfId = userStore.userInfo?.id
    if (!Number.isFinite(selfId)) return false
    return assigned !== Number(selfId)
  }

  /**
   * 待派单/待接单同时存在时，与详情页一致做互斥展示
   */
  const applyAssignTechExclusion = (
    keys: WorkOrderActionKey[],
    order: OrderListItem
  ): WorkOrderActionKey[] => {
    if (keys.length < 2) return keys
    if (!keys.includes('ASSIGN') || !keys.includes('TECH_ACCEPT')) return keys
    if (isOrderPendingAssign(order)) return keys.filter((k) => k !== 'TECH_ACCEPT')
    if (isOrderPendingTechAccept(order)) return keys.filter((k) => k !== 'ASSIGN')
    return keys
  }

  /** 承修方端暂不展示「上传寄件单号」 */
  const getOrderAvailableActions = (order: OrderListItem): WorkOrderActionKey[] => {
    const base = normalizeAvailableActions(order.availableActions).filter(
      (key) => key !== 'UPLOAD_SEND_EXPRESS'
    )
    return applyAssignTechExclusion(base, order)
  }

  /**
   * 业务约束过滤（后端已做权限计算；此处仅保留网点/转单/指派等前端必守规则）
   */
  const isActionAllowed = (order: OrderListItem, actionKey: WorkOrderActionKey) => {
    const primary = options.primaryTab.value
    if (primary === 'transferred') return false
    if (!options.isOrderAcceptedByCurrentCompany(order)) return false
    if (isDispatcherOrderAssignedToOther(order)) return false

    if (actionKey === 'TECH_ACCEPT' || actionKey === 'REPAIR_FINISH') {
      return canCurrentSiteOperateTransferredOrder(
        !!order.transferred,
        order.transferFromSite,
        userStore.currentNetworkName
      )
    }
    return true
  }

  const toActionButtons = (actionKeys: WorkOrderActionKey[]): WorkOrderVisibleAction[] =>
    actionKeys.map((key) => ({
      key,
      label: getActionLabel(key),
      className: getActionClassName(key)
    }))

  const getFallbackStatusActions = (order: OrderListItem): WorkOrderActionKey[] => {
    if (!options.showDispatcherActionBlock && !options.showOperatorActions) return []
    const actionKeys: WorkOrderActionKey[] = []
    const pushUnique = (actionKey: WorkOrderActionKey) => {
      if (!actionKeys.includes(actionKey)) actionKeys.push(actionKey)
    }

    if (options.showDispatcherActionBlock?.(order)) {
      if (isOrderPendingAssign(order)) pushUnique('ASSIGN')
      else if (isOrderPendingTechAccept(order)) pushUnique('TECH_ACCEPT')
      else if (
        order.status === 'IN_PROGRESS' &&
        userStore.hasPermission(Perms.WORKORDER_TRANSFER)
      ) {
        pushUnique('TRANSFER')
      } else if (order.status === 'COMPLETED') {
        if (
          userStore.hasPermission(Perms.WORKORDER_REVIEW) &&
          !options.operatorShowsRecheck?.(order)
        ) {
          pushUnique('REVIEW')
        }
        if (userStore.hasPermission(Perms.WORKORDER_TRANSFER)) pushUnique('TRANSFER')
      }
    }

    if (options.showOperatorActions?.(order)) {
      options.getOperatorActions?.(order.status).forEach((action) => {
        pushUnique(action.key)
      })
    }

    return actionKeys
  }

  const getOrderRoleForRegression = (
    order: OrderListItem
  ): 'hq' | 'dispatcher' | 'engineer' | 'unknown' => {
    if (options.isHqProcessView.value) return 'hq'
    if (userStore.hasPermission(Perms.WORKORDER_ASSIGN)) return 'dispatcher'
    if (
      userStore.hasPermission(Perms.WORKORDER_ACCEPT) ||
      userStore.hasPermission(Perms.WORKORDER_REPAIR) ||
      userStore.hasPermission(Perms.WORKORDER_REVIEW)
    ) {
      return 'engineer'
    }
    if (order.transferred) return 'engineer'
    return 'unknown'
  }

  /**
   * 解析当前行应展示的操作按钮（接口 availableActions 优先）
   */
  const getVisibleActions = (order: OrderListItem): WorkOrderVisibleAction[] => {
    const availableActionKeys = getOrderAvailableActions(order)
    let candidateActionKeys = availableActionKeys
    if (!availableActionKeys.length && ENABLE_LEGACY_STATUS_ACTION_FALLBACK) {
      candidateActionKeys = getFallbackStatusActions(order)
      auditLegacyStatusFallbackHit({
        orderId: order.id,
        role: getOrderRoleForRegression(order),
        status: order.status,
        primaryTab: options.primaryTab.value,
        secondaryTab: options.secondaryTab?.value ?? '',
        fallbackActions: candidateActionKeys
      })
    }
    const allowedKeys = candidateActionKeys.filter((key) => isActionAllowed(order, key))
    return toActionButtons(sortWorkOrderActionsForDisplay(allowedKeys))
  }

  return {
    getVisibleActions,
    getOrderAvailableActions,
    isDispatcherOrderAssignedToOther,
    isOrderPendingAssign,
    isOrderPendingTechAccept
  }
}
