<template>
  <!-- 工单列表容器 -->
  <view class="list-container">
    <!-- 空状态 -->
    <slot v-if="orders.length === 0" name="empty">
      <ListEmpty :title="emptyTitle" :desc="emptyDesc" />
    </slot>
    <!-- 每行独立 OrderCardItem，避免小程序同一组件内重复具名插槽 -->
    <template v-else>
      <OrderCardItem
        v-for="order in orders"
        :key="order.id"
        :order="order"
        :status-text="statusText"
        :show-inbound-transfer-tag="showInboundTransferTag"
        :show-transferred-tag="showTransferredTag"
        :show-repair-site-rows="showRepairSiteRows"
        :card-class="cardClass"
        @order-click="emit('order-click', $event)"
      >
        <template #extra-info="{ order: o }">
          <slot name="extra-info" :order="o"></slot>
        </template>
        <template #actions="{ order: o }">
          <slot name="actions" :order="o"></slot>
        </template>
      </OrderCardItem>

      <ListNoMore v-if="showNoMore" :text="noMoreText" />
    </template>
  </view>
</template>

<script setup lang="ts">
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import OrderCardItem from './OrderCardItem.vue'
  import type { OrderListItem } from '@/models/order'

  type OrderPredicate = (order: OrderListItem) => boolean

  const props = withDefaults(
    defineProps<{
      orders: OrderListItem[]
      statusText: (order: OrderListItem) => string
      emptyTitle?: string
      emptyDesc?: string
      showInboundTransferTag?: OrderPredicate
      showTransferredTag?: OrderPredicate
      cardClass?: string
      showNoMore?: boolean
      noMoreText?: string
      showRepairSiteRows?: boolean
    }>(),
    {
      emptyTitle: '暂无工单',
      emptyDesc: '当前筛选条件下没有工单',
      showInboundTransferTag: () => false,
      showTransferredTag: (order: OrderListItem) => !!order.transferred,
      cardClass: '',
      showNoMore: false,
      noMoreText: '没有更多了',
      showRepairSiteRows: false
    }
  )

  const emit = defineEmits<{
    (e: 'order-click', order: OrderListItem): void
  }>()

  const statusText = (order: OrderListItem) => props.statusText(order)
</script>
