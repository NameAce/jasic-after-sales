<template>
  <view>
    <!-- 维修信息标题 -->
    <view class="od-apply-section-header">
      <view class="section-mark"></view>
      <text class="section-title">维修信息</text>
    </view>
    <!-- 维修信息内容 -->
    <view class="info-list fault-form">
      <!-- 故障判定 -->
      <view class="fault-form-item">
        <text class="fault-form-label">故障判定 <text class="text-red">*</text></text>
        <picker mode="selector" :range="faultJudgeOptions" @change="onFaultJudgePicker">
          <view class="fault-picker">
            <text :class="['fault-picker-text', faultJudge ? '' : 'placeholder']">
              {{ faultJudge || '请选择' }}
            </text>
            <uni-icons type="down" size="15" color="#cbd5e1"></uni-icons>
          </view>
        </picker>
      </view>
      <!-- 维修报价（与接单接口一并提交） -->
      <template v-if="faultJudge === '有故障'">
        <view class="fault-form-item">
          <text class="fault-form-label">维修报价</text>
          <view class="fault-input-wrap">
            <text class="fault-currency">¥</text>
            <input
              v-model="repairQuote"
              class="fault-input"
              type="digit"
              placeholder="请输入维修报价"
              placeholder-class="fault-placeholder"
            />
          </view>
        </view>
        <!-- 报价说明 -->
        <view class="fault-form-item">
          <text class="fault-form-label">维修报价说明</text>
          <textarea
            v-model="quoteDesc"
            class="fault-textarea"
            placeholder="请输入维修报价说明"
            placeholder-class="fault-placeholder"
            :maxlength="-1"
            auto-height
          />
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
  /**
 * 故障判定 Form
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const faultJudgeOptions = ['有故障', '无故障']
  /**
 * 故障判定
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const faultJudge = defineModel<string>('faultJudge', { default: '' })
  /**
 * 维修报价
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const repairQuote = defineModel<string>('repairQuote', { default: '' })
  /**
 * 报价说明
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const quoteDesc = defineModel<string>('quoteDesc', { default: '' })

  /**
   * 故障判定选择
   * @param e 事件
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const onFaultJudgePicker = (e: { detail: { value: string | number } }) => {
    const idx = Number(e.detail.value)
    const next = faultJudgeOptions[idx] ?? ''
    faultJudge.value = next
    if (next === '无故障') {
      repairQuote.value = ''
      quoteDesc.value = ''
    }
  }
</script>

<style lang="scss" scoped>
  .od-apply-section-header {
    @include section-title-bar;
  }

  .info-list.fault-form {
    @include flex-col;
    gap: $space-lg;
    padding: 0 $space-md;
  }

  .fault-form {
    gap: $space-lg;

    .fault-form-item {
      @include flex-col;
      gap: $space-sm;
    }

    .fault-form-label {
      font-size: $font-md;
      color: $text-slate-500;
      line-height: 1.4;
    }

    .fault-picker {
      @include flex-between;
      @include form-field-frame;
      padding: 20rpx $space-lg;
      transition: all 0.2s;
      border: none;
    }

    .fault-picker-text {
      font-size: 26rpx;
      color: $text-slate-900;
      font-weight: 400;

      &.placeholder {
        color: $text-slate-400;
      }
    }

    .fault-input-wrap {
      position: relative;
      @include flex-row;
    }

    .fault-currency {
      position: absolute;
      left: $space-lg;
      z-index: 1;
      font-size: 26rpx;
      color: $text-slate-500;
      line-height: 1;
    }

    .fault-input,
    .fault-textarea {
      width: 100%;
      font-size: 26rpx;
      color: $text-slate-900;
      @include form-field-soft;
    }

    .fault-input {
      height: 80rpx;
      padding: 0 $space-lg 0 64rpx;

      &::placeholder {
        color: $text-slate-400;
      }
    }

    .fault-textarea {
      min-height: 100rpx;
      padding: $space-md;

      &::placeholder {
        color: $text-slate-400;
      }
    }
  }

  :deep(.fault-placeholder) {
    color: $text-slate-400;
  }
</style>
