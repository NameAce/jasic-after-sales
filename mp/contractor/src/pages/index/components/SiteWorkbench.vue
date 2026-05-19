<template>
  <view class="site-workbench">
    <view class="stats-section">
      <view class="stat-card primary" @tap="emit('stat-tap', 'pending')">
        <text class="stat-label">{{ primaryPendingStat.label }}</text>
        <text class="stat-value">{{ primaryPendingStat.count }}</text>
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
    </view>

    <OrderCardList
      :orders="orderList"
      :status-text="getOrderListStatusText"
      :empty-title="workbenchEmptyTitle"
      :empty-desc="workbenchEmptyDesc"
      :show-no-more="showNoMore"
      :show-inbound-transfer-tag="showInboundTransferTag"
      :show-transferred-tag="showTransferredTag"
      @order-click="(o) => emit('order-click', o)"
    >
      <template #actions="{ order }">
        <view v-if="getVisibleActions(order).length > 0" class="action-wrap">
          <button
            v-for="action in getVisibleActions(order)"
            :key="`${order.id}-${action.key}`"
            :class="`btn-action ${action.className}`"
            @tap.stop="emit('work-order-action', action.key, order.id)"
          >
            {{ action.label }}
          </button>
        </view>
      </template>
    </OrderCardList>
  </view>
</template>

<script setup lang="ts">
  import OrderCardList from '@/components/OrderCardList/OrderCardList.vue'
  import type { OrderListItem } from '@/models/order'
  import type { WorkOrderActionKey } from '@/constants/orderActions'
  import type { WorkOrderVisibleAction } from '@/composables/useWorkOrderVisibleActions'

  const props = withDefaults(
    defineProps<{
      /** 首卡：与接口 `status-count` 中 PENDING_ASSIGN / PENDING_TECH_ACCEPT 行的 displayStatus、countNum 一致 */
      primaryPendingStat: { label: string; count: number }
      /**
       * 来自 `GET /api/system/work-order/status-count`（`viewScope: CURRENT`），
       * 父级 `useIndexWorkbench` 内 `aggregateWorkOrderStatusTabCounts` 聚合后与列表一并刷新。
       */
      siteWorkbenchStats: {
        pendingAssign: number
        pendingTechAccept: number
        processing: number
        completed: number
        closed: number
      }
      workbenchListTitle: string
      workbenchEmptyTitle: string
      workbenchEmptyDesc: string
      /** 工单列表已加载完全部分页数据时展示「没有更多了」 */
      showNoMore?: boolean
      orderList: OrderListItem[]
      getOrderListStatusText: (order: OrderListItem) => string
      getVisibleActions: (order: OrderListItem) => WorkOrderVisibleAction[]
      showInboundTransferTag: (order: OrderListItem) => boolean
      showTransferredTag: (order: OrderListItem) => boolean
    }>(),
    { showNoMore: false }
  )

  const emit = defineEmits<{
    (e: 'stat-tap', tab: 'pending' | 'processing' | 'completed'): void
    (e: 'order-click', order: OrderListItem): void
    (e: 'work-order-action', actionKey: WorkOrderActionKey, orderId: string): void
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
  }
</style>
