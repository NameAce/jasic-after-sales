<template>
  <view class="page-container page-index page-padding">
    <!-- 加载状态 -->
    <view v-if="loading" class="state-wrap">
      <text class="state-text">加载中…</text>
    </view>
    <!-- 无数据状态 -->
    <view v-else-if="records.length === 0" class="state-wrap">
      <text class="state-text">暂无故障点维修历史</text>
    </view>
    <!-- 故障点维修历史列表 -->
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
          <view class="footer-item">
            <uni-icons type="calendar-filled" size="16" color="#9ca3af"></uni-icons>
            <text class="footer-text">{{ record.date }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { getOrderDetailAPI, type FaultPointMaintenanceRecord } from '@/api/order'

  // 故障点维修历史列表
  const records = ref<FaultPointMaintenanceRecord[]>([])
  // 加载状态
  const loading = ref(true)

  /**
   * 加载故障点维修历史列表
   * @param orderId - 工单ID
   * @returns void
   */
  const loadRecords = async (orderId: string) => {
    loading.value = true
    try {
      const res = await getOrderDetailAPI({ id: orderId })
      records.value = res.result?.faultPoint?.records ?? []
    } catch {
      records.value = []
    }
    loading.value = false
  }

  /**
   * 页面加载
   * @param options - 选项
   * @returns void
   */
  onLoad((options: Record<string, string | undefined>) => {
    const id = options?.orderId ? String(options.orderId) : ''
    if (!id) {
      loading.value = false
      uni.showToast({ title: '缺少工单编号', icon: 'none', duration: 1500 })
      return
    }
    loadRecords(id)
  })
</script>

<style lang="scss">
  .page-container.page-index.page-padding {
    padding-top: $space-lg;
  }

  .state-wrap {
    min-height: 60vh;
    @include flex-center;
    padding: $space-xl;

    .state-text {
      font-size: $font-md;
      color: $text-muted;
    }
  }

  .record-list {
    @include flex-column;
    gap: $space-lg;
  }

  .record-card {
    @include white-card($radius-md, $space-lg);
    @include flex-column;
    gap: $space-lg;

    .section {
      @include flex-column;
    }

    .card-footer {
      padding-top: $space-md;
      border-top: 2rpx solid $border-lighter;
      display: flex;
      flex-wrap: wrap;
      @include flex-between;
      gap: $space-sm;
    }
  }

  .section-label {
    font-size: $font-sm;
    color: $text-muted;
    font-weight: 500;
    margin-bottom: $space-xs;
  }

  .section-value {
    font-size: $font-md;
    color: $text-dark;

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
    @include flex-row;
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
    border-radius: $radius-md;
    border: 2rpx solid $border-light;
    background-color: $border-light;
  }

  .image-label {
    font-size: $font-xs;
    color: $text-muted;
    font-weight: 500;
    text-align: center;
  }

  .special-info {
    background-color: rgba(255, 247, 237, 0.5);
    border-left: 4rpx solid $primary;
    border-top-right-radius: $radius-md;
    border-bottom-right-radius: $radius-md;
    padding: $space-md;

    .special-header {
      @include flex-row;
      gap: $space-xs;
      margin-bottom: $space-xs;
    }

    .special-title {
      font-size: $font-xs;
      color: $primary;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .special-content {
      font-size: 26rpx;
      color: $text-body;
      line-height: 1.5;
    }
  }

  .parts-list {
    display: flex;
    flex-wrap: wrap;
    gap: $space-sm;

    .part-tag {
      @include flex-row;
      gap: 12rpx;
      background-color: $bg-light;
      padding: $space-xs 20rpx;
      border-radius: $radius-round;
      border: 2rpx solid $border-light;
    }

    .part-name {
      font-size: $font-sm;
      color: $text-body;
    }

    .part-count {
      font-size: $font-sm;
      color: $primary;
      font-weight: 700;
    }
  }

  .footer-item {
    @include flex-row;
    gap: 12rpx;

    &.location {
      min-width: 50%;
    }
  }

  .footer-text {
    font-size: 22rpx;
    color: $text-secondary;
    font-weight: 500;
    @include ellipsis(1);
    max-width: 400rpx;
  }
</style>
