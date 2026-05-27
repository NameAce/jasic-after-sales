<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）：BaseButton。 @修改人 黄碧莲 @修改时间 2026-05-22 -->
  <!-- 承修方小程序（网点/总部工单处理、派工）组件 BaseButton -->
  <view class="base-btn">
    <view class="base-btn-content">
      <slot />
      <slot v-if="$slots.submit" name="submit" />
    </view>
  </view>
</template>

<style scoped lang="scss">
  .base-btn {
    @include fixed-btn;
    box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.08);
    padding-bottom: calc(#{$space-md} + constant(safe-area-inset-bottom));
    padding-bottom: calc(#{$space-md} + env(safe-area-inset-bottom));

    .base-btn-content {
      @include flex-center;
      height: 80rpx;
    }
  }
</style>
