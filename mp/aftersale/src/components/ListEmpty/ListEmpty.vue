<template>
  <view class="list-empty">
    <view v-if="showIllustration" class="empty-icon-wrap">
      <image class="empty-list-illus" :src="emptyOrderListIcon" mode="aspectFit" />
    </view>
    <text class="empty-title">{{ title }}</text>
    <text v-if="desc" class="empty-desc">{{ desc }}</text>
  </view>
</template>

<script setup lang="ts">
  import { emptyOrderListIcon } from '@/svgs'

  withDefaults(
    defineProps<{
      title: string
      desc?: string
      showIllustration?: boolean
    }>(),
    {
      desc: '',
      showIllustration: true,
    },
  )
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .list-empty {
    min-height: 60vh;
    @include flex-column;
    align-items: center;
    justify-content: center;
    padding: $space-xl;
    box-sizing: border-box;
  }

  .empty-icon-wrap {
    margin-bottom: $space-md;
  }

  .empty-list-illus {
    width: 240rpx;
    height: 240rpx;
  }

  .empty-title {
    font-size: $font-md;
    color: $text-muted;
    text-align: center;
  }

  .empty-desc {
    margin-top: $space-xs;
    font-size: $font-sm;
    color: $text-placeholder;
    text-align: center;
  }
</style>
