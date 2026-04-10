<template>
  <view v-if="show" class="od-apply-card">
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">服务信息</text>
    </view>
    <view class="od-apply-info-list">
      <view v-if="hasVal(service.sitePhone)" class="info-item">
        <text class="info-label">网点电话</text>
        <text class="info-value">{{ service.sitePhone }}</text>
      </view>
      <view v-if="hasVal(service.repairMethod)" class="info-item">
        <text class="info-label">维修方式</text>
        <view :class="['tag-method', repairMethodTagClass]">{{ service.repairMethod }}</view>
      </view>
      <view v-if="hasVal(service.source)" class="info-item">
        <text class="info-label">申请来源</text>
        <text class="info-value">{{ service.source }}</text>
      </view>
      <view v-if="hasVal(transferFromSite)" class="info-item">
        <text class="info-label">转单网点</text>
        <text class="info-value">{{ transferFromSite }}</text>
      </view>
      <view
        v-if="showServiceSenderInfo && hasVal(service.senderInfo)"
        class="info-item align-top"
      >
        <text class="info-label shrink">寄件信息</text>
        <text class="info-value text-right">{{ service.senderInfo }}</text>
      </view>
      <view
        v-if="showServiceSenderInfo && hasVal(service.senderVoucherImg)"
        class="info-item align-center"
      >
        <text class="info-label">寄件快递单号</text>
        <image class="shipping-img" mode="aspectFill" :src="service.senderVoucherImg"></image>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { getRepairMethodTagClass } from '@/utils/orderTags'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    service: OrderDetail['service']
    transferFromSite: string
  }>()

  const repairMethodTagClass = computed(() =>
    getRepairMethodTagClass(props.service.repairMethod)
  )

  const showServiceSenderInfo = computed(() => {
    const method = (props.service.repairMethod || '').trim()
    return /邮寄/.test(method)
  })

  /** 与原 hasServiceInfoCard 一致 */
  const show = computed(() => {
    const s = props.service
    const b = props.transferFromSite
    if (hasVal(s.sitePhone) || hasVal(s.repairMethod) || hasVal(s.source)) return true
    if (hasVal(b)) return true
    if (!showServiceSenderInfo.value) return false
    return hasVal(s.senderInfo) || hasVal(s.senderVoucherImg)
  })
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';
</style>
