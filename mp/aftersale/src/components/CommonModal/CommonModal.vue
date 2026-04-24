<template>
  <view v-if="modelValue" class="cm-mask" @tap="onMaskTap">
    <view
      :class="['cm-panel', animationClass]"
      :style="{ maxHeight, maxWidth, borderTopLeftRadius: radius, borderTopRightRadius: radius }"
      @tap.stop
    >
      <view v-if="showHandle" class="cm-handle-wrap">
        <view class="cm-handle"></view>
      </view>

      <view v-if="$slots.header || title" class="cm-header">
        <slot name="header">
          <text class="cm-title">{{ title }}</text>
        </slot>
      </view>

      <view class="cm-body">
        <slot></slot>
      </view>

      <view v-if="$slots.footer" class="cm-footer">
        <slot name="footer"></slot>
      </view>

      <view v-if="safeArea" class="cm-safe-area"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'

  type Animation = 'none' | 'slide-up'

  const props = withDefaults(
    defineProps<{
      modelValue: boolean
      title?: string
      closeOnMask?: boolean
      showHandle?: boolean
      safeArea?: boolean
      animation?: Animation
      maxHeight?: string
      maxWidth?: string
      radius?: string
    }>(),
    {
      title: '',
      closeOnMask: true,
      showHandle: true,
      safeArea: false,
      animation: 'none',
      maxHeight: '90vh',
      maxWidth: '896rpx',
      radius: '48rpx',
    },
  )

  const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'close'): void
  }>()

  const animationClass = computed(() => {
    if (props.animation === 'slide-up') return 'cm-animate-slide-up'
    return ''
  })

  const onMaskTap = () => {
    if (!props.closeOnMask) return
    emit('update:modelValue', false)
    emit('close')
  }
</script>

<style lang="scss" scoped>
  .cm-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(15, 23, 42, 0.4);
    backdrop-filter: blur(4px);
    z-index: $z-modal;
    display: flex;
    align-items: flex-end;
    justify-content: center;
  }

  .cm-panel {
    background-color: $bg-card;
    width: 100%;
    border-radius: 48rpx 48rpx 0 0;
    overflow: hidden;
    box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.1);
    position: relative;
    display: flex;
    flex-direction: column;
  }

  .cm-animate-slide-up {
    animation: cmSlideInFromBottom 0.3s ease-out;
  }

  .cm-handle-wrap {
    display: flex;
    height: 48rpx;
    width: 100%;
    align-items: center;
    justify-content: center;
    padding-top: 16rpx;
    box-sizing: border-box;
  }

  .cm-handle {
    height: 12rpx;
    width: 96rpx;
    border-radius: 9999rpx;
    background-color: $border-slate;
  }

  .cm-header {
    padding: 16rpx 32rpx 0;
    text-align: center;
    border-bottom: 2rpx solid $bg-hover;
    box-sizing: border-box;
  }

  .cm-title {
    color: $text-slate-900;
    font-size: 36rpx;
    font-weight: bold;
    line-height: 1.5;
    display: block;
  }

  .cm-body {
    flex: 1;
    min-height: 0;
  }

  .cm-footer {
    box-sizing: border-box;
  }

  .cm-safe-area {
    height: 48rpx;
    background-color: $bg-card;
  }

  @keyframes cmSlideInFromBottom {
    from {
      transform: translateY(100%);
    }
    to {
      transform: translateY(0);
    }
  }
</style>
