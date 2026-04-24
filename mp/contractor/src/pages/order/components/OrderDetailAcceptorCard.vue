<template>
  <view v-if="show" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">受理方信息</text>
    </view>
    <view class="od-info-list">
      <!-- 网点电话：同源 `currentAcceptCompanyPhone`，兼容 `sitePhone` -->
      <view v-if="hasVal(outletPhoneDisplay)" class="info-item">
        <text class="info-label">网点电话</text>
        <text class="info-value">{{ outletPhoneDisplay }}</text>
      </view>
      <view v-if="hasVal(acceptor.currentAcceptCompanyName)" class="info-item align-top">
        <text class="info-label shrink">受理方</text>
        <text class="info-value text-right">{{ acceptor.currentAcceptCompanyName }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    acceptor: OrderDetail['acceptor']
  }>()

  /** 优先详情 `currentAcceptCompanyPhone`，兼容同源的 `sitePhone` */
  const outletPhoneDisplay = computed(() => {
    const a = props.acceptor
    return String(a.currentAcceptCompanyPhone ?? a.sitePhone ?? '').trim()
  })

  const show = computed(() => {
    const a = props.acceptor
    return hasVal(a.currentAcceptCompanyName) || hasVal(outletPhoneDisplay.value)
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
