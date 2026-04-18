<template>
  <CommonModal v-model="visible" title="转单申请" animation="slide-up" safe-area>
    <!-- 内容 -->
    <view class="modal-body">
      <view class="form-group">
        <text class="label">选择转单网点<text class="text-red">*</text></text>
        <view class="select-wrap">
          <picker mode="selector" :range="networkList" range-key="name" @change="onNetworkChange">
            <view class="picker-value">
              <text :class="['text', !selectedNetworkValue && 'placeholder']">
                {{ selectedNetworkValue ? selectedNetworkValue.name : '请选择转单网点' }}
              </text>
              <uni-icons type="down" size="24" color="#cbd5e1"></uni-icons>
            </view>
          </picker>
        </view>
      </view>
      <!-- 转单原因说明 -->
      <view class="form-group">
        <text class="label">转单原因说明<text class="text-red">*</text></text>
        <textarea
          v-model="reasonValue"
          class="textarea"
          placeholder="请输入详细的转单原因，以便审核人员快速处理..."
          placeholder-class="placeholder-text"
        ></textarea>
      </view>

      <!-- 底部按钮 -->
      <view class="modal-footer">
        <view class="btns btn-cancel" @tap="onCancel">
          <text class="text">取消</text>
        </view>
        <view class="btns btn-confirm" @tap="onConfirm">
          <text class="text">确认转单</text>
        </view>
      </view>
    </view>
  </CommonModal>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import CommonModal from '@/components/CommonModal/CommonModal.vue'

  type NetworkItem = { id: string | number; name: string; [k: string]: any }

  /**
   * 转单弹窗
   * @param modelValue 是否显示
   * @param networkList 网点列表
   * @param selectedNetwork 选中网点
   * @param reason 转单原因
   * @returns void
   */
  const props = withDefaults(
    defineProps<{
      modelValue: boolean
      networkList: NetworkItem[]
      selectedNetwork?: NetworkItem | null
      reason?: string
    }>(),
    {
      selectedNetwork: null,
      reason: ''
    }
  )
  /**
   * 转单弹窗 - 事件
   * @param update:modelValue 是否显示
   * @param update:selectedNetwork 选中网点
   * @param update:reason 转单原因
   * @param cancel 取消
   * @param confirm 确认
   * @returns void
   */
  const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'update:selectedNetwork', v: NetworkItem | null): void
    (e: 'update:reason', v: string): void
    (e: 'cancel'): void
    (e: 'confirm', payload: { selectedNetwork: NetworkItem | null; reason: string }): void
  }>()

  /**
   * 转单弹窗 - 是否显示
   * @param modelValue 是否显示
   */
  const visible = computed({
    get: () => props.modelValue,
    set: (v: boolean) => emit('update:modelValue', v)
  })

  /**
   * 转单弹窗 - 选中网点
   * @param selectedNetwork 选中网点
   */
  const selectedNetworkValue = computed({
    get: () => props.selectedNetwork ?? null,
    set: (v: NetworkItem | null) => emit('update:selectedNetwork', v)
  })

  /**
   * 转单弹窗 - 转单原因
   * @param reason 转单原因
   */
  const reasonValue = computed({
    get: () => props.reason ?? '',
    set: (v: string) => emit('update:reason', v)
  })

  /**
   * 转单弹窗 - 网点选择
   * @param e 选择事件
   * @returns void
   */
  const onNetworkChange = (e: any) => {
    const idx = Number(e?.detail?.value ?? -1)
    if (Number.isNaN(idx) || idx < 0 || idx >= props.networkList.length) return
    selectedNetworkValue.value = props.networkList[idx] ?? null
  }

  /**
   * 转单弹窗 - 取消
   * @returns void
   */
  const onCancel = () => {
    emit('update:modelValue', false)
    emit('cancel')
  }

  /**
   * 转单弹窗 - 确认
   * @returns void
   */
  const onConfirm = () => {
    emit('confirm', { selectedNetwork: selectedNetworkValue.value, reason: reasonValue.value })
  }
</script>

<style lang="scss" scoped>
  /* Modal（弹窗容器由 CommonModal 提供，这里保留业务内容样式） */
  .modal-body {
    padding: 48rpx 48rpx 0;
    display: flex;
    flex-direction: column;
    gap: 24rpx;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 16rpx;

    .label {
      font-size: 28rpx;
      font-weight: 600;
      color: $text-slate-900;
      display: block;
    }

    .select-wrap {
      position: relative;

      .picker-value {
        height: 96rpx;
        @include form-field-frame;
        background-color: $bg-card;
        padding: 0 32rpx;
        @include flex-between;

        .text {
          font-size: 28rpx;
          color: $text-slate-900;

          &.placeholder {
            color: $text-slate-400;
          }
        }

        .icon {
          font-size: 40rpx;
          color: $text-slate-400;
        }
      }
    }

    .textarea {
      width: 100%;
      min-height: 240rpx;
      @include form-field-frame;
      background-color: $bg-card;
      padding: 32rpx;
      font-size: 28rpx;
      color: $text-slate-900;
      box-sizing: border-box;

      .placeholder-text {
        color: $text-slate-400;
      }
    }
  }

  .modal-footer {
    @include modal-footer-bar;
    border-top: none;
    padding-top: $space-sm;
    padding-bottom: 0;
  }
</style>
