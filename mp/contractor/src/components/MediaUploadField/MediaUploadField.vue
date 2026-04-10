<template>
  <view>
    <view v-if="showLabelRow" class="shipping-label">
      <text>{{ label }}</text>
      <text v-if="tip" class="upload-tip">{{ tip }}</text>
    </view>
    <!-- 文件选择器 -->
    <uni-file-picker
      v-model="innerValue"
      :file-mediatype="fileMediatype"
      :limit="limit"
      :max-file-size="maxFileSize"
      :list-styles="listStyles"
      :image-styles="imageStyles"
      :del-icon="delIcon"
      @select="(e) => emit('select', e)"
      @delete="(e) => emit('delete', e)"
    >
      <slot name="add">
        <view class="file-picker-add-box">
          <view class="add-box-icon-wrap">
            <uni-icons type="camera" size="26" color="#909399"></uni-icons>
            <view class="add-box-plus">
              <uni-icons type="plusempty" size="10" color="#909399"></uni-icons>
            </view>
          </view>
          <text class="add-box-text">{{ addText }}</text>
        </view>
      </slot>
    </uni-file-picker>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'

  // 组件属性
  const props = withDefaults(
    defineProps<{
      // 文件列表
      modelValue: any
      // 标签（showLabelRow 为 false 时可不传）
      label?: string
      // 提示文案
      tip?: string
      // 是否展示顶部标签行（表单内已有总标题时可关）
      showLabelRow?: boolean
      // 文件类型
      fileMediatype?: string
      // 限制数量
      limit?: number
      // 最大文件大小
      maxFileSize?: number
      // 列表样式
      listStyles?: any
      // 图片样式
      imageStyles?: any
      // 删除图标
      delIcon?: boolean
    }>(),
    {
      label: '',
      tip: '',
      showLabelRow: true,
      fileMediatype: 'image',
      limit: 1,
      delIcon: true,
      maxFileSize: 10 * 1024 * 1024,
      listStyles: {
        dividline: true
      },
      imageStyles: {
        width: 84,
        height: 84,
        border: { width: 0, style: 'none', radius: '8rpx' }
      }
    }
  )

  // 组件事件
  const emit = defineEmits<{
    (e: 'update:modelValue', value: any): void
    (e: 'select', payload: any): void
    (e: 'delete', payload: any): void
  }>()

  // 文件列表
  const innerValue = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  /**
   * 提示文案
   * @returns 提示文案
   */
  const addText = computed(() => {
    switch (props.fileMediatype) {
      case 'video':
        return '视频'
      case 'all':
        return '图片/视频'
      default:
        return '图片'
    }
  })
</script>

<style lang="scss">
  // 故障视频/图片上传数量备注
  .shipping-label {
    @include flex-row;
    gap: $space-xs;
    margin-bottom: $space-lg;
    font-size: $font-md;
    font-weight: bold;

    .upload-tip {
      display: block;
      font-size: $font-sm;
      color: $text-placeholder;
    }
  }

  // 文件选择器「添加图片」样式（虚线框 + 相机加号 + 文案）--------------------------------------------------
  .file-picker-add-box {
    @include flex-column-center;
    gap: $space-xs;
    width: 140rpx;
    height: 140rpx;
    box-sizing: border-box;
    background-color: $surface-slate-50;
    border: 4rpx dashed rgba(0, 0, 0, 0.15);
    border-radius: $radius-md;

    .add-box-icon-wrap {
      position: relative;

      .add-box-plus {
        position: absolute;
        top: -6rpx;
        right: -6rpx;
        width: 32rpx;
        height: 32rpx;
        @include flex-center;
        background: $surface-slate-50;
      }
    }

    .add-box-text {
      font-size: $font-sm;
      color: $text-placeholder;
    }
  }
</style>
