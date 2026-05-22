<template>
  <view :class="['order-card', cardClass]" @tap="emit('order-click', order)">
    <!-- 工单卡片头部 -->
    <view class="card-header">
      <view class="order-no">
        <text class="value">{{ order.orderNo || order.id }}</text>
      </view>
      <view
        :class="[
          'status-badge',
          statusBadgeClass(order),
          isPendingTechAcceptBadge(order) ? 'status-badge--pending-tech-accept' : ''
        ]"
      >
        <text class="text">{{ statusText(order) }}</text>
      </view>
    </view>

    <!-- 工单卡片标签 -->
    <view class="tags-wrap">
      <view
        v-if="orderTypeTagLabel(order)"
        :class="['tag', orderTypeTagStyleClass(order)]"
      >
        <text class="text">{{ orderTypeTagLabel(order) }}</text>
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
      <template v-if="useExpandedRepairInfo(order)">
        <view v-if="showRepairSiteRows && hasText(order.siteName)" class="info-item">
          <text class="label">网点名称</text>
          <text class="value">{{ order.siteName }}</text>
        </view>
        <view v-if="showRepairSiteRows && hasText(order.sitePhone)" class="info-item">
          <text class="label">网点联系电话</text>
          <text class="value primary">{{ order.sitePhone }}</text>
        </view>
        <view v-if="hasText(order.repairMethodLabel)" class="info-item">
          <text class="label">维修方式</text>
          <text class="value">{{ order.repairMethodLabel }}</text>
        </view>
        <view v-if="hasRepairPrice(order)" class="info-item">
          <text class="label">维修价格</text>
          <text class="value value-repair-price">{{ repairPriceDisplay(order) }}</text>
        </view>
        <view v-if="hasText(order.phone)" class="info-item">
          <text class="label">客户联系方式</text>
          <text class="value primary">{{ order.phone }}</text>
        </view>
        <view v-if="order.isJiashi && hasText(order.barcode)" class="info-item">
          <text class="label">条码</text>
          <text class="value">{{ order.barcode }}</text>
        </view>
        <view v-if="hasText(order.model)" class="info-item">
          <text class="label">机器型号</text>
          <text class="value">{{ order.model }}</text>
        </view>
      </template>
      <template v-else>
        <view v-if="hasText(order.phone)" class="info-item">
          <text class="label">联系电话</text>
          <text class="value primary">{{ order.phone }}</text>
        </view>
        <view v-if="order.isJiashi && order.barcode" class="info-item">
          <text class="label">机器条码</text>
          <text class="value">{{ order.barcode }}</text>
        </view>
        <view v-if="order.model" class="info-item">
          <text class="label">机器型号</text>
          <text class="value">{{ order.model }}</text>
        </view>
        <view v-if="hasText(order.repairMethodLabel)" class="info-item">
          <text class="label">维修方式</text>
          <text class="value">{{ order.repairMethodLabel }}</text>
        </view>
      </template>
      <view v-if="order.outDate" class="info-item">
        <text class="label">最后出库日期</text>
        <text class="value">{{ order.outDate }}</text>
      </view>
      <view v-if="order.outDate && hasText(order.warrantyText)" class="info-item">
        <text class="label">质保判定</text>
        <view :class="['tag-value', listWarrantyTagClass(order)]">{{
          (order.warrantyText ?? '').trim()
        }}</view>
      </view>
      <slot name="extra-info" :order="order"></slot>
    </view>

    <view v-if="orderFaultDescText(order)" class="card-desc">
      <text class="text">
        <text class="label">故障描述：</text>
        {{ orderFaultDescText(order) }}
      </text>
    </view>

    <view class="card-footer">
      <view v-if="orderSubmitTimeText(order)" class="time-wrap">
        <uni-icons type="calendar-filled" size="16" color="#94a3b8"></uni-icons>
        <text class="time-text">{{ orderSubmitTimeText(order) }}</text>
      </view>
      <view class="card-footer-actions">
        <slot name="actions" :order="order"></slot>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import type { OrderListItem } from '@/models/order'
  import { formatIsoDateTime } from '@/utils/format'
  import { getWarrantyTagClass } from '@/utils/orderTags'

  type OrderPredicate = (order: OrderListItem) => boolean

  const props = withDefaults(
    defineProps<{
      order: OrderListItem
      statusText: (order: OrderListItem) => string
      showInboundTransferTag?: OrderPredicate
      showTransferredTag?: OrderPredicate
      cardClass?: string
      /**
 * 维修中/已完成/已关闭是否展示网点名称、网点联系电话
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      showRepairSiteRows?: boolean
    }>(),
    {
      showInboundTransferTag: () => false,
      showTransferredTag: (order: OrderListItem) => !!order.transferred,
      cardClass: '',
      showRepairSiteRows: false
    }
  )

  const emit = defineEmits<{
    (e: 'order-click', order: OrderListItem): void
  }>()

  const orderFaultDescText = (order: OrderListItem) => {
    const a = (order.faultDesc ?? '').trim()
    if (a) return a
    return (order.desc ?? '').trim()
  }

  /**
 * 与详情「工单类型」一致：仅展示接口 `brandTypeLabel`，无则不占位
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const orderTypeTagLabel = (order: OrderListItem) => (order.brandTypeLabel ?? '').trim()

  const orderTypeTagStyleClass = (order: OrderListItem) => {
    const label = orderTypeTagLabel(order)
    if (!label) return 'tag-brand'
    return /非佳士/.test(label) ? 'tag-other-brand' : 'tag-brand'
  }

  const statusText = (order: OrderListItem) => props.statusText(order)

  const statusBadgeClass = (order: OrderListItem) => {
    if (order.status === 'PENDING_ASSIGN' || order.status === 'PENDING_TECH_ACCEPT')
      return 'pending'
    if (order.status === 'IN_PROGRESS') return 'processing'
    if (order.status === 'COMPLETED') return 'completed'
    if (order.status === 'CLOSED') return 'closed'
    return 'pending'
  }

  const isPendingTechAcceptBadge = (order: OrderListItem) => statusText(order) === '待接单'

  const listWarrantyTagClass = (order: OrderListItem) =>
    getWarrantyTagClass((order.warrantyText ?? '').trim())

  const showInboundTransferTag = (order: OrderListItem) => props.showInboundTransferTag(order)
  const showTransferredTag = (order: OrderListItem) => props.showTransferredTag(order)

  const useExpandedRepairInfo = (order: OrderListItem) =>
    order.status === 'IN_PROGRESS' || order.status === 'COMPLETED' || order.status === 'CLOSED'

  const hasText = (raw?: string | null) => !!(raw ?? '').trim()

  const hasRepairPrice = (order: OrderListItem) => !!(order.repairPriceText ?? '').trim()

  const repairPriceDisplay = (order: OrderListItem) => {
    const p = (order.repairPriceText ?? '').trim()
    return p ? `¥ ${p}` : ''
  }

  const orderSubmitTimeText = (order: OrderListItem) => formatIsoDateTime(order.createTime)
</script>

<style lang="scss" scoped>
  @use '@/styles/variables.scss' as *;
  @use '@/styles/mixins.scss' as *;

  /* 与详情页「质保判定」一致：保内绿 tag / 保外红 tag / 其它灰 */
  .tag-value {
    padding: 4rpx $space-sm;
    font-size: $font-sm;
    font-weight: 500;
    border-radius: $radius-sm;
    line-height: 1.4;
    align-self: flex-start;
  }

  .tag-value-neutral {
    @include surface-muted;
    color: $text-slate-500;
  }

  .tag-warranty-green {
    background-color: rgba($status-completed-text, 0.14);
    color: $status-completed-text;
  }

  .tag-warranty-red {
    background-color: rgba($red-600, 0.14);
    color: $red-600;
  }
</style>
