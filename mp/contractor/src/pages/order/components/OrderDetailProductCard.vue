<template>
  <view v-if="show" class="od-apply-card">
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">商品信息</text>
    </view>
    <view class="od-apply-info-list">
      <!-- 机型：仅展示（佳士品牌缺机型时由 detail.vue 触发补录弹窗，不再提供自由文本输入） -->
      <view v-if="hasVal(product.model)" class="info-item">
        <text class="info-label">机器型号</text>
        <text class="info-value">{{ product.model }}</text>
      </view>
      <view v-else-if="needSupplement" class="info-item info-item--cta">
        <text class="info-label">机器型号</text>
        <view class="supplement-cta" @click="emit('supplement')">
          <text class="supplement-cta-text">点击补录机器型号</text>
          <uni-icons type="right" size="14" color="#f26604" />
        </view>
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

  /**
   * 商品信息卡片：
   * - 机型只读展示；不再在卡片内自由输入（避免用户绕过后端校验写入空/非法机型）。
   * - 佳士品牌 + 缺机型时渲染"点击补录机器型号"入口，由父组件 detail.vue 负责唤起 MachineModelSupplementModal。
   */
  const props = defineProps<{
    product: OrderDetail['product']
    /** 是否为需要"补录机型"的场景（佳士品牌 + 当前 product.model 为空） */
    needSupplement?: boolean
  }>()

  const emit = defineEmits<{
    (e: 'supplement'): void
  }>()

  const show = computed(() => {
    const p = props.product
    return (
      !!props.needSupplement ||
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

  .info-item--cta {
    align-items: center;
  }

  .supplement-cta {
    display: inline-flex;
    align-items: center;
    gap: 4rpx;
    padding: 8rpx 0;

    .supplement-cta-text {
      font-size: 26rpx;
      color: $primary;
    }
  }
</style>
