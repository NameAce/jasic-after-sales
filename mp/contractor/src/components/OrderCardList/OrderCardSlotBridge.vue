<script lang="ts">
  import { defineComponent, inject, unref } from 'vue'
  import type { OrderListItem } from '@/models/order'
  import {
    orderListParentSlotsKey,
    type OrderListParentSlotBridge
  } from './orderCardListInject'

  /**
   * 调用 OrderCardList 快照的 renderExtra/renderActions；无 template（mp 端由 vite 插件补 .wxml）。
   * slotBridge 以 props 为主：微信小程序等端跨自定义组件 inject 父级 provide 易失效。
   */
  export default defineComponent({
    name: 'OrderCardSlotBridge',
    props: {
      order: {
        type: Object as () => OrderListItem,
        required: true
      },
      bridgeKind: {
        type: String as () => 'extra' | 'actions',
        required: true
      },
      /** 由 OrderCardList 经 props 传入；小程序端仅靠 inject 常拿不到父组件 provide */
      slotBridge: {
        type: Object as () => OrderListParentSlotBridge | undefined,
        default: undefined
      }
    },
    setup(props) {
      const injected = inject(orderListParentSlotsKey, undefined)
      return () => {
        const bridge = props.slotBridge ?? unref(injected)
        const fn =
          props.bridgeKind === 'extra' ? bridge?.renderExtra : bridge?.renderActions
        if (typeof fn !== 'function') return null
        return fn(props.order)
      }
    }
  })
</script>
