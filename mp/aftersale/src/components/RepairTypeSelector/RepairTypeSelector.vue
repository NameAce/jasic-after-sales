<template>
  <!-- 维修路径选择器 -->
  <view class="repair-types">
    <!-- 维修路径选项 -->
    <view
      v-for="(item, index) in options"
      :key="index"
      class="type-item"
      :class="{ active: modelValue === item.value }"
      @click="handleClick(item.value)"
    >
      <uni-icons
        :type="item.icon"
        size="20"
        :color="modelValue === item.value ? themeColor.primary : themeColor.info"
      ></uni-icons>
      <text>{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { themeColor } from '@/constants/theme'

  interface RepairTypeOption {
    label: string
    value: string
    icon: string
  }

  // 定义 props
  defineProps<{
    // 维修路径
    modelValue: string
    // 维修路径选项
    options: RepairTypeOption[]
  }>()

  // 定义事件
  const emit = defineEmits<{
    (e: 'update:modelValue', value: string): void
  }>()

  // 点击选择维修路径
  const handleClick = (value: string) => {
    emit('update:modelValue', value)
  }
</script>
<style scoped lang="scss">
  .repair-types {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: $space-md;

    .type-item {
      @include flex-column-center;
      gap: $space-xs;
      padding: $space-sm;
      border: 1px solid $border-color;
      border-radius: $radius-lg;
      transition: all 0.3s;

      text {
        font-size: $font-sm;
        color: $text-secondary;
      }

      &.active {
        border-color: $primary;
        background-color: rgba($primary, 0.05);

        text {
          color: $primary;
        }
      }
    }
  }
</style>
