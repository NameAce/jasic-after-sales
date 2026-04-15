<template>
  <view v-if="show" class="od-apply-card">
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">商品信息</text>
    </view>
    <view class="od-apply-info-list">
      <view v-if="hasVal(product.model)" class="info-item">
        <text class="info-label">机器型号</text>
        <text class="info-value">{{ product.model }}</text>
      </view>
      <view v-else-if="showModelInput" class="info-item info-item--input">
        <text class="info-label">机器型号</text>
        <input
          v-model.trim="modelInputValue"
          class="model-input-v2"
          type="text"
          placeholder="请输入机器型号"
          placeholder-class="model-input-placeholder-v2"
          :maxlength="60"
        />
      </view>
      <view v-if="hasVal(product.brandName)" class="info-item">
        <text class="info-label">品牌</text>
        <text class="info-value">{{ product.brandName }}</text>
      </view>
      <view v-if="hasVal(product.barcode)" class="info-item">
        <text class="info-label">条形码</text>
        <text class="info-value">{{ product.barcode }}</text>
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
    showModelInput?: boolean
  }>()

  const modelInputValue = defineModel<string>('modelInput', { default: '' })

  const show = computed(() => {
    const p = props.product
    return (
      hasVal(p.model) ||
      hasVal(p.brandName) ||
      hasVal(p.barcode) ||
      hasVal(p.serialNo) ||
      hasVal(p.outDate) ||
      hasVal(p.warrantyClass)
    )
  })

  const warrantyTagClass = computed(() => getWarrantyTagClass(props.product.warrantyClass))
</script>

<style lang="scss" scoped>
  @use './orderDetailApplyCards.scss';

  .info-item--input {
    flex-direction: column;
    align-items: stretch;
    gap: $space-sm;

    .info-label {
      line-height: 1.4;
    }
  }

  .model-input-v2 {
    @include form-field-soft;
    box-sizing: border-box;
    width: 100%;
    height: 80rpx;
    padding: 0 $space-lg;
    font-size: 26rpx;
    color: $text-slate-900;
  }

  :deep(.model-input-placeholder-v2) {
    color: #94a3b8;
    font-size: 26rpx;
  }
</style>
