<template>
  <view class="feedback-list-page">
    <custom-nav-bar title="投诉与建议" surface="sticky" :shadow="false" />
    <scroll-view class="feedback-list-scroll" scroll-y>
      <view class="page-index list-content">
        <view class="search-wrap">
          <view class="search-box">
            <view
              class="search-input-wrap"
              :class="{ 'search-input-wrap--with-clear': dateFilterActive }"
            >
              <uni-datetime-picker
                ref="datePickerRef"
                :model-value="daterange"
                type="daterange"
                rangeSeparator="至"
                return-type="string"
                :border="false"
                :clear-icon="false"
                @update:model-value="handleDateRangeModelUpdate"
                @input="handleDateRangeModelUpdate"
                @change="handleDateRangeChange"
              />
              <view
                v-if="dateFilterActive"
                class="search-clear-hit"
                @touchstart.stop.prevent="handleDateRangeClear"
                @click.stop="handleDateRangeClear"
              >
                <uni-icons type="closeempty" size="18" color="#94a3b8" />
              </view>
            </view>
          </view>
        </view>

        <view v-if="records.length === 0" class="empty-wrap">
          <ListEmpty title="暂无反馈记录" desc="提交后可在此查看处理进度" />
        </view>

        <view v-else class="feedback-list">
          <view v-for="item in records" :key="item.id" class="feedback-card">
            <view class="feedback-card-head">
              <text class="feedback-card-title">反馈内容</text>
              <text class="feedback-card-time">{{ item.submitTime }}</text>
            </view>
            <text class="feedback-card-content">{{ item.content }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <base-button>
      <view class="btn btn-primary" @click="goApply">投诉与建议申请</view>
    </base-button>
  </view>
</template>

<script setup lang="ts">
  import { nextTick, ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import CustomNavBar from '@/components/CustomNavBar/CustomNavBar.vue'
  import BaseButton from '@/components/BaseButton/BaseButton.vue'
  import ListEmpty from '@/components/ListEmpty/ListEmpty.vue'
  import { listFeedback, type FeedbackRecordDTO } from '@/api/feedback'

  type FeedbackListItem = {
    id: string
    content: string
    submitTime: string
  }

  const records = ref<FeedbackListItem[]>([])
  const daterange = ref<string[]>([])
  /** 是否展示日期清空按钮（独立状态，避免小程序端 v-model 不同步） */
  const dateFilterActive = ref(false)
  /** 日期组件实例，清空时同步调用组件内部 clear */
  const datePickerRef = ref<{ clear?: () => void } | null>(null)

  /**
   * 统一解析日期范围组件返回值。
   * 小程序端 `v-model` 偶发不同步，需同时兼容 `@change` 直传数组、字符串及 `detail` 包装。
   */
  const resolveDateRangeValue = (payload: unknown): string[] => {
    let raw: unknown = payload
    if (raw && typeof raw === 'object' && 'detail' in (raw as Record<string, unknown>)) {
      raw = (raw as { detail: unknown }).detail
    }
    if (Array.isArray(raw)) {
      return raw.map((item) => String(item ?? '').trim())
    }
    const text = String(raw ?? '').trim()
    if (!text) return []
    return text.split(',').map((item) => item.trim())
  }

  /**
   * 判断日期范围是否已选择（任一端非空即视为有筛选）。
   */
  const hasDateRangeValue = (range: unknown) =>
    resolveDateRangeValue(range).some((item) => item.length > 0)

  /**
   * 同步日期筛选激活态，供清空按钮显隐与列表查询共用。
   */
  const syncDateFilterActive = (range: unknown) => {
    dateFilterActive.value = hasDateRangeValue(range)
  }

  /**
   * 统一格式化后端时间，保证列表展示稳定。
   */
  const formatTime = (raw: unknown) => {
    const source = String(raw ?? '').trim()
    if (!source) return '--'
    return source.replace('T', ' ').slice(0, 16)
  }

  /**
   * 将后端 DTO 映射为页面展示模型，兼容字段差异。
   */
  const mapFeedbackItem = (item: FeedbackRecordDTO, index: number): FeedbackListItem => {
    const content = String(item.content ?? item.feedbackContent ?? '').trim()
    const submitTime = item.submitTime ?? item.createTime ?? item.createdAt ?? ''
    return {
      id: String(item.id ?? `${submitTime}-${index}`),
      content: content || '--',
      submitTime: formatTime(submitTime)
    }
  }

  /**
   * 拉取反馈列表，支持按提交日期区间筛选。
   */
  const loadList = async () => {
    const [beginCreateTime = '', endCreateTime = ''] = resolveDateRangeValue(daterange.value)
    try {
      const res = await listFeedback({
        pageNum: 1,
        pageSize: 50,
        beginCreateTime: beginCreateTime || undefined,
        endCreateTime: endCreateTime || undefined
      })
      const rows = Array.isArray(res.data?.records) ? res.data.records : []
      records.value = rows.map(mapFeedbackItem)
    } catch {
      records.value = []
    }
  }

  /**
   * 处理 `update:model-value` / `input`，保证父级状态与组件一致。
   */
  const handleDateRangeModelUpdate = (payload: unknown) => {
    daterange.value = resolveDateRangeValue(payload)
    syncDateFilterActive(daterange.value)
  }

  /**
   * 日期范围确认后刷新列表。
   * 微信小程序部分场景 `@change` 不传值，需在 nextTick 回读 model，必要时按“已确认选择”兜底亮显清空按钮。
   */
  const handleDateRangeChange = (payload: unknown) => {
    const resolved = resolveDateRangeValue(payload)
    if (hasDateRangeValue(resolved)) {
      daterange.value = resolved
      syncDateFilterActive(resolved)
      void loadList()
      return
    }

    void nextTick(() => {
      const synced = resolveDateRangeValue(daterange.value)
      if (hasDateRangeValue(synced)) {
        daterange.value = synced
        syncDateFilterActive(synced)
      } else if (payload === undefined || payload === null) {
        // 小程序端常见：change 触发但不带参数，此时组件内已展示选中日期
        dateFilterActive.value = true
      } else {
        syncDateFilterActive(synced)
      }
      void loadList()
    })
  }

  /**
   * 清空日期范围并立即刷新列表。
   */
  const handleDateRangeClear = () => {
    daterange.value = []
    dateFilterActive.value = false
    datePickerRef.value?.clear?.()
    void loadList()
  }

  const goApply = () => {
    uni.navigateTo({ url: '/pages/feedback/apply' })
  }

  onShow(() => {
    void loadList()
  })
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;
  $feedback-date-muted-color: $text-slate-400;

  .feedback-list-page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    box-sizing: border-box;
    overflow: hidden;

    /**
     * 反馈页内底部固定按钮层级下调，避免遮挡日期范围选择器弹窗。
     */
    :deep(.base-btn) {
      z-index: 10;
    }
  }

  .feedback-list-scroll {
    flex: 1;
    height: 0;
    min-height: 0;
  }

  .list-content {
    padding-top: 0;
    padding-left: $space-lg;
    padding-right: $space-lg;
    padding-bottom: 220rpx;
  }

  .search-wrap {
    background-color: $bg-card;
    @include flex-column-gap;
    box-sizing: border-box;
    padding: $space-sm $space-lg $space-md;
    margin: 0 (-$space-lg) 0;
  }

  .search-box {
    @include flex-row;
    align-items: center;
    background-color: $bg-hover;
    height: 88rpx;
    border-radius: $radius-lg;
    padding: 0 $space-lg;
    box-sizing: border-box;
  }

  .search-input-wrap {
    position: relative;
    display: flex;
    align-items: center;
    flex: 1;
    min-width: 0;
    height: 100%;

    /* 为右侧自定义清空按钮预留空间，避免与日期文案重叠 */
    &--with-clear {
      :deep(.uni-date-x) {
        padding-right: 64rpx;
        box-sizing: border-box;
      }
    }
  }

  .search-clear-hit {
    position: absolute;
    right: 0;
    top: 0;
    bottom: 0;
    z-index: 3;
    @include flex-center;
    width: 64rpx;
    height: 100%;
  }

  :deep(.uni-date) {
    width: 100%;
    height: 100%;
  }

  :deep(.uni-date-x) {
    width: 100%;
    height: 100%;
    background: transparent;
    border: none;
    padding: 0;
  }

  :deep(.uni-date-x--border) {
    border: none !important;
  }

  :deep(.uni-date-editor) {
    display: flex;
    align-items: center;
    width: 100%;
    height: 100%;
    border: none !important;
    background: transparent !important;
  }

  :deep(.uni-date-range) {
    display: grid;
    grid-template-columns: auto 1fr auto 1fr;
    align-items: center;
    width: 100%;
    height: 100%;
    column-gap: 0;
  }

  :deep(.uni-date-range .icon-calendar) {
    grid-column: 1;
    justify-self: center;
    padding-left: 0;
    margin-right: 12rpx;
  }

  :deep(.uni-date__x-input) {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    font-size: $font-md;
    line-height: 1;
    color: $feedback-date-muted-color !important;
    min-width: 0;
  }

  :deep(.uni-date-range > .uni-date__x-input:first-of-type) {
    grid-column: 2;
  }

  :deep(.uni-date-range > .uni-date__x-input:last-of-type) {
    grid-column: 4;
  }

  /* 开始/结束日期在各自分段内水平居中。 */
  :deep(.uni-date-range .uni-date__x-input),
  :deep(.uni-date-range .uni-date__x-input-text),
  :deep(.uni-date-range .uni-date__x-input text) {
    width: 100%;
    text-align: center !important;
    white-space: nowrap;
    color: $feedback-date-muted-color !important;
  }

  :deep(.placeholder-text) {
    color: $feedback-date-muted-color;
  }

  /**
   * 日期范围组件占位文案颜色对齐 order/list 的 placeholder 视觉。
   */
  :deep(.uni-date__x-input-placeholder),
  :deep(.uni-date__x-placeholder),
  :deep(.uni-date-x__placeholder),
  :deep(.uni-date-x-placeholder),
  :deep(.uni-date__x-input-placeholder-text),
  :deep(.uni-date-x .uni-date__x-input-placeholder-text),
  :deep(.uni-date__x-input text[data-placeholder='true']),
  :deep(.uni-date-x .uni-date__x-input .uni-date__x-input-text),
  :deep(.uni-date-x .uni-date__x-input text) {
    color: $feedback-date-muted-color !important;
  }

  :deep(.uni-date__x-input::placeholder) {
    color: $feedback-date-muted-color !important;
  }

  /* 与 order/list 一致：占位提示文案使用浅灰色。 */
  :deep(.uni-date__x-input-text.uni-date__x-input-placeholder-text),
  :deep(.uni-date-x .uni-date__x-input text.uni-date__x-input-placeholder-text) {
    color: $feedback-date-muted-color !important;
  }

  /* “至”固定在两个日期之间，避免视觉偏移。 */
  :deep(.uni-date__x-separator),
  :deep(.uni-date-x__separator),
  :deep(.uni-date-x .uni-date__x-input-separator),
  :deep(.uni-date-x .uni-date__x-separator text),
  :deep(.uni-date-range .range-separator) {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    grid-column: 3;
    justify-self: center;
    color: $feedback-date-muted-color !important;
    flex-shrink: 0;
    margin: 0 16rpx;
    line-height: 1;
  }

  :deep(.uni-date__x-icon) {
    color: $feedback-date-muted-color !important;
  }

  :deep(.uni-date__x-icon) {
    display: none !important;
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
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: $space-lg;
    border: 2rpx solid $bg-hover;
    box-sizing: border-box;
  }

  .feedback-card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $space-sm;
  }

  .feedback-card-title {
    font-size: $font-lg;
    color: $text-slate-900;
    font-weight: bold;
  }

  .feedback-card-time {
    font-size: 22rpx;
    color: $text-slate-400;
  }

  .feedback-card-content {
    display: block;
    font-size: 26rpx;
    color: $text-slate-700;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-all;
  }
</style>
