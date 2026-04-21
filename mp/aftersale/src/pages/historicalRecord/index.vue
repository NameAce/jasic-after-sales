<template>
  <view class="historical-record-page">
    <custom-nav-bar title="故障点历史记录" surface="sticky" />
    <view class="page-container">
      <ListEmpty v-if="records.length === 0" title="暂无历史维修记录" />

      <view v-else class="record-list">
        <view v-for="(record, index) in records" :key="index" class="record-card">
          <view v-if="showRepairDescSection(record)" class="section">
            <text class="section-label">维修说明</text>
            <text class="section-value section-value--emphasis">{{
              recordRepairLine(record)
            }}</text>
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

      <ListNoMore v-if="records.length > 0" />
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref } from 'vue'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import ListNoMore from '@/components/ListNoMore/ListNoMore.vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { fetchOrderRepairFaultRecords } from '@/api/workOrder'
  import { WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY } from '@/constants/historicalRecord'
  import type { FaultPointRecord } from '@/models/order'
  import { previewImages, resolvePreviewableUrl } from '@/utils/mediaPreview'

  /** 与登记表单、详情页一致：选项值为「其它维修说明」时主文案用 otherDesc */
  const OTHER_REPAIR_DESC = '其它维修说明'

  const hasStructuredRepairFields = (r: FaultPointRecord) =>
    r.faultDesc !== undefined || r.repairDesc !== undefined || r.otherDesc !== undefined

  /** 旧缓存 description 曾为 faultDesc · repairDesc，仅取维修侧 */
  const legacyDescriptionRepairOnly = (description: string) => {
    const d = String(description || '').trim()
    if (!d) return ''
    const sep = ' · '
    const i = d.indexOf(sep)
    if (i === -1) return d
    return d.slice(i + sep.length).trim()
  }

  /** 维修说明主行：仅 repairDesc；为「其它维修说明」时用 otherDesc（不含 faultDesc） */
  const recordRepairLine = (r: FaultPointRecord) => {
    if (!hasStructuredRepairFields(r)) {
      return legacyDescriptionRepairOnly(String(r.description || ''))
    }
    const repairDesc = String(r.repairDesc ?? '').trim()
    const otherDesc = String(r.otherDesc ?? '').trim()
    return repairDesc === OTHER_REPAIR_DESC ? otherDesc : repairDesc
  }

  const showRepairDescSection = (r: FaultPointRecord) =>
    recordRepairLine(r).trim() !== OTHER_REPAIR_DESC

  /** repairDesc 非「其它」且填写了 otherDesc 时展示补充块（避免与主行重复） */
  const recordOtherSupplement = (r: FaultPointRecord) => {
    if (!hasStructuredRepairFields(r)) {
      return String(r.specialInfo || '').trim()
    }
    const repairDesc = String(r.repairDesc ?? '').trim()
    const otherDesc = String(r.otherDesc ?? '').trim()
    if (!otherDesc || repairDesc === OTHER_REPAIR_DESC) return ''
    return otherDesc
  }

  const parseStoredFaultHistory = (raw: string): FaultPointRecord[] => {
    try {
      const v = JSON.parse(raw || '[]') as unknown
      return Array.isArray(v) ? (v as FaultPointRecord[]) : []
    } catch {
      return []
    }
  }

  /**
   * 历史维修记录列表
   * @returns 历史维修记录列表
   */
  const records = ref<FaultPointRecord[]>([])

  const previewRecordImage = (record: FaultPointRecord, imgIndex: number) => {
    const imgs = record.images || []
    const urls = imgs.map((img) => resolvePreviewableUrl(img.url)).filter(Boolean)
    if (!urls.length) return
    previewImages(urls, imgIndex)
  }

  onLoad(async (options: any) => {
    const mode = String(options?.mode || '')
    if (mode !== 'repairs') {
      records.value = []
      return
    }
    let raw: string
    try {
      raw = String(uni.getStorageSync(WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY) || '')
    } catch {
      raw = ''
    }
    records.value = parseStoredFaultHistory(raw)
    try {
      uni.removeStorageSync(WORK_ORDER_REPAIR_FAULTS_HISTORY_STORAGE_KEY)
    } catch {
      /* noop */
    }
    const orderId = decodeURIComponent(String(options?.orderId || '').trim())
    if (!orderId) return
    try {
      records.value = await fetchOrderRepairFaultRecords(orderId)
    } catch {
      /* 接口失败时保留上面从 storage 解析的列表 */
    }
  })
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .historical-record-page {
    min-height: 100vh;
    background-color: $bg-light;
    box-sizing: border-box;
  }

  .page-container {
    padding: $space-md;
    padding-bottom: 40rpx;
    box-sizing: border-box;
  }

  .record-list {
    @include flex-column-gap;
    gap: $space-md;
    margin-bottom: $space-md;
  }

  .record-card {
    display: flex;
    flex-direction: column;
    gap: $space-lg;
    background-color: $bg-card;
    border-radius: $radius-md;
    padding: $space-lg;
    border: 1px solid $bg-hover;
  }

  .section {
    display: flex;
    flex-direction: column;
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

    &--emphasis {
      color: $red-500;
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
    border: 1px solid $bg-hover;
    background-color: $bg-hover;
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
    background-color: $bg-light;
    padding: $space-xs 20rpx;
    border-radius: $radius-pill;
    border: 1px solid $bg-hover;
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
    border-top: 1px solid $bg-light;
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
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 400rpx;
  }
</style>
