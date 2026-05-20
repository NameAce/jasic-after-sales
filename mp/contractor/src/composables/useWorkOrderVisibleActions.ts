import { type ComputedRef, type Ref } from 'vue'
import { useUserStore } from '@/stores'
import type { OrderListItem } from '@/models/order'
import {
  getContractorListActionClassName,
  getContractorListActionLabel,
  normalizeAvailableActions,
  type WorkOrderActionKey
} from '@/constants/orderActions'
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
  /** 受理方是否为当前登录主体公司 */
  isOrderAcceptedByCurrentCompany: (order: OrderListItem) => boolean
}

/**
 * 工单列表/工作台行内按钮：仅依据接口 `availableActions` 渲染。
 * 前端仅做承修方必守的展示层过滤（已转单 Tab、受理主体、指派他人、转单网点约束等）。
 */
export function useWorkOrderVisibleActions(options: UseWorkOrderVisibleActionsOptions) {
  const userStore = useUserStore()

  const isOrderPendingTechAccept = (order: OrderListItem) => order.status === 'PENDING_TECH_ACCEPT'

  const isOrderPendingAssign = (order: OrderListItem) =>
    order.status === 'PENDING_ASSIGN' && !isWorkOrderPendingTechAcceptMainStatus(order.mainStatus)

  /**
   * 派单员：工单已指派给他人时仅可查看（防止接口异常时误展示按钮）
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
   * 展示层约束：后端已做权限与实例校验，此处仅保留小程序必守规则
   */
  const isActionAllowed = (order: OrderListItem, actionKey: WorkOrderActionKey) => {
    if (options.primaryTab.value === 'transferred') return false
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
      label: getContractorListActionLabel(key),
      className: getContractorListActionClassName(key)
    }))

  /**
   * 解析当前行应展示的操作按钮（严格按接口 `availableActions` 顺序）
   */
  const getVisibleActions = (order: OrderListItem): WorkOrderVisibleAction[] => {
    const allowedKeys = getOrderAvailableActions(order).filter((key) =>
      isActionAllowed(order, key)
    )
    return toActionButtons(allowedKeys)
  }

  return {
    getVisibleActions,
    getOrderAvailableActions,
    isDispatcherOrderAssignedToOther,
    isOrderPendingAssign,
    isOrderPendingTechAccept
  }
}
