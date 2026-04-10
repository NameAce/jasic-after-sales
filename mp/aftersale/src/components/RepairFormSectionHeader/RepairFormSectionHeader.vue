<template>
  <view
    class="repair-form-section-header"
    :class="{ 'is-collapsible': collapsible }"
    @click="collapsible && $emit('toggle')"
  >
    <view>{{ title }}</view>
    <view v-if="collapsible" class="collapse-toggle">
      <text class="optional-badge">OPTIONAL</text>
      <text class="toggle-text">{{ expanded ? '收缩' : '展开' }}</text>
    </view>
    <text v-else class="required-badge">REQUIRED</text>
  </view>
</template>

<script setup lang="ts">
  defineProps<{
    title: string
    collapsible?: boolean
    expanded?: boolean
  }>()

  defineEmits<{
    toggle: []
  }>()
</script>

<style lang="scss" scoped>
  .repair-form-section-header {
    @include flex-between;
    padding: 0 $space-xs;

    > view:first-child {
      font-size: $font-md;
      font-weight: bold;
      color: $text-main;
    }

    .required-badge {
      font-size: 20rpx;
      font-weight: bold;
      color: rgba($primary, 0.6);
      letter-spacing: 2rpx;
      text-transform: uppercase;
    }

    .optional-badge {
      font-size: 20rpx;
      font-weight: bold;
      color: $text-disabled;
      letter-spacing: 2rpx;
      text-transform: uppercase;
    }

    &.is-collapsible {
      cursor: pointer;
    }
  }

  .collapse-toggle {
    @include flex-row;
    gap: $space-sm;
    align-items: center;

    .toggle-text {
      font-size: $font-sm;
      color: $text-secondary;
    }
  }
</style>
