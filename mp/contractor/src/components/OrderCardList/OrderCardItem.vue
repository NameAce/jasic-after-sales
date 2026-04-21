<template>
  <view :class="['order-card', cardClass]" @tap="emit('order-click', order)">
    <view class="card-header">
      <view class="order-no">
        <text class="value">{{ order.orderNo || order.id }}</text>
      </view>
      <view :class="['status-badge', statusBadgeClass(order)]">
        <text class="text">{{ statusText(order) }}</text>
      </view>
    </view>
    <!-- 标签 -->
    <view class="tags-wrap">
      <view :class="['tag', order.isJiashi ? 'tag-brand' : 'tag-other-brand']">
        <text class="text">{{ order.isJiashi ? brandLabel : otherBrandLabel }}</text>
      </view>
      <view v-if="order.warrantyText" :class="['tag', order.warrantyClass]">
        <text class="text">{{ order.warrantyText }}</text>
      </view>
      <view v-if="showInboundTransferTag(order)" class="tag tag-transfer-in">
        <text class="text">转单</text>
      </view>
      <view v-if="showTransferredTag(order)" class="tag tag-out-warranty">
        <text class="text">已转单</text>
      </view>
    </view>

    <!-- 内容 -->
    <view class="card-body">
      <view class="info-item">
        <text class="label">联系电话</text>
        <text class="value primary">{{ order.phone }}</text>
      </view>
      <view v-if="order.barcode" class="info-item">
        <text class="label">机器条码</text>
        <text class="value">{{ order.barcode }}</text>
      </view>
      <view v-if="order.model" class="info-item">
        <text class="label">机器型号</text>
        <text class="value">{{ order.model }}</text>
      </view>
      <view v-if="order.outDate" class="info-item">
        <text class="label">最后出库日期</text>
        <text class="value">{{ order.outDate }}</text>
      </view>
      <slot name="extra-info" :order="order"></slot>
    </view>

    <!-- 描述 -->
    <view class="card-desc">
      <text class="text"><text class="label">故障描述：</text>{{ order.desc }}</text>
    </view>

    <view class="card-footer">
      <slot name="actions" :order="order"></slot>
    </view>
  </view>
</template>

<script setup lang="ts">
  import type { OrderListItem } from '@/models/order'
  // 定义类型
  type OrderPredicate = (order: OrderListItem) => boolean
  /**
   * 定义 props
   */
  const props = withDefaults(
    defineProps<{
      order: OrderListItem
      statusText: (order: OrderListItem) => string
      brandLabel?: string
      otherBrandLabel?: string
      showInboundTransferTag?: OrderPredicate
      showTransferredTag?: OrderPredicate
      cardClass?: string
    }>(),
    {
      brandLabel: '佳士',
      otherBrandLabel: '非佳士',
      showInboundTransferTag: () => false,
      showTransferredTag: (order: OrderListItem) => !!order.transferred,
      cardClass: ''
    }
  )

  /**
   * 定义 emits
   */
  const emit = defineEmits<{
    (e: 'order-click', order: OrderListItem): void
  }>()

  // 定义状态文本
  const statusText = (order: OrderListItem) => props.statusText(order)
  /** 将接口主状态映射到样式类：pending / processing / completed / closed */
  const statusBadgeClass = (order: OrderListItem) => {
    if (order.status === 'PENDING_ASSIGN' || order.status === 'PENDING_TECH_ACCEPT') return 'pending'
    if (order.status === 'IN_PROGRESS') return 'processing'
    if (order.status === 'COMPLETED') return 'completed'
    if (order.status === 'CLOSED') return 'closed'
    return 'pending'
  }
  // 定义转单标签
  const showInboundTransferTag = (order: OrderListItem) => props.showInboundTransferTag(order)
  // 定义已转单标签
  const showTransferredTag = (order: OrderListItem) => props.showTransferredTag(order)
</script>
