<template>
  <CommonModal
    v-model="visible"
    :title="noFaultRequired ? '工单关闭原因（无故障）' : '工单关闭原因'"
    animation="slide-up"
    safe-area
  >
    <view class="modal-content">
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
  }
</script>

<style lang="scss" scoped>
  .modal-content {
    padding: 32rpx;
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

  .modal-footer {
    @include modal-footer-bar;
  }
</style>
