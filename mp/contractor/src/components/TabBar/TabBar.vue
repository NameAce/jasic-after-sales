<template>
  <!-- 胶囊标签栏 -->
  <view v-if="props.variant === 'pill'" :class="['tabs-pill', props.sticky && 'sticky-tabs']">
    <view class="tabs-pill-inner">
      <view
        v-for="tab in props.tabs"
        :key="tab.value"
        :class="['tab-item', String(tab.value) === String(props.modelValue) && 'active']"
        @tap="onSelect(tab.value)"
      >
        <text class="text">{{ tab.label }}</text>
      </view>
    </view>
  </view>
  <!-- 下划线标签栏 -->
  <view v-else :class="rootClass">
    <scroll-view
      v-if="props.scrollable"
      class="tabs-scroll"
      scroll-x
      :show-scrollbar="false"
      :scroll-into-view="props.scrollIntoView"
      scroll-with-animation
    >
      <view class="tabs-inner">
        <view
          v-for="tab in props.tabs"
          :id="getTabId(tab.value)"
          :key="tab.value"
          :class="['tab-item', String(tab.value) === String(props.modelValue) && 'active']"
          @tap="onSelect(tab.value)"
        >
          <text class="text">{{ tab.label }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 非滚动标签栏 -->
    <view v-else class="tabs-inner">
      <view
        v-for="tab in props.tabs"
        :id="getTabId(tab.value)"
        :key="tab.value"
        :class="['tab-item', String(tab.value) === String(props.modelValue) && 'active']"
        @tap="onSelect(tab.value)"
      >
        <text class="text">{{ tab.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'

  type TabItem = { label: string; value: string }
  type TabBarVariant = 'pill' | 'underline'
  type TabBarTone = 'list' | 'sheet'
  type UnderlineStyle = 'default' | 'rounded'
  type TabItemPadding = 'sm' | 'md'

  // 标签栏属性
  const props = withDefaults(
    defineProps<{
      tabs: TabItem[]
      modelValue: string
      variant?: TabBarVariant
      scrollable?: boolean
      sticky?: boolean
      tone?: TabBarTone
      underline?: UnderlineStyle
      padding?: TabItemPadding
      scrollIntoView?: string
      idPrefix?: string
    }>(),
    {
      variant: 'underline',
      scrollable: true,
      sticky: false,
      tone: 'list',
      underline: 'default',
      padding: 'md',
      scrollIntoView: '',
      idPrefix: ''
    }
  )

  // 标签栏事件
  const emit = defineEmits<{
    (e: 'update:modelValue', v: string): void
    (e: 'change', v: string): void
  }>()

  // 根类
  const rootClass = computed(() => {
    if (props.variant === 'pill') return []
    return [
      'tabs-wrap',
      `tabs-wrap--${props.tone}`,
      props.sticky ? 'sticky-tabs' : '',
      `underline--${props.underline}`,
      `item-padding--${props.padding}`
    ]
  })
  /**
   * 获取标签ID
   * @param value 标签值
   * @returns 标签ID
   */
  const getTabId = (value: string) => {
    if (!props.idPrefix) return undefined
    return `${props.idPrefix}${value}`
  }

  /**
   * 选择标签
   * @param value 标签值
   * @returns void
   */
  const onSelect = (value: string) => {
    emit('change', value)
    emit('update:modelValue', value)
  }
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;

  .sticky-tabs {
    position: sticky;
    top: 0;
    z-index: $z-nav-tabbar;
  }

  /* pill/segment */
  .tabs-pill {
    @include pill-tabs;
    width: 100%;

    .tabs-pill-inner {
      width: 100%;
      display: flex;
      flex-direction: row;
    }
  }

  /* underline tabs (scroll x) */
  .tabs-wrap {
    width: 100%;
  }

  .tabs-wrap--list {
    border-bottom: 2rpx solid $bg-hover;
  }

  .tabs-wrap--sheet {
    background-color: $bg-card;
    border-bottom: 2rpx solid $bg-hover;
  }

  .tabs-scroll {
    width: 100%;
  }

  .tabs-inner {
    @include tabs-track;
  }

  .item-padding--sm .tab-item {
    padding: $space-sm 0 $space-md;
  }

  .item-padding--md .tab-item {
    padding: $space-md 0;
  }

  .underline--default .tab-item {
    @include tab-underline-item;
  }

  .underline--rounded .tab-item {
    @include tab-underline-item($text-color: $text-slate-500, $bar-height: 6rpx, $bar-radius: 6rpx);
  }
</style>
