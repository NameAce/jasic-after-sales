<template>
  <view v-if="show" class="od-apply-card">
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">商品信息</text>
    </view>
    <view class="od-apply-info-list">
      <view v-if="hasVal(product.barcode)" class="info-item">
        <text class="info-label">条形码</text>
        <text class="info-value">{{ product.barcode }}</text>
      </view>
      <view v-if="hasVal(product.model)" class="info-item">
        <text class="info-label">机器型号</text>
        <text class="info-value">{{ product.model }}</text>
      </view>
      <view v-if="hasVal(product.serialNo)" class="info-item">
        <text class="info-label">机器小号</text>
        <text class="info-value">{{ product.serialNo }}</text>
      </view>
      <view v-if="hasVal(product.outDate)" class="info-item">
        <text class="info-label">最后出库日期</text>
        <text class="info-value">{{ product.outDate }}</text>
      </view>
      <view v-if="hasVal(product.warrantyClass)" class="info-item">
        <text class="info-label">质保判定</text>
        <view :class="['tag-value', warrantyTagClass]">{{ product.warrantyClass }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import type { OrderDetail } from '@/models/order'
  import { getWarrantyTagClass } from '@/utils/orderTags'
  import { hasVal } from '@/utils/value'

  const props = defineProps<{
    product: OrderDetail['product']
  }>()

  /** 与原先 hasBarcode 一致：有条码才展示商品卡 */
  const show = computed(() => !!props.product.barcode)

  const warrantyTagClass = computed(() => getWarrantyTagClass(props.product.warrantyClass))
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';
</style>
