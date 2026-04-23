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
          <view :class="['tag', order.isJiashi ? 'tag-brand' : 'tag-other-brand']">
            <text class="text">{{ orderTypeTagText(order) }}</text>
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
          <!-- 维修中/已完成/已关闭：按一级 Tab（未转单/已转单）展示维修信息与客户/条码/型号 -->
          <template v-if="useExpandedRepairInfo(order)">
            <view v-if="showRepairSiteRows && hasText(order.siteName)" class="info-item">
              <text class="label">维修网点</text>
              <text class="value">{{ order.siteName }}</text>
            </view>
            <view v-if="hasText(order.repairMethodLabel)" class="info-item">
              <text class="label">维修方式</text>
              <text class="value">{{ order.repairMethodLabel }}</text>
            </view>
            <view v-if="hasRepairPrice(order)" class="info-item">
              <text class="label">维修价格</text>
              <text class="value">{{ repairPriceDisplay(order) }}</text>
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
          <!-- 与详情一致：有出库日期且有质保文案时才展示质保判定 -->
          <view v-if="order.outDate && hasText(order.warrantyText)" class="info-item">
            <text class="label">质保判定</text>
            <text :class="['value', listWarrantyBodyClass(order)]">{{
              (order.warrantyText ?? '').trim()
            }}</text>
          </view>
          <slot name="extra-info" :order="order"></slot>
        </view>

        <!-- 工单卡片描述（无故障描述时不展示整块） -->
        <view v-if="orderFaultDescText(order)" class="card-desc">
          <text class="text">
            <text class="label">故障描述：</text>
            {{ orderFaultDescText(order) }}
          </text>
        </view>

        <!-- 工单卡片底部：提交时间（标签 + 时间，在按钮上一行靠左）+ 操作区 -->
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
      /**
       * 与工单列表一级 Tab「已转单」一致：维修中/已完成/已关闭卡片额外展示维修网点、网点电话。
       * 未转单 Tab 传 false。
       */
      showRepairSiteRows?: boolean
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
      noMoreText: '没有更多了',
      showRepairSiteRows: false
    }
  )

  // 工单卡片事件
  const emit = defineEmits<{
    (e: 'order-click', order: OrderListItem): void
  }>()

  /** 列表卡片故障描述：faultDesc 优先，否则 desc；无有效内容返回空串（整块不展示） */
  const orderFaultDescText = (order: OrderListItem) => {
    const a = (order.faultDesc ?? '').trim()
    if (a) return a
    return (order.desc ?? '').trim()
  }

  /** 工单类型文案：优先接口 brandTypeLabel，否则按 isJiashi 用默认「佳士/非佳士」文案 */
  const orderTypeTagText = (order: OrderListItem) => {
    const label = (order.brandTypeLabel ?? '').trim()
    if (label) return label
    return order.isJiashi ? props.brandLabel : props.otherBrandLabel
  }

  // 工单状态文本
  const statusText = (order: OrderListItem) => props.statusText(order)
  /** 将接口主状态映射到样式类：pending / processing / completed / closed */
  const statusBadgeClass = (order: OrderListItem) => {
    if (order.status === 'PENDING_ASSIGN' || order.status === 'PENDING_TECH_ACCEPT')
      return 'pending'
    if (order.status === 'IN_PROGRESS') return 'processing'
    if (order.status === 'COMPLETED') return 'completed'
    if (order.status === 'CLOSED') return 'closed'
    return 'pending'
  }
  /** 与列表页一致：展示文案为「待接单」时用红色角标（含派单员/工程师等自定义 statusText） */
  const isPendingTechAcceptBadge = (order: OrderListItem) => statusText(order) === '待接单'

  /** 正文「质保判定」数值颜色：保内绿 / 保外红 / 其它中性 */
  const listWarrantyBodyClass = (order: OrderListItem) => {
    const t = (order.warrantyText ?? '').trim()
    if (!t) return 'value-warranty--neutral'
    if (/保内/.test(t)) return 'value-warranty--in'
    if (/保外/.test(t)) return 'value-warranty--out'
    return 'value-warranty--neutral'
  }

  // 是否展示转单标记
  const showInboundTransferTag = (order: OrderListItem) => props.showInboundTransferTag(order)
  // 是否展示已转单标记
  const showTransferredTag = (order: OrderListItem) => props.showTransferredTag(order)
  // 工单卡片类
  const cardClass = props.cardClass

  /** 维修中 / 已完成 / 已关闭：使用扩展信息区（与列表一级 Tab 未转单/已转单组合） */
  const useExpandedRepairInfo = (order: OrderListItem) =>
    order.status === 'IN_PROGRESS' || order.status === 'COMPLETED' || order.status === 'CLOSED'

  const hasText = (raw?: string | null) => !!(raw ?? '').trim()

  const hasRepairPrice = (order: OrderListItem) => !!(order.repairPriceText ?? '').trim()

  const repairPriceDisplay = (order: OrderListItem) => {
    const p = (order.repairPriceText ?? '').trim()
    return p ? `¥ ${p}` : ''
  }

  /** 列表底部「提交时间」展示为列表项 `createTime` */
  const orderSubmitTimeText = (order: OrderListItem) => (order.createTime ?? '').trim()
</script>

<style lang="scss" scoped>
  @use '@/styles/variables.scss' as *;

  .value-warranty--in {
    color: $status-completed-text;
    font-weight: 600;
  }

  .value-warranty--out {
    color: $red-600;
    font-weight: 600;
  }

  .value-warranty--neutral {
    color: $text-slate-500;
  }
</style>
