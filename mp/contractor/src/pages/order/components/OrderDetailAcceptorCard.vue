<template>
  <view v-if="show" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">受理方信息</text>
    </view>
    <view class="od-info-list">
      <view v-if="hasVal(acceptor.sitePhone)" class="info-item">
        <text class="info-label">网点电话</text>
        <text class="info-value">{{ acceptor.sitePhone }}</text>
      </view>
      <view v-if="hasVal(acceptor.acceptorName)" class="info-item align-top">
        <text class="info-label shrink">受理方</text>
        <text class="info-value text-right">{{ acceptor.acceptorName }}</text>
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

  const show = computed(() => {
    const a = props.acceptor
    return hasVal(a.sitePhone) || hasVal(a.acceptorName)
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
