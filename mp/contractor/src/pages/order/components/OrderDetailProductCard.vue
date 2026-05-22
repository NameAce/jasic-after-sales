<template>
  <view v-if="show" class="od-apply-card">
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">商品信息</text>
    </view>
    <view class="od-apply-info-list">
      <!-- 机型：仅展示（仅维修登记入口缺机型时由 detail.vue 触发补录弹窗） -->
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
      <!-- C 端申请内容：商品信息不展示品牌 -->
      <view v-if="hasVal(product.barcode)" class="info-item">
        <text class="info-label">条形码</text>
        <text class="info-value">{{ product.barcode }}</text>
      </view>
      <view v-if="hasVal(product.serialNo)" class="info-item">
        <text class="info-label">机器小号</text>
        <text class="info-value">{{ product.serialNo }}</text>
      </view>
      <view v-if="hasVal(product.lastOutDate)" class="info-item">
        <text class="info-label">最后出库日期</text>
        <text class="info-value">{{ product.lastOutDate }}</text>
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
   * - 「须补录机型」时（维修登记缺机型，或复检且佳士缺机型）渲染「点击补录机器型号」，由父组件 detail.vue 唤起 MachineModelSupplementModal。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const props = defineProps<{
    product: OrderDetail['product']
    /**
 * 是否需要补录机型入口（父级 detail：维修登记无机型，或复检佳士无机型）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
      hasVal(p.barcode) ||
      hasVal(p.serialNo) ||
      hasVal(p.lastOutDate) ||
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
