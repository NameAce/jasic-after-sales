<template>
  <!-- 工单列表容器 -->
  <view class="list-container">
    <!-- 空状态 -->
    <slot v-if="orders.length === 0" name="empty">
      <ListEmpty :title="emptyTitle" :desc="emptyDesc" />
    </slot>
    <!-- 工单列表 -->
    <template v-else>
      <view
        v-for="order in orders"
        :key="order.id"
        :class="['order-card', cardClass]"
        @tap="emit('order-click', order)"
      >
        <!-- 工单卡片头部 -->
        <view class="card-header">
          <view class="order-no">
            <text class="value">{{ order.orderNo || order.id }}</text>
          </view>
          <view :class="['status-badge', order.status]">
            <text class="text">{{ statusText(order) }}</text>
          </view>
        </view>

        <!-- 工单卡片标签 -->
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

        <!-- 工单卡片内容 -->
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

        <!-- 工单卡片描述 -->
        <view class="card-desc">
          <text class="text"><text class="label">故障描述：</text>{{ order.desc }}</text>
        </view>

        <!-- 工单卡片底部 -->
        <view class="card-footer">
          <slot name="actions" :order="order"></slot>
        </view>
      </view>

      <ListNoMore v-if="showNoMore" :text="noMoreText" />
    </template>
  </view>
</template>

<script setup lang="ts">
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import type { OrderListItem } from '@/models/order'
  // 工单状态文本
  type OrderPredicate = (order: OrderListItem) => boolean

  // 工单卡片属性
  const props = withDefaults(
    defineProps<{
      orders: OrderListItem[]
      statusText: (order: OrderListItem) => string
      emptyTitle?: string
      emptyDesc?: string
      brandLabel?: string
      otherBrandLabel?: string
      showInboundTransferTag?: OrderPredicate
      showTransferredTag?: OrderPredicate
      cardClass?: string
      showNoMore?: boolean
      noMoreText?: string
    }>(),
    {
      emptyTitle: '暂无工单',
      emptyDesc: '当前筛选条件下没有工单',
      brandLabel: '佳士',
      otherBrandLabel: '非佳士',
      showInboundTransferTag: () => false,
      showTransferredTag: (order: OrderListItem) => !!order.transferred,
      cardClass: '',
      showNoMore: false,
      noMoreText: '没有更多了'
    }
  )

  // 工单卡片事件
  const emit = defineEmits<{
    (e: 'order-click', order: OrderListItem): void
  }>()

  // 工单状态文本
  const statusText = (order: OrderListItem) => props.statusText(order)
  // 是否展示转单标记
  const showInboundTransferTag = (order: OrderListItem) => props.showInboundTransferTag(order)
  // 是否展示已转单标记
  const showTransferredTag = (order: OrderListItem) => props.showTransferredTag(order)
  // 工单卡片类
  const cardClass = props.cardClass
</script>
