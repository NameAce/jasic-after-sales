<template>
  <view class="base-btn">
    <view class="base-btn-content">
      <slot />
      <slot name="submit" />
    </view>
  </view>
</template>

<style scoped lang="scss">
  .base-btn {
    @include fixed-btn;

    .base-btn-content {
      @include flex-center;
      height: 80rpx;
    }
  }
</style>
