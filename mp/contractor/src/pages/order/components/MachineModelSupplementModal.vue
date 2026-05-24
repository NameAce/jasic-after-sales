<template>
  <!-- 佳士缺机型强制补录：无取消、蒙层不可关，须从列表选中机型并确认后才由父级关闭 -->
  <view v-if="visible" class="mms-mask" @touchmove.stop.prevent>
    <view class="mms-card" @click.stop>
      <view class="mms-title">补录机器型号</view>
      <view class="mms-tip">
        佳士品牌工单在维修登记或复检登记前若无机器型号须先补录。补录后不可再次修改。请从下方已启用机型列表中选择。
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
          无匹配结果，请调整关键词，或从列表选择已启用的机器型号
        </view>
        <scroll-view v-else class="mms-options" scroll-y>
          <view
            v-for="opt in options"
            :key="opt"
            :class="['mms-option', pickedModel === opt && 'is-active']"
            @click="onOptionPick(opt)"
          >
            <text class="mms-option-text">{{ opt }}</text>
          </view>
        </scroll-view>
      </view>
      <view class="mms-actions">
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
   * 佳士缺机型强制补录弹窗：须从归属总部已启用机型列表中点选一项后确认，不可取消或点蒙层关闭。
   * - 打开时用空 keyword 拉候选，并保存全量列表用于确认校验
   * - 用户输入时防抖再查（300ms）；仅点选列表项视为已选机型
   * - 确认：所选型号须在全量已启用列表中，成功后由父级关闭弹窗
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  import { ref, watch, computed } from 'vue'
  import { listRepairProductModelOptions } from '@/api/workOrder'

  const props = defineProps<{
    visible: boolean
    workOrderId: number
  }>()

  const emit = defineEmits<{
    (e: 'confirm', productModel: string): void
  }>()

  const keyword = ref('')
  /** 用户从候选列表点选的机型（未点选前不可确认，弹窗亦不可关闭） */
  const pickedModel = ref('')
  const options = ref<string[]>([])
  /**
 * 打开弹窗时「全量」已启用机型，用于与后端一致：仅允许选库内型号
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const enabledAllModels = ref<string[]>([])
  const loading = ref(false)
  let debounceTimer: ReturnType<typeof setTimeout> | null = null

  const canSubmit = computed(() => {
    const val = pickedModel.value.trim()
    return !!val && enabledAllModels.value.includes(val)
  })

  const loadOptions = async (kw: string) => {
    if (!props.workOrderId) return
    loading.value = true
    try {
      const list = await listRepairProductModelOptions(props.workOrderId, { keyword: kw })
      options.value = list
      if (!String(kw || '').trim()) {
        enabledAllModels.value = list
      }
    } catch {
      options.value = []
    } finally {
      loading.value = false
    }
  }

  const onKeywordInput = () => {
    // 搜索词变更后须重新从列表点选，避免仅输入文字绕过点选约束
    pickedModel.value = ''
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      void loadOptions(keyword.value.trim())
    }, 300)
  }

  const onOptionPick = (opt: string) => {
    keyword.value = opt
    pickedModel.value = opt
  }

  const onConfirm = () => {
    if (!canSubmit.value) {
      uni.showToast({ title: '请从列表中选择机器型号', icon: 'none' })
      return
    }
    const val = pickedModel.value.trim()
    if (!enabledAllModels.value.length) {
      uni.showToast({ title: '未加载到可选机型，请稍后重试', icon: 'none' })
      return
    }
    if (!enabledAllModels.value.includes(val)) {
      uni.showToast({ title: '请选择已启用的机器型号', icon: 'none' })
      return
    }
    emit('confirm', val)
  }

  watch(
    () => props.visible,
    (vis) => {
      if (vis) {
        keyword.value = ''
        pickedModel.value = ''
        options.value = []
        enabledAllModels.value = []
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
    justify-content: stretch;
    padding-top: $space-sm;
  }

  .mms-btn {
    flex: 1;
    text-align: center;
    font-size: 26rpx;
    padding: 14rpx 28rpx;
    border-radius: $radius-sm;
  }

  .mms-btn--confirm {
    color: $text-bg;
    background: $primary;

    &.is-disabled {
      opacity: 0.5;
    }
  }
</style>
