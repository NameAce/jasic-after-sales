<template>
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
      <view v-if="hasVal(base.orderTypeName)" class="info-item">
        <text class="info-label">工单类型</text>
        <view :class="['tag-value', orderTypeTagClass]">{{ base.orderTypeName }}</view>
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

  const orderTypeTagClass = computed(() => getOrderTypeTagClass(props.base.orderTypeName))
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';
</style>
