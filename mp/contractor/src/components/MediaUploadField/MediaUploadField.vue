<template>
  <view>
    <view v-if="showLabelRow" class="shipping-label">
      <text>{{ label }}</text>
      <text class="upload-tip">{{ tip }}</text>
    </view>

    <uni-file-picker
      v-model="innerValue"
      :mode="mode"
      :file-mediatype="fileMediatype"
      :limit="limit"
      :max-file-size="maxFileSize"
      :list-styles="listStyles"
      :image-styles="imageStyles"
      :del-icon="delIcon"
      @select="handleSelect"
      @delete="handleDelete"
    >
      <view class="file-picker-add-box">
        <view class="add-box-icon-wrap">
          <uni-icons type="camera" size="26" :color="themeColor.info"></uni-icons>
          <view class="add-box-plus">
            <uni-icons type="plusempty" size="10" :color="themeColor.info"></uni-icons>
          </view>
        </view>
        <text class="add-box-text">{{ addText }}</text>
      </view>
    </uni-file-picker>
  </view>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { themeColor } from '@/theme/colors'
  import { uploadSystemFile } from '@/api/file'

  const props = withDefaults(
    defineProps<{
      modelValue: any[]
      label?: string
      tip?: string
      showLabelRow?: boolean
      fileMediatype?: string
      mode?: 'grid' | 'list'
      limit?: number
      maxFileSize?: number
      listStyles?: any
      imageStyles?: any
      delIcon?: boolean
    }>(),
    {
      label: '',
      tip: '',
      showLabelRow: true,
      fileMediatype: 'image',
      mode: 'grid',
      limit: 1,
      delIcon: true,
      maxFileSize: 10 * 1024 * 1024,
      listStyles: () => ({ dividline: true }),
      imageStyles: () => ({
        width: 84,
        height: 84,
        border: { width: 0, style: 'none', radius: '8rpx' }
      })
    }
  )

  const emit = defineEmits<{
    (e: 'update:modelValue', value: any[]): void
    (e: 'select', payload: any): void
    (e: 'delete', payload: any): void
  }>()

  const innerValue = computed({
    get: () => props.modelValue || [],
    set: (val) => emit('update:modelValue', val || [])
  })

  const addText = computed(() => {
    if (props.fileMediatype === 'video') return '视频'
    if (props.fileMediatype === 'all') return '图片/视频'
    return '图片'
  })

  const normalizePreviewUrl = (url: unknown): string => {
    const raw = String(url ?? '').trim()
    if (!raw) return ''
    if (/^(data:|blob:|file:|wxfile:|http:\/\/tmp\/|https:\/\/tmp\/)/i.test(raw)) return raw
    if (/^\/?(tmp|storage|var|private|android|sdcard)\//i.test(raw)) return raw
    if (/^https?:\/\//i.test(raw)) return raw
    const base = String(import.meta.env.VITE_HTTP || '')
      .trim()
      .replace(/\/$/, '')
    if (!base) return raw
    return `${base}${raw.startsWith('/') ? raw : `/${raw}`}`
  }

  const pickLocalPath = (item: Record<string, unknown>): string => {
    const path = item.path ?? item.filePath ?? item.tempFilePath ?? item.previewUrl ?? item.url
    return String(path ?? '').trim()
  }

  const handleSelect = async (e: any) => {
    emit('select', e)
    const files = Array.isArray(e?.tempFiles) ? e.tempFiles : []
    if (!files.length) return

    const nextList = [...(Array.isArray(props.modelValue) ? props.modelValue : [])] as Record<
      string,
      unknown
    >[]

    uni.showLoading({ title: '上传中...' })
    try {
      for (const file of files) {
        const localPath = pickLocalPath(file)
        if (!localPath) continue
        const uploaded = await uploadSystemFile(localPath)
        const previewUrl = normalizePreviewUrl(uploaded.previewUrl)
        const normalized: Record<string, unknown> = {
          ...file,
          fileId: uploaded.fileId,
          id: uploaded.fileId,
          url: previewUrl || localPath,
          path: previewUrl || localPath,
          tempFilePath: localPath,
          name: uploaded.originalName || String(file.name ?? ''),
          extname: uploaded.fileExt || String(file.extname ?? '')
        }
        const idx = nextList.findIndex((x) => pickLocalPath(x) === localPath)
        if (idx >= 0) nextList[idx] = { ...nextList[idx], ...normalized }
        else nextList.push(normalized)
      }
      emit('update:modelValue', nextList)
    } catch (err: any) {
      uni.showToast({ title: err?.message || '上传失败', icon: 'none', duration: 1500 })
    } finally {
      uni.hideLoading()
    }
  }

  const handleDelete = (e: any) => {
    emit('delete', e)
  }
</script>

<style lang="scss">
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

  .file-picker-add-box {
    @include flex-column-center;
    gap: $space-xs;
    width: 140rpx;
    height: 140rpx;
    box-sizing: border-box;
    background-color: $bg-input;
    border: 4rpx dashed rgba(0, 0, 0, 0.15);
    border-radius: $radius-md;

    .add-box-icon-wrap {
      position: relative;
    }

    .add-box-plus {
      position: absolute;
      top: -6rpx;
      right: -6rpx;
      width: 32rpx;
      height: 32rpx;
      @include flex-center;
      background: $bg-input;
    }

    .add-box-text {
      font-size: $font-sm;
      color: $text-placeholder;
    }
  }
</style>
