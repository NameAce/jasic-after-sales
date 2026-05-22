<template>
  <!-- 售后客户端小程序（报修、工单、地址）组件 CustomNavBar -->
  <view :class="rootClass" :style="rootStyle">
    <view class="custom-nav-bar__row custom-nav-bar__row--center">
      <view v-if="$slots.left" class="custom-nav-bar__left">
        <slot name="left" />
      </view>
      <view v-else-if="showBack" class="custom-nav-bar__back" @click="handleBack">
        <uni-icons type="left" :size="backIconSize" :color="iconColor" />
      </view>
      <view v-else class="custom-nav-bar__side-spacer" />
      <text class="custom-nav-bar__title" :style="titleStyle">{{ title }}</text>
      <view class="custom-nav-bar__right">
        <slot name="right" />
      </view>
    </view>

    <slot />
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { themeColors } from '@/constants/theme'
  import { useAppStore } from '@/stores'

  const props = withDefaults(
    defineProps<{
      /**
 * 导航标题
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      title: string
      /**
 * 标题对齐方式
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      titleAlign?: 'left' | 'center' | 'right'
      /**
 * 是否显示返回区（占位或按钮）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      showBack?: boolean
      /**
       * bar: 白底底边线（地址等）
       * sticky: 吸顶 + 浅阴影（工单列表等）
       * frosted: 吸顶毛玻璃（评价页）
       * transparent: 透明底，配合 tone 用于有色背景上
       * plain: 仅排版，无背景边线（登录页 fixed 顶栏）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      surface?: 'bar' | 'sticky' | 'frosted' | 'transparent' | 'plain' | 'workbench'
      /**
 * transparent 时：dark 深色图标 / light 浅色图标与标题
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      tone?: 'dark' | 'light'
      /**
 * 同时覆盖标题与返回图标颜色
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      color?: string
      /**
 * 是否显示导航阴影（sticky 等样式可关闭）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      shadow?: boolean
      backIconSize?: number
      /**
 * 覆盖自动计算的返回图标颜色
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      backIconColor?: string
      /**
 * 导航根节点背景（如 transparent 时与顶栏色块对齐），支持任意 CSS background 值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      background?: string
      /**
 * 是否 fixed 固定在顶部（登录页）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      fixed?: boolean
    }>(),
    {
      showBack: true,
      titleAlign: 'center',
      surface: 'bar',
      tone: 'dark',
      shadow: true,
      backIconSize: 20,
      fixed: false,
      background: undefined,
      color: undefined,
      backIconColor: undefined
    }
  )

  const emit = defineEmits<{ back: [] }>()

  const appStore = useAppStore()
  const statusBarHeight = computed(() => appStore.statusBarHeight)

  const iconColor = computed(() => {
    if (props.color) return props.color
    if (props.backIconColor) return props.backIconColor
    if (props.surface === 'transparent' && props.tone === 'light') return themeColors.textBg
    return themeColors.textDark
  })

  const titleStyle = computed(() => {
    const style: Record<string, string> = {
      textAlign: props.titleAlign
    }
    if (props.color) {
      style.color = props.color
      return style
    }
    if (props.surface === 'transparent' && props.tone === 'light') {
      style.color = themeColors.textBg
      return style
    }
    return style
  })

  const rootStyle = computed(() => {
    if (props.surface === 'workbench') {
      const style: Record<string, string> = {}
      if (props.background) {
        style.background = props.background
      }
      return style
    }

    const style: Record<string, string> = {
      paddingTop: `${statusBarHeight.value}px`,
      paddingLeft: '32rpx',
      paddingRight: '32rpx'
    }
    if (props.background) {
      style.background = props.background
    }
    return style
  })

  const rootClass = computed(() => [
    'custom-nav-bar',
    `custom-nav-bar--${props.surface}`,
    {
      'custom-nav-bar--fixed': props.fixed,
      'custom-nav-bar--no-shadow': !props.shadow,
      'custom-nav-bar--tone-light': props.surface === 'transparent' && props.tone === 'light'
    }
  ])

  function handleBack() {
    emit('back')
    uni.navigateBack()
  }
</script>
<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;

  .custom-nav-bar {
    box-sizing: border-box;
    position: relative;
    z-index: 9999;
  }

  .custom-nav-bar--fixed {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 9999;
  }

  .custom-nav-bar--bar {
    @include nav-bar;

    .custom-nav-bar__row {
      @include nav-bar-inner;
    }
  }

  .custom-nav-bar--sticky {
    background-color: $bg-card;
    position: sticky;
    top: 0;
    z-index: 9999;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
    @include flex-column;

    .custom-nav-bar__row {
      @include flex-between;
      padding: 0 0 $space-lg;
    }
  }

  .custom-nav-bar--frosted {
    position: sticky;
    top: 0;
    z-index: 9999;
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    border-bottom: 2rpx solid $border-lighter;

    .custom-nav-bar__row {
      @include nav-bar-inner;
    }
  }

  .custom-nav-bar--no-shadow {
    box-shadow: none !important;
  }

  .custom-nav-bar--transparent {
    background-color: transparent;

    .custom-nav-bar__row--left {
      @include flex-between;
      padding: 0 0 $space-md;
      min-height: 88rpx;
      box-sizing: border-box;
    }

    .custom-nav-bar__row--center {
      @include flex-between;
      padding: 0 0 $space-md;
    }
  }

  .custom-nav-bar--plain {
    padding: 0 0 $space-lg;
    box-sizing: border-box;

    .custom-nav-bar__row--center {
      width: 100%;
      @include flex-between;
    }

    .custom-nav-bar__back {
      width: 96rpx;
      height: 96rpx;
      @include flex-row;
    }

    .custom-nav-bar__side-spacer {
      width: 96rpx;
      height: 96rpx;
    }

    .custom-nav-bar__right {
      width: 96rpx;
      min-height: 96rpx;
    }

    .custom-nav-bar__title {
      flex: 1;
      text-align: center;
      font-size: $font-xl;
      font-weight: 700;
      color: $text-dark;
    }
  }

  .custom-nav-bar--workbench {
    position: sticky;
    top: 0;
    z-index: 9999;
    background-color: $bg-card;
    padding: $space-lg;
    padding-top: calc(var(--status-bar-height) + #{$space-lg});
    padding-bottom: $space-sm;
    border-bottom: 2rpx solid $bg-hover;

    .custom-nav-bar__row--center {
      width: 100%;
      @include flex-between;
    }

    .custom-nav-bar__left {
      width: 80rpx;
      height: 80rpx;
      flex-shrink: 0;
      @include flex-center;
    }

    .custom-nav-bar__title {
      flex: 1;
      text-align: left;
      padding: 0 $space-md;
      font-size: 36rpx;
      font-weight: 700;
      color: $text-dark;
      line-height: 1.25;
    }

    .custom-nav-bar__right {
      width: 96rpx;
      min-height: 80rpx;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      flex-shrink: 0;
    }
  }

  .custom-nav-bar__row--center {
    @include flex-between;

    .custom-nav-bar__back {
      @include nav-back-btn;
      justify-content: center;
    }

    .custom-nav-bar__side-spacer {
      width: 80rpx;
      flex-shrink: 0;
    }

    .custom-nav-bar__title {
      @include nav-title;
    }

    .custom-nav-bar__right {
      @include nav-right-placeholder;
      @include flex-row;
      justify-content: flex-end;
    }
  }

  .custom-nav-bar--bar .custom-nav-bar__row--center {
    @include nav-bar-inner;

    .custom-nav-bar__back {
      @include nav-back-btn;
    }

    .custom-nav-bar__title {
      @include nav-title;
    }

    .custom-nav-bar__right {
      @include nav-right-placeholder;
      @include flex-row;
      justify-content: flex-end;
    }
  }

  .custom-nav-bar--frosted .custom-nav-bar__row--center {
    .custom-nav-bar__back {
      @include nav-back-btn;
    }

    .custom-nav-bar__title {
      @include nav-title;
    }

    .custom-nav-bar__right {
      @include nav-right-placeholder;
      @include flex-row;
      justify-content: flex-end;
    }
  }
</style>
