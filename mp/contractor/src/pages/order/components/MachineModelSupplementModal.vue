<template>
  <view v-if="visible" class="mms-mask" @click.self="onCancel">
    <view class="mms-card">
      <view class="mms-title">补录机器型号</view>
      <view class="mms-tip">
        当前工单为佳士品牌但缺少机器型号，请先选择（或输入）机器型号后再继续维修登记。
      </view>
      <view class="mms-field">
        <input
          v-model.trim="keyword"
          class="mms-input"
          placeholder="输入或搜索机器型号"
          placeholder-class="mms-placeholder"
          :maxlength="60"
          @input="onKeywordInput"
        />
        <view v-if="loading" class="mms-status">加载中...</view>
        <view v-else-if="options.length === 0" class="mms-status">
          无匹配结果，可直接使用当前输入
        </view>
        <scroll-view v-else class="mms-options" scroll-y>
          <view
            v-for="opt in options"
            :key="opt"
            :class="['mms-option', keyword === opt && 'is-active']"
            @click="onOptionPick(opt)"
          >
            <text class="mms-option-text">{{ opt }}</text>
          </view>
        </scroll-view>
      </view>
      <view class="mms-actions">
        <view class="mms-btn mms-btn--cancel" @click="onCancel">取消</view>
        <view
          :class="['mms-btn', 'mms-btn--confirm', !canSubmit && 'is-disabled']"
          @click="onConfirm"
        >
          确认并继续
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  /**
   * 机器型号补录弹窗
   *
   * 交互对齐 jasic-ui：
   * - 打开时先用空 keyword 调 `listRepairProductModelOptions` 拉默认候选
   * - 用户输入时防抖再查一次（300ms）
   * - 允许用户从列表选中，也允许直接使用手动输入的 keyword 兜底
   * - 点"确认并继续"回抛 `confirm(productModel)` 给父组件，由父组件调 `updateRepairProductModel` 后刷新详情
   */
  import { ref, watch } from 'vue'
  import { listRepairProductModelOptions } from '@/api/workOrder'

  const props = defineProps<{
    visible: boolean
    workOrderId: number
  }>()

  const emit = defineEmits<{
    (e: 'update:visible', val: boolean): void
    (e: 'confirm', productModel: string): void
    (e: 'cancel'): void
  }>()

  const keyword = ref('')
  const options = ref<string[]>([])
  const loading = ref(false)
  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  const canSubmit = () => keyword.value.trim().length > 0

  const loadOptions = async (kw: string) => {
    if (!props.workOrderId) return
    loading.value = true
    try {
      options.value = await listRepairProductModelOptions(props.workOrderId, { keyword: kw })
    } catch {
      options.value = []
    } finally {
      loading.value = false
    }
  }

  const onKeywordInput = () => {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      void loadOptions(keyword.value.trim())
    }, 300)
  }

  const onOptionPick = (opt: string) => {
    keyword.value = opt
  }

  const onCancel = () => {
    emit('update:visible', false)
    emit('cancel')
  }

  const onConfirm = () => {
    const val = keyword.value.trim()
    if (!val) {
      uni.showToast({ title: '请填写或选择机器型号', icon: 'none' })
      return
    }
    emit('confirm', val)
  }

  watch(
    () => props.visible,
    (vis) => {
      if (vis) {
        keyword.value = ''
        options.value = []
        void loadOptions('')
      }
    },
    { immediate: true }
  )
</script>

<style lang="scss" scoped>
  .mms-mask {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.48);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 999;
  }

  .mms-card {
    width: 620rpx;
    max-width: 90vw;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $space-lg;
    @include flex-col;
    gap: $space-md;
  }

  .mms-title {
    font-size: $font-lg;
    font-weight: bold;
    color: $text-main;
  }

  .mms-tip {
    font-size: $font-sm;
    color: $text-slate-500;
    line-height: 1.5;
  }

  .mms-field {
    @include flex-col;
    gap: $space-sm;
  }

  .mms-input {
    @include form-field-soft;
    width: 100%;
    height: 80rpx;
    padding: 0 $space-lg;
    font-size: 26rpx;
    color: $text-slate-900;
  }

  .mms-placeholder {
    color: $text-slate-400;
    font-size: 26rpx;
  }

  .mms-status {
    font-size: $font-sm;
    color: $text-placeholder;
    padding: 0 $space-xs;
  }

  .mms-options {
    max-height: 360rpx;
    border: 2rpx solid $border-slate;
    border-radius: $radius-md;
    background: $bg-card;
  }

  .mms-option {
    padding: 18rpx $space-md;

    &:not(:last-child) {
      border-bottom: 2rpx solid $bg-light;
    }

    &.is-active {
      background: $bg-light;
    }
  }

  .mms-option-text {
    font-size: 26rpx;
    color: $text-slate-900;
  }

  .mms-actions {
    @include flex-row;
    justify-content: flex-end;
    gap: $space-sm;
    padding-top: $space-sm;
  }

  .mms-btn {
    font-size: 26rpx;
    padding: 14rpx 28rpx;
    border-radius: $radius-sm;
  }

  .mms-btn--cancel {
    color: $text-secondary;
    background: $bg-light;
  }

  .mms-btn--confirm {
    color: $text-bg;
    background: $primary;

    &.is-disabled {
      opacity: 0.5;
    }
  }
</style>
