<template>
  <!-- 承修方小程序（网点/总部工单处理、派工）组件 RepairTypeSelector -->
  <view class="repair-types">
    <view
      v-for="(item, index) in options"
      :key="index"
      class="type-item"
      :class="{ active: modelValue === item.value }"
      @click="handleClick(item.value)"
    >
      <!-- 图标 -->
      <uni-icons
        :type="item.icon as any"
        size="20"
        :color="modelValue === item.value ? themeColor.primary : themeColor.info"
      ></uni-icons>
      <!-- 标签 -->
      <text>{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { themeColor } from '@/theme/colors'

  // 维修路径选项
  interface RepairTypeOption {
    label: string
    value: string
    icon: string
  }

  // 组件属性
  defineProps<{
    // 维修路径
    modelValue: string
    // 维修路径选项
    options: RepairTypeOption[]
  }>()

  // 组件事件
  const emit = defineEmits<{
    (e: 'update:modelValue', value: string): void
  }>()

  /**
   * 选择维修路径
   * @param value 维修路径
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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
