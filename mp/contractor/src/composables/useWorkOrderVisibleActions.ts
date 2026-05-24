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
 * @修改时间 2026-05-24
 */
export type WorkOrderVisibleAction = {
  key: WorkOrderActionKey
  label: string
  className: 'primary' | 'outline'
}

export type WorkOrderVisibleActionsPrimaryTab = 'untransferred' | 'transferred'

export type UseWorkOrderVisibleActionsOptions = {
  /**
   * 一级 Tab：当前处理走展示层过滤；历史转出按接口 availableActions 渲染
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  primaryTab: Ref<WorkOrderVisibleActionsPrimaryTab> | ComputedRef<WorkOrderVisibleActionsPrimaryTab>
  /**
   * 受理方是否为当前登录主体公司（仅当前处理 Tab 生效）
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  isOrderAcceptedByCurrentCompany: (order: OrderListItem) => boolean
  /**
   * 是否为历史转出列表视图（非总部 + 历史转出 Tab）
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  isHistoryListView?: Ref<boolean> | ComputedRef<boolean>
}

/**
 * 工单列表/工作台行内按钮：仅依据接口 `availableActions` 渲染。
 * 当前处理 Tab 保留承修方必守的展示层过滤；历史转出 Tab 信任后端权限结果。
 * @修改人 黄碧莲
 * @修改时间 2026-05-24
 */
export function useWorkOrderVisibleActions(options: UseWorkOrderVisibleActionsOptions) {
  const userStore = useUserStore()

  /**
   * 是否处于历史转出列表（viewScope=HISTORY）
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const isHistoryListView = () => options.isHistoryListView?.value === true

  /**
   * 从列表项解析可展示动作（与后端 `WorkOrderPermissionService.listAvailableActions` 对齐）。
   * - `viewScope=CURRENT` / `HISTORY` 时列表接口会填充 `availableActions`；
   * - `RETURN_METHOD` 在 normalize 阶段映射为 `CLOSE`（列表统一展示「机器返回方式」）；
   * - 当前处理列表暂不展示「上传寄件单号」；历史转出保留后端下发的该动作。
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const getOrderAvailableActions = (order: OrderListItem): WorkOrderActionKey[] => {
    let normalized = normalizeAvailableActions(order.availableActions)
    if (!isHistoryListView()) {
      normalized = normalized.filter((key) => key !== 'UPLOAD_SEND_EXPRESS')
    }
    return sortWorkOrderActionsForDisplay(normalized)
  }

  /**
   * 展示层约束：当前处理 Tab 保留小程序必守规则；历史转出 Tab 直接信任后端 availableActions
   * @修改人 黄碧莲
   * @修改时间 2026-05-24
   */
  const isActionAllowed = (order: OrderListItem, actionKey: WorkOrderActionKey) => {
    if (isHistoryListView()) return true
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
   * @修改时间 2026-05-24
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
   * @修改时间 2026-05-24
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
