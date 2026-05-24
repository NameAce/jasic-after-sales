<template>
  <!-- 售后客户端小程序（报修、工单、地址）组件 ServicePointFormItem -->
  <uni-forms-item label="选择附近网点" name="centerId" required>
    <FormItemAnchor name="centerId" />
    <!-- 只读选择行：不用 input，避免点击时弹出键盘 -->
    <view class="service-point-picker" @click="goToServicePoint">
      <uni-icons type="location-filled" size="20" :color="themeColor.info" class="picker-icon" />
      <text :class="['service-point-picker-text', { placeholder: !hasDisplayText }]">{{
        pickerText
      }}</text>
      <uni-icons type="right" size="14" :color="themeColor.textMuted" />
    </view>
  </uni-forms-item>
</template>

<script setup lang="ts">
/**
 * 售后客户端小程序（报修、工单、地址）：ServicePointFormItem。
 * 跳转附近网点列表页选择，展示为只读行（非输入框）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { computed } from 'vue'
  import { themeColor } from '@/constants/theme'
  import FormItemAnchor from '@/components/FormItemAnchor/FormItemAnchor.vue'

  const props = defineProps<{
    displayText: string
  }>()

  const PLACEHOLDER = '请选择距离您最近的维修中心'

  const hasDisplayText = computed(() => String(props.displayText ?? '').trim().length > 0)

  const pickerText = computed(() =>
    hasDisplayText.value ? String(props.displayText).trim() : PLACEHOLDER
  )

  const goToServicePoint = () => {
    uni.hideKeyboard()
    uni.navigateTo({ url: '/pages/servicePoint/index' })
  }
</script>

<style lang="scss" scoped>
  .service-point-picker {
    @include flex-row;
    align-items: center;
    justify-content: space-between;
    gap: $space-sm;
    border: 1px solid $border-muted;
    border-radius: $radius-md;
    padding: 18rpx 24rpx;
    min-height: 80rpx;
    box-sizing: border-box;
    background-color: $bg-input;
  }

  .picker-icon {
    flex-shrink: 0;
  }

  .service-point-picker-text {
    flex: 1;
    min-width: 0;
    font-size: $font-sm;
    color: $text-body;
    line-height: 1.5;
    @include ellipsis(1);

    &.placeholder {
      color: $text-placeholder;
    }
  }
</style>
