<template>
  <view class="site-workbench">
    <view class="stats-section">
      <view class="stat-card primary" @tap="emit('stat-tap', 'pending')">
        <text class="stat-label">今日{{ pendingStatLabel }}</text>
        <text class="stat-value">{{ siteWorkbenchStats.pending }}</text>
      </view>
      <view class="stat-card" @tap="emit('stat-tap', 'processing')">
        <text class="stat-label">维修中</text>
        <text class="stat-value">{{ siteWorkbenchStats.processing }}</text>
      </view>
      <view class="stat-card" @tap="emit('stat-tap', 'completed')">
        <text class="stat-label">已完结</text>
        <text class="stat-value">{{
          siteWorkbenchStats.completed + siteWorkbenchStats.closed
        }}</text>
      </view>
    </view>

    <view class="list-header">
      <text class="list-title">{{ workbenchListTitle }}</text>
      <text class="badge">{{ orderList.length }}条</text>
    </view>

    <OrderCardList
      :orders="orderList"
      :status-text="getOrderListStatusText"
      brand-label="佳士品牌"
      other-brand-label="非佳士品牌"
      :empty-title="workbenchEmptyTitle"
      :empty-desc="workbenchEmptyDesc"
      :show-inbound-transfer-tag="showInboundTransferTag"
      :show-transferred-tag="showTransferredTag"
      @order-click="(o) => emit('order-click', o)"
    >
      <template #actions="{ order }">
        <view class="action-wrap">
          <button
            v-if="showAcceptOrderButton(order)"
            class="btn-action primary"
            @tap.stop="emit('accept-order', order.id)"
          >
            一键接单
          </button>
          <button
            v-if="showDispatchOrderButton(order)"
            class="btn-action primary"
            @tap.stop="emit('dispatch-order', order.id)"
          >
            一键派单
          </button>
        </view>
      </template>
    </OrderCardList>
  </view>
</template>

<script setup lang="ts">
  import OrderCardList from '@/components/OrderCardList/OrderCardList.vue'
  import type { OrderListItem } from '@/models/order'

  defineProps<{
    pendingStatLabel: string
    siteWorkbenchStats: {
      pending: number
      processing: number
      completed: number
      closed: number
    }
    workbenchListTitle: string
    workbenchEmptyTitle: string
    workbenchEmptyDesc: string
    orderList: OrderListItem[]
    getOrderListStatusText: (order: OrderListItem) => string
    showAcceptOrderButton: (order: OrderListItem) => boolean
    showDispatchOrderButton: (order: OrderListItem) => boolean
    showInboundTransferTag: (order: OrderListItem) => boolean
    showTransferredTag: (order: OrderListItem) => boolean
  }>()

  const emit = defineEmits<{
    (e: 'stat-tap', tab: 'pending' | 'processing' | 'completed'): void
    (e: 'order-click', order: OrderListItem): void
    (e: 'accept-order', orderId: string): void
    (e: 'dispatch-order', orderId: string): void
  }>()
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .site-workbench .stats-section {
    @include flex-row;
    gap: $space-md;
    padding: $space-md $space-lg;

    .stat-card {
      flex: 1;
      @include flex-col;
      gap: $space-xs;
      border-radius: $radius-lg;
      padding: 18rpx 28rpx;
      background-color: $bg-card;
      border: 2rpx solid $bg-hover;
      box-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.05);

      &.primary {
        background-color: $primary;
        border: none;
        color: $text-bg;

        .stat-label {
          color: rgba(255, 255, 255, 0.8);
        }
        .stat-value {
          color: $text-bg;
        }
      }

      .stat-label {
        font-size: $font-sm;
        font-weight: 500;
        color: $text-slate-500;
      }

      .stat-value {
        font-size: $font-xl;
        font-weight: bold;
        color: $text-slate-900;
      }
    }
  }

  .site-workbench .list-header {
    padding: $space-md 48rpx $space-md 50rpx;
    @include flex-row;
    gap: $space-sm;

    .list-title {
      font-size: $font-lg;
      font-weight: bold;
      color: $text-slate-900;
    }

    .badge {
      background-color: $primary-alpha-10;
      color: $primary;
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: $radius-pill;
      font-weight: bold;
    }
  }
</style>
