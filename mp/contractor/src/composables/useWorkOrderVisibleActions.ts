import { type ComputedRef, type Ref } from 'vue'
import { useUserStore } from '@/stores'
import type { OrderListItem } from '@/models/order'
import {
  getContractorListActionClassName,
  getContractorListActionLabel,
  normalizeAvailableActions,
  sortWorkOrderActionsForDisplay,
  type WorkOrderActionKey
} from '@/constants/orderActions'
import { canCurrentSiteOperateTransferredOrder } from '@/utils/orderTransfer'

/**
 * 列表/工作台行内操作按钮
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type WorkOrderVisibleAction = {
  key: WorkOrderActionKey
  label: string
  className: 'primary' | 'outline'
}

export type WorkOrderVisibleActionsPrimaryTab = 'untransferred' | 'transferred'

export type UseWorkOrderVisibleActionsOptions = {
  /**
 * 一级 Tab：已转单不展示操作按钮
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  primaryTab: Ref<WorkOrderVisibleActionsPrimaryTab> | ComputedRef<WorkOrderVisibleActionsPrimaryTab>
  /**
 * 受理方是否为当前登录主体公司
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  isOrderAcceptedByCurrentCompany: (order: OrderListItem) => boolean
}

/**
 * 工单列表/工作台行内按钮：仅依据接口 `availableActions` 渲染。
 * 前端仅做承修方必守的展示层过滤（已转单 Tab、受理主体、转单网点约束等）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useWorkOrderVisibleActions(options: UseWorkOrderVisibleActionsOptions) {
  const userStore = useUserStore()

  /**
   * 从列表项解析可展示动作（与后端 `WorkOrderPermissionService.listAvailableActions` 对齐）。
   * - 仅 `viewScope=CURRENT` 时列表接口会填充 `availableActions`；
   * - `RETURN_METHOD` 在 normalize 阶段映射为 `CLOSE`（列表统一展示「机器返回方式」）；
   * - 承修方小程序列表暂不展示「上传寄件单号」。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const getOrderAvailableActions = (order: OrderListItem): WorkOrderActionKey[] => {
    const normalized = normalizeAvailableActions(order.availableActions).filter(
      (key) => key !== 'UPLOAD_SEND_EXPRESS'
    )
    return sortWorkOrderActionsForDisplay(normalized)
  }

  /**
   * 展示层约束：后端已做权限与实例校验，此处仅保留小程序必守规则
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const isActionAllowed = (order: OrderListItem, actionKey: WorkOrderActionKey) => {
    if (options.primaryTab.value === 'transferred') return false
    if (!options.isOrderAcceptedByCurrentCompany(order)) return false

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
   * 解析当前行应展示的操作按钮（与后端 `DETAIL_ACTION_ORDER` 一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const getVisibleActions = (order: OrderListItem): WorkOrderVisibleAction[] => {
    const allowedKeys = getOrderAvailableActions(order).filter((key) =>
      isActionAllowed(order, key)
    )
    return toActionButtons(allowedKeys)
  }

  /**
 * 当前行是否包含指定动作（用于点击前二次校验，避免列表刷新滞后）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const hasVisibleAction = (order: OrderListItem, actionKey: WorkOrderActionKey) =>
    getOrderAvailableActions(order).some(
      (key) => key === actionKey && isActionAllowed(order, key)
    )

  return {
    getVisibleActions,
    getOrderAvailableActions,
    hasVisibleAction
  }
}
