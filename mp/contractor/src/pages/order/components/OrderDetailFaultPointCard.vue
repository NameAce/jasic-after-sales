<template>
  <view :class="asCard ? 'od-card-box' : ''">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">故障点信息</text>
    </view>
    <view class="od-fault-point-info">
      <view class="history-header">
        <text class="history-title">{{ historyTitle }}</text>
        <view class="history-btn" @click="openRepairHistory">查看历史记录</view>
      </view>
      <view class="history-record">
        <view v-if="hasVal(date)" class="record-top">
          <text class="record-label">{{ recordLabel }}</text>
          <text class="record-date">{{ date }}</text>
        </view>
        <text v-if="hasVal(desc)" class="record-desc">{{ desc }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { hasVal } from '@/utils/value'

  const props = withDefaults(
    defineProps<{
      historyTitle: string
      recordLabel: string
      date: string
      desc: string
      /** 用于跳转历史记录页 */
      orderId: string
      /** false：维修过程 Tab 内嵌（无外层白底 card） */
      asCard?: boolean
    }>(),
    { asCard: true }
  )

  function openRepairHistory() {
    const id = (props.orderId || '').trim()
    if (!id) return
    uni.navigateTo({
      url: `/pages/historicalRecord/index?orderId=${encodeURIComponent(id)}`
    })
  }
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
