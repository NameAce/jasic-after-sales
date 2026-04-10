<template>
  <view class="page-container">
    <!-- 暂无历史维修记录 -->
    <ListEmpty v-if="records.length === 0" title="暂无历史维修记录" />

    <!-- 历史维修记录列表 -->
    <view v-else class="record-list">
      <view v-for="(record, index) in records" :key="index" class="record-card">
        <!-- 维修说明 -->
        <view class="section">
          <text class="section-label">维修说明</text>
          <text class="section-value font-bold">{{ record.description }}</text>
        </view>

        <!-- 故障点图片 -->
        <view class="section image-section">
          <text class="section-label">故障点图片</text>
          <scroll-view scroll-x class="image-scroll-view" :show-scrollbar="false">
            <view class="image-list">
              <view v-for="(img, imgIndex) in record.images" :key="imgIndex" class="image-item">
                <image class="image-content" :src="img.url" mode="aspectFill"></image>
                <text class="image-label">{{ img.label }}</text>
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 其它维修说明 -->
        <view v-if="record.specialInfo" class="special-info">
          <view class="special-header">
            <uni-icons type="info" size="14" color="#f26604"></uni-icons>
            <text class="special-title">其它维修说明</text>
          </view>
          <text class="special-content">{{ record.specialInfo }}</text>
        </view>

        <!-- 更换配件 -->
        <view v-if="record.parts && record.parts.length > 0" class="section">
          <text class="section-label">更换配件</text>
          <view class="parts-list">
            <view v-for="(part, pIndex) in record.parts" :key="pIndex" class="part-tag">
              <text class="part-name">{{ part.name }}</text>
              <text class="part-count">x{{ part.count }}</text>
            </view>
          </view>
        </view>

        <!-- 底部 -->
        <view class="card-footer">
          <view class="footer-item location">
            <uni-icons type="location-filled" size="16" color="#9ca3af"></uni-icons>
            <text class="footer-text">{{ record.location }}</text>
          </view>
          <view class="footer-item time">
            <uni-icons type="calendar-filled" size="16" color="#9ca3af"></uni-icons>
            <text class="footer-text">{{ record.date }}</text>
          </view>
        </view>
      </view>
    </view>

    <ListNoMore v-if="records.length > 0" />
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import { onLoad } from '@dcloudio/uni-app'
  import type { FaultPointRecord } from '@/models/order'

  /**
   * 历史维修记录列表
   * @returns 历史维修记录列表
   */
  const records = ref<FaultPointRecord[]>([])

  onLoad((options: any) => {
    const orderId = String(options?.orderId || '')
    if (orderId) {
      // 移除 mock 数据源：接口对接后从后端获取历史维修记录
      records.value = []
    }
  })
</script>

<style lang="scss" scoped>
  .page-container {
    min-height: 100vh;
    background-color: $surface-slate-50;
    padding: $space-md;
    box-sizing: border-box;
  }

  .record-list {
    @include flex-col;
    gap: $space-md;
  }

  .record-card {
    @include flex-col;
    gap: $space-lg;
    background-color: $surface-white;
    border-radius: $radius-md;
    padding: $space-lg;
    border: 1px solid $surface-slate-100;
  }

  .section {
    @include flex-col;
  }

  .section-label {
    font-size: $font-sm;
    color: $text-slate-400;
    font-weight: 500;
    margin-bottom: $space-xs;
  }

  .section-value {
    font-size: $font-md;
    color: $text-slate-900;

    &.font-bold {
      font-weight: 600;
    }
  }

  .image-section {
    gap: $space-sm;
  }

  .image-scroll-view {
    width: 100%;
    white-space: nowrap;
  }

  .image-list {
    display: flex;
    gap: $space-md;
    padding-bottom: $space-xs;
  }

  .image-item {
    @include flex-column-center;
    gap: $space-xs;
    flex-shrink: 0;
  }

  .image-content {
    width: 112rpx;
    height: 112rpx;
    border-radius: 12rpx;
    border: 1px solid $surface-slate-100;
    background-color: $surface-slate-100;
  }

  .image-label {
    font-size: 20rpx;
    color: $text-slate-400;
    font-weight: 500;
    text-align: center;
  }

  .special-info {
    background-color: rgba($tag-brand-bg, 0.5);
    border-left: 2px solid $primary;
    border-top-right-radius: $radius-md;
    border-bottom-right-radius: $radius-md;
    padding: $space-md;
  }

  .special-header {
    @include flex-row;
    gap: $space-xs;
    margin-bottom: $space-xs;
  }

  .special-title {
    font-size: 20rpx;
    color: $primary;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  .special-content {
    font-size: 26rpx;
    color: $text-slate-700;
    line-height: 1.5;
  }

  .parts-list {
    display: flex;
    flex-wrap: wrap;
    gap: $space-sm;
  }

  .part-tag {
    @include flex-row;
    gap: 12rpx;
    background-color: $surface-slate-50;
    padding: $space-xs 20rpx;
    border-radius: $radius-pill;
    border: 1px solid $surface-slate-100;
  }

  .part-name {
    font-size: $font-sm;
    color: $text-slate-700;
  }

  .part-count {
    font-size: $font-sm;
    color: $primary;
    font-weight: 700;
  }

  .card-footer {
    padding-top: $space-md;
    border-top: 1px solid $surface-slate-50;
    @include flex-between;
    flex-wrap: wrap;
    gap: $space-sm;
  }

  .footer-item {
    @include flex-row;
    gap: 12rpx;
  }

  .location {
    min-width: 50%;
  }

  .footer-text {
    font-size: 22rpx;
    color: $text-slate-500;
    font-weight: 500;
    @include text-ellipsis;
    max-width: 400rpx;
  }
</style>
