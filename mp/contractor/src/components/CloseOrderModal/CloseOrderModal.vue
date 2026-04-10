<template>
  <CommonModal v-model="visible" title="工单关闭原因" animation="slide-up">
    <view class="modal-content">
      <!-- 输入关闭原因 -->
      <view class="textarea-wrap">
        <textarea
          v-model="reason"
          class="reason-input"
          placeholder="请输入关闭的具体原因..."
          :maxlength="200"
          :cursor-spacing="20"
        />
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
  const props = defineProps<{
    modelValue: boolean
  }>()

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
    if (!reason.value.trim()) {
      uni.showToast({ title: '请输入关闭原因', icon: 'none' })
      return
    }
    emit('confirm', reason.value)
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

  .textarea-wrap {
    background-color: $surface-slate-50;
    border-radius: 24rpx;
    padding: 24rpx;
    margin-bottom: 32rpx;
    position: relative;
  }

  .reason-input {
    width: 100%;
    height: 200rpx;
    font-size: 28rpx;
    color: $text-slate-700;
    line-height: 1.5;
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
