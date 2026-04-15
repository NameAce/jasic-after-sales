<template>
  <CommonModal
    v-model="visible"
    :title="noFaultRequired ? '工单关闭原因（无故障）' : '工单关闭原因'"
    animation="slide-up"
  >
    <view class="modal-content">
      <text v-if="noFaultRequired" class="no-fault-tip">无故障关单时关闭原因必填，请如实填写。</text>
      <!-- 输入关闭原因 -->
      <view class="textarea-wrap">
        <view v-if="noFaultRequired" class="field-label-row">
          <text class="field-label">关闭原因</text>
          <text class="text-red">*</text>
        </view>
        <textarea
          v-model="reason"
          class="reason-input"
          :class="{ 'reason-input--with-label': noFaultRequired }"
          placeholder-class="reason-input-placeholder"
          :placeholder="
            noFaultRequired
              ? '请填写关闭原因（无故障必填），如：已指导用户解决、用户申请取消…'
              : '请输入关闭的具体原因...'
          "
          :maxlength="200"
          :cursor-spacing="20"
        ></textarea>
        <view class="char-count">{{ reason.length }}/200</view>
      </view>

      <!-- 快捷标签 -->
      <view class="quick-tags-wrap">
        <text class="quick-tags-title">快捷标签</text>
        <view class="tags-list">
          <view
            v-for="(tag, index) in quickTags"
            :key="index"
            class="tag-item"
            :class="{ active: selectedTag === tag }"
            @tap="selectTag(tag)"
          >
            {{ tag }}
          </view>
        </view>
      </view>
    </view>

    <!-- 底部按钮 -->
    <template #footer>
      <view class="modal-footer">
        <view class="btns btn-cancel" @tap="onCancel">
          <text class="text">取消</text>
        </view>
        <view class="btns btn-confirm" @tap="onConfirm">
          <text class="text">确认提交</text>
        </view>
      </view>
    </template>
  </CommonModal>
</template>

<script setup lang="ts">
  import { ref, computed } from 'vue'
  import CommonModal from '@/components/CommonModal/CommonModal.vue'

  /**
   * 组件属性
   * @param modelValue 是否显示弹窗
   */
  const props = withDefaults(
    defineProps<{
      modelValue: boolean
      /** 无故障关单链路：关闭原因必填（文案与校验提示） */
      noFaultRequired?: boolean
    }>(),
    { noFaultRequired: false }
  )

  /**
   * 组件事件
   * @param e 事件
   * @param v 值
   */
  const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'confirm', reason: string): void
  }>()

  /**
   * 是否显示弹窗
   * @returns 是否显示弹窗
   */
  const visible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  // 关闭原因
  const reason = ref('')
  // 选择的标签
  const selectedTag = ref('')

  // 快捷标签
  const quickTags = ['已指导用户解决', '用户申请取消', '重复下单', '其他原因']

  /**
   * 选择标签
   * @param tag 标签
   */
  const selectTag = (tag: string) => {
    selectedTag.value = tag
    // 简易处理：将标签内容直接作为前缀或覆盖原因
    // 如果当前是其他标签的内容，直接覆盖；如果是手写内容，可以选择追加
    if (quickTags.includes(reason.value) || reason.value === '') {
      reason.value = tag
    } else {
      reason.value = `${reason.value} ${tag}`
    }
  }

  /**
   * 取消
   */
  const onCancel = () => {
    visible.value = false
  }

  /**
   * 确认提交
   */
  const onConfirm = () => {
    const text = reason.value.trim()
    if (!text) {
      uni.showToast({
        title: props.noFaultRequired ? '请填写关闭原因（无故障必填）' : '请输入关闭原因',
        icon: 'none'
      })
      return
    }
    emit('confirm', text)
    visible.value = false
    // 重置状态
    reason.value = ''
    selectedTag.value = ''
  }
</script>

<style lang="scss" scoped>
  .modal-content {
    padding: 32rpx;
  }

  .no-fault-tip {
    display: block;
    font-size: 26rpx;
    color: $text-slate-500;
    line-height: 1.5;
    margin-bottom: 24rpx;
  }

  .field-label-row {
    display: flex;
    align-items: center;
    gap: 4rpx;
    margin-bottom: 16rpx;
  }

  .field-label {
    font-size: 28rpx;
    font-weight: 600;
    color: $text-slate-800;
  }

  .text-red {
    color: $red-500;
    font-size: 28rpx;
    font-weight: bold;
  }

  .textarea-wrap {
    border-radius: 24rpx;
    padding: 0;
    margin-bottom: 32rpx;
    position: relative;
  }

  .reason-input {
    width: 100%;
    min-height: 100rpx;
    padding: $space-md;
    @include form-field-soft;
    font-size: 26rpx;
    color: $text-slate-900;
    line-height: 1.5;
    box-sizing: border-box;

    &--with-label {
      min-height: 100rpx;
    }
  }

  :deep(.reason-input-placeholder) {
    color: $text-slate-400;
  }

  .char-count {
    text-align: right;
    font-size: 24rpx;
    color: $text-slate-400;
    margin-top: 8rpx;
  }

  .quick-tags-wrap {
    .quick-tags-title {
      font-size: 28rpx;
      font-weight: 500;
      color: $text-slate-500;
      margin-bottom: 24rpx;
      display: block;
    }

    .tags-list {
      display: flex;
      flex-wrap: wrap;
      gap: 16rpx;
    }

    .tag-item {
      padding: 12rpx 32rpx;
      border-radius: 999rpx;
      border: 2rpx solid $surface-slate-200;
      background-color: $surface-slate-50;
      font-size: 26rpx;
      color: $text-slate-600;
      transition: all 0.3s;

      &.active {
        background-color: rgba($primary, 0.1);
        border-color: $primary;
        color: $primary;
      }
    }
  }

  .modal-footer {
    @include modal-footer-bar;
  }
</style>
