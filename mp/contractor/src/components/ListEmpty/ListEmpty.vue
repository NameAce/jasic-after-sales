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
      showIllustration: true
    }
  )
</script>
