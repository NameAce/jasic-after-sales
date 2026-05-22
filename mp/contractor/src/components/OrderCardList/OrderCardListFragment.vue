<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）组件 OrderCardList -->
  <OrderCardItem
    :order="order"
    :status-text="statusText"
    :show-inbound-transfer-tag="showInboundTransferTag"
    :show-transferred-tag="showTransferredTag"
    :card-class="cardClass"
    @order-click="emit('order-click', $event)"
  >
    <template #extra-info="{ order: o }">
      <OrderCardSlotBridge
        v-if="slotBridge?.renderExtra"
        :slot-bridge="slotBridge"
        :order="o"
        bridge-kind="extra"
      />
    </template>
    <template #actions="{ order: o }">
      <OrderCardSlotBridge
        v-if="slotBridge?.renderActions"
        :slot-bridge="slotBridge"
        :order="o"
        bridge-kind="actions"
      />
    </template>
  </OrderCardItem>
</template>

<script setup lang="ts">
  import OrderCardItem from './OrderCardItem.vue'
  import OrderCardSlotBridge from './OrderCardSlotBridge.vue'
  import type { OrderListParentSlotBridge } from './orderCardListInject'
  import type { OrderListItem } from '@/models/order'

  type OrderPredicate = (order: OrderListItem) => boolean
  // 定义 props
  defineProps<{
    order: OrderListItem
    slotBridge: OrderListParentSlotBridge
    statusText: (order: OrderListItem) => string
    showInboundTransferTag?: OrderPredicate
    showTransferredTag?: OrderPredicate
    cardClass?: string
  }>()
  /**
   * 定义 emits
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const emit = defineEmits<{
    (e: 'order-click', order: OrderListItem): void
  }>()
</script>
