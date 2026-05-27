<template>
  <view class="feedback-list-page">
    <custom-nav-bar title="已提交反馈" surface="sticky" />
    <scroll-view class="feedback-list-scroll" scroll-y>
      <view class="page-index page-padding list-content">
        <view v-if="records.length === 0" class="empty-wrap">
          <ListEmpty title="暂无已提交反馈" desc="提交后可在此查看历史记录" />
        </view>

        <view v-else class="feedback-list">
          <view v-for="item in records" :key="item.id" class="feedback-card">
            <view class="feedback-card-head">
              <text class="feedback-card-title">反馈内容</text>
              <text class="feedback-card-time">{{ formatTime(item.createdAt) }}</text>
            </view>
            <text class="feedback-card-content">{{ item.content }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import { loadFeedbackHistory, type FeedbackHistoryItem } from '@/utils/feedbackHistory'

  const records = ref<FeedbackHistoryItem[]>([])

  /**
   * 页面展示时读取本地反馈历史，保证列表数据最新。
   */
  const refreshList = () => {
    records.value = loadFeedbackHistory()
  }

  onShow(() => {
    refreshList()
  })

  /**
   * 将毫秒时间戳格式化为 `YYYY-MM-DD HH:mm` 便于阅读。
   * @param ts 毫秒时间戳
   */
  const formatTime = (ts: number) => {
    const d = new Date(ts)
    const yyyy = d.getFullYear()
    const mm = `${d.getMonth() + 1}`.padStart(2, '0')
    const dd = `${d.getDate()}`.padStart(2, '0')
    const hh = `${d.getHours()}`.padStart(2, '0')
    const min = `${d.getMinutes()}`.padStart(2, '0')
    return `${yyyy}-${mm}-${dd} ${hh}:${min}`
  }
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .feedback-list-page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    box-sizing: border-box;
    overflow: hidden;
  }

  .feedback-list-scroll {
    flex: 1;
    height: 0;
    min-height: 0;
  }

  .list-content {
    padding-top: $space-lg;
    padding-bottom: calc(#{$space-xl} + env(safe-area-inset-bottom));
  }

  .empty-wrap {
    margin-top: 60rpx;
  }

  .feedback-list {
    display: flex;
    flex-direction: column;
    gap: $space-md;
  }

  .feedback-card {
    @include white-card($radius-lg, $space-lg);
    border: 2rpx solid $border-lighter;
  }

  .feedback-card-head {
    @include flex-between;
    margin-bottom: $space-sm;
  }

  .feedback-card-title {
    font-size: $font-md;
    color: $text-label;
    font-weight: 600;
  }

  .feedback-card-time {
    font-size: $font-xs;
    color: $text-muted;
  }

  .feedback-card-content {
    display: block;
    font-size: $font-md;
    color: $text-body;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-all;
  }
</style>
