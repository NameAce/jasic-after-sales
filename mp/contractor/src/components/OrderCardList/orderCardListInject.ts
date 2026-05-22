import type { InjectionKey, Ref } from 'vue'
import type { OrderListItem } from '@/models/order'

/**
 * 在 OrderCardList setup 里从 useSlots() 快照出的可调用包装，经 props 传给 OrderCardListFragment → OrderCardSlotBridge。
 * 小程序端深层 inject 易失效；插槽项在部分端也可能非 function，需 normalize（见 OrderCardList.vue）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export type OrderListParentSlotBridge = {
  hasExtra: boolean
  hasActions: boolean
  renderExtra?: (order: OrderListItem) => unknown
  renderActions?: (order: OrderListItem) => unknown
}

/**
 * provide 侧为 ref，inject 后需 unref（见 OrderCardSlotBridge）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export const orderListParentSlotsKey: InjectionKey<Ref<OrderListParentSlotBridge>> =
  Symbol('orderListParentSlots')
