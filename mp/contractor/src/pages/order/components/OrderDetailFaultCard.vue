<template>
  <view v-if="visible" class="od-card-box">
    <view class="od-section-header">
      <view class="section-mark"></view>
      <text class="section-title">故障信息</text>
    </view>
    <view class="od-fault-details">
      <view v-if="!isOtherFault && hasVal(fault.desc)" class="detail-group">
        <text class="group-title">故障描述</text>
        <text class="group-content">{{ fault.desc }}</text>
      </view>
      <view v-if="isOtherFault && hasVal(fault.faultExplain)" class="detail-group">
        <text class="group-title">故障说明</text>
        <text class="group-content">{{ fault.faultExplain }}</text>
      </view>
      <view v-if="hasVal(fault.voiceDuration)" class="detail-group">
        <text class="group-title">语音说明</text>
        <view class="voice-msg">
          <image class="voice-icon" :src="volumeUpIcon" mode="aspectFit" />
          <view class="voice-waves">
            <view class="wave wave-1"></view>
            <view class="wave wave-2"></view>
            <view class="wave wave-3"></view>
            <view class="wave wave-4"></view>
          </view>
          <text class="voice-duration">{{ fault.voiceDuration }}</text>
        </view>
      </view>
      <view v-if="hasVal(fault.images)" class="detail-group">
        <text class="group-title">故障图片</text>
        <view class="image-grid">
          <image
            v-for="(img, idx) in fault.images"
            :key="'fault-img-' + idx"
            class="grid-img"
            mode="widthFix"
            :src="img"
          ></image>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { volumeUpIcon } from '@/svgs'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    fault: OrderDetail['fault']
  }>()

  const isOtherFault = computed(() => props.fault.desc === '其它故障')

  const visible = computed(() => {
    const f = props.fault
    const other = f.desc === '其它故障'
    const textOk = other ? hasVal(f.faultExplain) : hasVal(f.desc)
    return textOk || hasVal(f.voiceDuration) || (Array.isArray(f.images) && f.images.length > 0)
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailCardStyles.scss';
</style>
