<template>
  <view class="page-container">
    <view v-if="loading" class="state-wrap">
      <text class="state-text">加载中…</text>
    </view>
    <ListEmpty v-else-if="records.length === 0" title="暂无历史维修记录" />

    <view v-else class="record-list">
      <view v-for="(record, index) in records" :key="index" class="record-card">
        <view v-if="showRepairDescSection(record)" class="section">
          <text class="section-label">维修说明</text>
          <text class="section-value font-bold">{{ recordRepairLine(record) }}</text>
        </view>

        <view v-if="recordOtherSupplement(record)" class="special-info">
          <view class="special-header">
            <uni-icons type="info" size="14" color="#f26604"></uni-icons>
            <text class="special-title">其它维修说明</text>
          </view>
          <text class="special-content">{{ recordOtherSupplement(record) }}</text>
        </view>

        <view v-if="record.parts && record.parts.length > 0" class="section">
          <text class="section-label">更换配件</text>
          <view class="parts-list">
            <view v-for="(part, pIndex) in record.parts" :key="pIndex" class="part-tag">
              <text class="part-name">{{ part.name }}</text>
              <text class="part-count">x{{ part.count }}</text>
            </view>
          </view>
        </view>

        <view v-if="(record.images || []).length > 0" class="section image-section">
          <text class="section-label">故障点图片</text>
          <scroll-view scroll-x class="image-scroll-view" :show-scrollbar="false">
            <view class="image-list">
              <view
                v-for="(img, imgIndex) in record.images"
                :key="imgIndex"
                class="image-item"
                @tap="previewRecordImage(record, imgIndex)"
              >
                <image class="image-content" :src="img.url" mode="aspectFill"></image>
                <text v-if="img.label" class="image-label">{{ img.label }}</text>
              </view>
            </view>
          </scroll-view>
        </view>

        <view class="card-footer">
          <view v-if="record.location" class="footer-item location">
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

    <ListNoMore v-if="!loading && records.length > 0" />
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import { WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY } from '@/constants/historicalRecord'
  import { getOrderDetailAPI, type FaultPointMaintenanceRecord } from '@/api/order'
  import { previewImages, resolvePreviewableUrl } from '@/utils/mediaPreview'

  const OTHER_REPAIR_DESC = '其它维修说明'

  const hasStructuredRepairFields = (r: FaultPointMaintenanceRecord) =>
    r.faultDesc !== undefined || r.repairDesc !== undefined || r.otherDesc !== undefined

  const legacyDescriptionRepairOnly = (description: string) => {
    const d = String(description || '').trim()
    if (!d) return ''
    const sep = ' · '
    const i = d.indexOf(sep)
    if (i === -1) return d
    return d.slice(i + sep.length).trim()
  }

  const recordRepairLine = (r: FaultPointMaintenanceRecord) => {
    if (!hasStructuredRepairFields(r)) {
      return legacyDescriptionRepairOnly(String(r.description || ''))
    }
    const repairDesc = String(r.repairDesc ?? '').trim()
    const otherDesc = String(r.otherDesc ?? '').trim()
    return repairDesc === OTHER_REPAIR_DESC ? otherDesc : repairDesc
  }

  const showRepairDescSection = (r: FaultPointMaintenanceRecord) =>
    recordRepairLine(r).trim() !== OTHER_REPAIR_DESC

  const recordOtherSupplement = (r: FaultPointMaintenanceRecord) => {
    if (!hasStructuredRepairFields(r)) {
      return String(r.specialInfo || '').trim()
    }
    const repairDesc = String(r.repairDesc ?? '').trim()
    const otherDesc = String(r.otherDesc ?? '').trim()
    if (!otherDesc || repairDesc === OTHER_REPAIR_DESC) return ''
    return otherDesc
  }

  const parseStoredFaultHistory = (raw: string): FaultPointMaintenanceRecord[] => {
    try {
      const v = JSON.parse(raw || '[]') as unknown
      return Array.isArray(v) ? (v as FaultPointMaintenanceRecord[]) : []
    } catch {
      return []
    }
  }

  const records = ref<FaultPointMaintenanceRecord[]>([])
  const loading = ref(true)

  const previewRecordImage = (record: FaultPointMaintenanceRecord, imgIndex: number) => {
    const imgs = record.images || []
    const urls = imgs.map((img) => resolvePreviewableUrl(img.url)).filter(Boolean)
    if (!urls.length) return
    previewImages(urls, imgIndex)
  }

  const loadRecordsFromOrder = async (orderId: string) => {
    loading.value = true
    try {
      const res = await getOrderDetailAPI({ id: orderId })
      records.value = res.result?.faultPoint?.records ?? []
    } catch {
      records.value = []
    }
    loading.value = false
  }

  onLoad((options: Record<string, string | undefined>) => {
    const mode = String(options?.mode || '')
    if (mode === 'repairs') {
      loading.value = true
      let raw: string
      try {
        raw = String(uni.getStorageSync(WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY) || '')
      } catch {
        raw = ''
      }
      records.value = parseStoredFaultHistory(raw)
      loading.value = false
      try {
        uni.removeStorageSync(WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY)
      } catch {
        /* noop */
      }
      return
    }

    const id = options?.orderId ? String(options.orderId) : ''
    if (!id) {
      loading.value = false
      records.value = []
      uni.showToast({ title: '缺少工单编号', icon: 'none', duration: 1500 })
      return
    }
    loadRecordsFromOrder(id)
  })
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .page-container {
    min-height: 100vh;
    background-color: $bg-light;
    padding: $space-md;
    box-sizing: border-box;
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
    gap: $space-md;
  }

  .record-card {
    @include flex-column;
    gap: $space-lg;
    background-color: $bg-card;
    border-radius: $radius-md;
    padding: $space-lg;
    border: 1px solid $border-light;
  }

  .section {
    @include flex-column;
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
    border: 1px solid $border-light;
    background-color: $border-light;
  }

  .image-label {
    font-size: 20rpx;
    color: $text-muted;
    font-weight: 500;
    text-align: center;
  }

  .special-info {
    background-color: rgba(255, 247, 237, 0.5);
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
    color: $text-body;
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
    background-color: $bg-light;
    padding: $space-xs 20rpx;
    border-radius: $radius-round;
    border: 1px solid $border-light;
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

  .card-footer {
    padding-top: $space-md;
    border-top: 1px solid $border-lighter;
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
    color: $text-secondary;
    font-weight: 500;
    @include ellipsis(1);
    max-width: 400rpx;
  }
</style>
