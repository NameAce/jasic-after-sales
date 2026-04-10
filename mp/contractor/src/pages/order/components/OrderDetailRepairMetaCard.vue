<template>
  <view v-if="show" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">维修信息</text>
    </view>
    <view class="od-info-list">
      <view v-if="hasVal(repair.faultJudge)" class="info-item">
        <text class="info-label">故障判定</text>
        <text class="info-value">{{ repair.faultJudge }}</text>
      </view>
      <view v-if="hasVal(repair.quoteAmount)" class="info-item">
        <text class="info-label">维修报价</text>
        <text class="info-value text-primary" style="font-size: 36rpx; font-weight: bold">
          ¥ {{ repair.quoteAmount }}
        </text>
      </view>
      <view v-if="hasVal(repair.quoteDesc)" class="info-item-col">
        <text class="info-label" style="margin-bottom: 16rpx; display: block">报价说明</text>
        <view class="desc-box">
          <text class="desc-text">{{ repair.quoteDesc }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    repair: OrderDetail['repair']
  }>()

  const show = computed(() => {
    const r = props.repair
    return hasVal(r.faultJudge) || hasVal(r.quoteAmount) || hasVal(r.quoteDesc)
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
