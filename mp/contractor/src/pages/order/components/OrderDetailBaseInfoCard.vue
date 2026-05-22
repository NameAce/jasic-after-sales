<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）页面 order / components / OrderDetailBaseInfoCard -->
  <view>
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">工单信息</text>
    </view>
    <view class="od-apply-info-list">
      <view v-if="hasVal(base.orderNo)" class="info-item">
        <text class="info-label">工单编号</text>
        <text class="info-value">{{ base.orderNo }}</text>
      </view>
      <view v-if="hasVal(base.brandTypeLabel)" class="info-item">
        <text class="info-label">工单类型</text>
        <view :class="['tag-value', orderTypeTagClass]">{{ base.brandTypeLabel }}</view>
      </view>
      <view v-if="hasVal(base.submitTime)" class="info-item">
        <text class="info-label">提交时间</text>
        <text class="info-value">{{ base.submitTime }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { getOrderTypeTagClass } from '@/utils/orderTags'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    base: OrderDetail['base']
  }>()

  const orderTypeTagClass = computed(() => getOrderTypeTagClass(props.base.brandTypeLabel))
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';
  @use '@/styles/variables.scss' as *;
  @use '@/styles/mixins.scss' as *;

  /**
   * 工单信息「工单类型」取值：与 `orderDetailApplyCards.scss` 中 `.info-item` 下规则一致。
   * 须在本组件顶层再写一遍，否则 `scoped` + `@use` 过深时微信小程序可能不命中（同 OrderDetailRepairMetaCard 故障角标处理）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  .tag-value {
    padding: 4rpx $space-sm;
    font-size: $font-sm;
    font-weight: 500;
    border-radius: $radius-sm;
    line-height: 1.4;
  }

  .tag-value-neutral {
    @include surface-muted;
    color: $text-slate-500;
  }

  .tag-order-type-orange {
    background-color: $primary-alpha-14;
    color: $tag-brand-text;
  }

  .tag-order-type-gray {
    background-color: rgba($text-slate-500, 0.14);
    color: $text-slate-500;
  }
</style>
