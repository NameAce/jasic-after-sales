<template>
  <view>
    <view v-if="showLabelRow" class="shipping-label">
      <text>{{ label }}</text>
      <text v-if="tip" class="upload-tip">{{ tip }}</text>
    </view>

    <!-- 图片+视频混合：uni-file-picker 在 all 下强制 list，视频无法宫格缩略；改用 chooseMedia + 与图片一致的宫格回显 -->
    <view v-if="useMixedMediaGrid" class="mixed-media-wrap">
      <view class="mixed-media-grid">
        <view
          v-for="(item, index) in mixedFileList"
          :key="mixedItemKey(item, index)"
          class="mixed-tile"
        >
          <view class="mixed-tile-inner" @click="previewMixedItem(item)">
            <template v-if="tileIsVideo(item)">
              <image
                v-if="tileVideoPoster(item)"
                class="mixed-thumb"
                mode="aspectFill"
                :src="tileVideoPoster(item)"
              />
              <video
                v-else
                class="mixed-thumb mixed-thumb-video"
                :src="tileVideoPlaySrc(item)"
                :poster="tileVideoPoster(item)"
                object-fit="cover"
                :muted="true"
                :controls="false"
                :show-center-play-btn="false"
                :enable-progress-gesture="false"
              />
              <view class="mixed-video-badge">
                <uni-icons type="videocam" size="20" color="#fff"></uni-icons>
              </view>
            </template>
            <image v-else class="mixed-thumb" mode="aspectFill" :src="tileImageSrc(item)" />
          </view>
          <view v-if="delIcon" class="mixed-del" @click.stop="removeMixedAt(index)"></view>
        </view>
        <view
          v-if="mixedFileList.length < limitNum"
          class="file-picker-add-box"
          @click="chooseMixedMedia"
        >
          <view class="add-box-icon-wrap">
            <uni-icons type="camera" size="26" color="#909399"></uni-icons>
            <view class="add-box-plus">
              <uni-icons type="plusempty" size="10" color="#909399"></uni-icons>
            </view>
          </view>
          <text class="add-box-text">{{ addText }}</text>
        </view>
      </view>
    </view>

    <!-- 仅图片 / 仅视频 / 不支持 chooseMedia 时的 all（如部分 H5） -->
    <uni-file-picker
      v-else
      v-model="innerValue"
      class="single-media-picker"
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
  import { uploadSystemFile } from '@/api/file'
  import { beginMediaUpload, endMediaUpload, toastIfMediaUploading } from '@/utils/mediaUploadLock'
  import { previewImages, previewVideo, resolvePreviewableUrl } from '@/utils/mediaPreview'
  import { validateFaultMediaSelection } from '@/utils/repairMediaLimits'
  import { isVideoMediaItem } from '@/utils/workOrderFileIds'

  /** 与 uni-file-picker 的 `file-mediatype`（_UniFilePickerFileMediatype）一致 */
  type FileMediatypeProp = 'image' | 'video' | 'all'

  const props = withDefaults(
    defineProps<{
      modelValue: unknown
      label?: string
      tip?: string
      showLabelRow?: boolean
      fileMediatype?: FileMediatypeProp
      mode?: 'grid' | 'list'
      limit?: number
      maxFileSize?: number
      listStyles?: Record<string, unknown>
      imageStyles?: Record<string, unknown>
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
    (e: 'update:modelValue', value: unknown): void
    (e: 'select', payload: unknown): void
    (e: 'delete', payload: unknown): void
  }>()

  const innerValue = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  const limitNum = computed(() => Number(props.limit ?? 1) || 1)

  const useMixedMediaGrid = computed(
    () => props.fileMediatype === 'all' && typeof uni.chooseMedia === 'function'
  )

  const mixedFileList = computed(() =>
    Array.isArray(props.modelValue) ? (props.modelValue as Record<string, unknown>[]) : []
  )

  const mixedItemKey = (item: Record<string, unknown>, index: number) => {
    const id = item.fileId ?? item.id
    const p = pickLocalPath(item)
    return `${typeof id === 'number' ? id : ''}-${p}-${index}`
  }

  const tileIsVideo = (item: Record<string, unknown>) => isVideoMediaItem(item)

  const tileVideoPoster = (item: Record<string, unknown>) => {
    return String(item.videoPoster ?? '').trim()
  }

  const tileImageSrc = (item: Record<string, unknown>) => {
    const raw =
      item.url ?? item.previewUrl ?? item.fileUrl ?? item.path ?? item.filePath ?? item.tempFilePath
    const s = resolvePreviewableUrl(raw)
    return s || pickLocalPath(item)
  }

  const tileVideoPlaySrc = (item: Record<string, unknown>) => {
    const raw =
      item.url ?? item.previewUrl ?? item.fileUrl ?? item.path ?? item.filePath ?? item.tempFilePath
    return resolvePreviewableUrl(raw) || pickLocalPath(item)
  }

  const uploadAndMergeFiles = async (pending: Record<string, unknown>[]): Promise<void> => {
    if (!pending.length) return
    const nextList = [...(Array.isArray(props.modelValue) ? props.modelValue : [])] as Record<
      string,
      unknown
    >[]

    beginMediaUpload()
    uni.showLoading({ title: '当前正在上传数据，请稍等', mask: true })
    try {
      for (const file of pending) {
        const localPath = pickLocalPath(file)
        if (!localPath) continue
        const uploaded = await uploadSystemFile(localPath)
        const previewUrl = resolvePreviewableUrl(uploaded.previewUrl)
        const fileId = uploaded.fileId != null ? Number(uploaded.fileId) : undefined
        const normalized: Record<string, unknown> = {
          ...file,
          fileId,
          id: fileId,
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
    } catch (err: unknown) {
      const msg =
        (err as { message?: string })?.message || (err as { msg?: string })?.msg || '上传失败'
      uni.showToast({ title: msg, icon: 'none', duration: 1500 })
    } finally {
      uni.hideLoading()
      endMediaUpload()
    }
  }

  const chooseMixedMedia = (): void => {
    if (toastIfMediaUploading()) return
    const current = mixedFileList.value
    const remain = limitNum.value - current.length
    if (remain <= 0) {
      uni.showToast({ title: `最多选择 ${limitNum.value} 个文件`, icon: 'none', duration: 1500 })
      return
    }
    uni.chooseMedia({
      count: remain,
      mediaType: ['image', 'video'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        void handleMixedChooseSuccess(res.tempFiles ?? [])
      },
      fail: (err) => {
        const msg = (err as { errMsg?: string })?.errMsg || ''
        if (!/cancel/i.test(msg)) {
          uni.showToast({ title: '选择文件失败', icon: 'none', duration: 1500 })
        }
      }
    })
  }

  const extFromPath = (p: string): string => {
    const m = p.match(/\.([^.\\/]+)$/)
    return (m?.[1] ?? '').toLowerCase()
  }

  type ChooseMediaTempFile = {
    tempFilePath?: string
    fileType?: string
    size?: number
    name?: string
    thumbTempFilePath?: string
  }

  const handleMixedChooseSuccess = async (tempFiles: ChooseMediaTempFile[]): Promise<void> => {
    const base = mixedFileList.value
    const mapped: Record<string, unknown>[] = []
    for (const f of tempFiles) {
      const path = String(f.tempFilePath ?? '').trim()
      if (!path) continue
      const maxSz = props.maxFileSize ?? 10 * 1024 * 1024
      if (typeof f.size === 'number' && f.size > maxSz) {
        uni.showToast({ title: '文件过大', icon: 'none', duration: 1500 })
        continue
      }
      const ft = f.fileType === 'video' ? 'video' : 'image'
      const thumb = ft === 'video' ? String(f.thumbTempFilePath ?? '').trim() : ''
      mapped.push({
        path,
        tempFilePath: path,
        fileType: ft,
        size: f.size,
        name: String(f.name ?? '').trim() || path.substring(path.lastIndexOf('/') + 1),
        extname: extFromPath(path),
        videoPoster: thumb
      })
    }
    if (!mapped.length) return

    const merged = [...base, ...mapped]
    const forValidate = merged.map((x) => ({
      fileType: isVideoMediaItem(x) ? ('video' as const) : ('image' as const)
    }))
    emit('select', { tempFiles: forValidate })
    if (!validateFaultMediaSelection(forValidate)) {
      return
    }

    await uploadAndMergeFiles(mapped)
  }

  const removeMixedAt = (index: number): void => {
    if (toastIfMediaUploading()) return
    const list = [...mixedFileList.value]
    const removed = list.splice(index, 1)[0]
    emit('update:modelValue', list)
    emit('delete', {
      index,
      tempFile: removed,
      tempFiles: list.map((x) => ({
        fileType: isVideoMediaItem(x) ? 'video' : 'image'
      }))
    })
  }

  const previewMixedItem = (item: Record<string, unknown>): void => {
    if (toastIfMediaUploading()) return
    if (tileIsVideo(item)) {
      const src = tileVideoPlaySrc(item)
      if (!src) return
      previewVideo(src)
      return
    }
    const urls = mixedFileList.value
      .filter((x) => !tileIsVideo(x))
      .map((x) => tileImageSrc(x))
      .filter(Boolean)
    const cur = tileImageSrc(item)
    if (!urls.length || !cur) return
    previewImages(urls, cur)
  }

  const addText = computed((): string => {
    switch (props.fileMediatype) {
      case 'video':
        return '视频'
      case 'all':
        return '图片/视频'
      default:
        return '图片'
    }
  })

  const pickLocalPath = (item: Record<string, unknown>): string => {
    const path = item.path ?? item.filePath ?? item.tempFilePath ?? item.previewUrl ?? item.url
    return String(path ?? '').trim()
  }

  const handleSelect = async (e: unknown): Promise<void> => {
    if (toastIfMediaUploading()) return
    const payload = e as { tempFiles?: Record<string, unknown>[] }
    emit('select', payload)
    const files = Array.isArray(payload?.tempFiles) ? payload.tempFiles : []
    if (!files.length) return

    const pending = files.filter((f) => {
      const hasId = typeof f.fileId === 'number' || typeof f.id === 'number'
      return !hasId && !!pickLocalPath(f)
    })
    if (!pending.length) return

    await uploadAndMergeFiles(pending)
  }

  const handleDelete = (e: unknown): void => {
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

      .add-box-plus {
        position: absolute;
        top: -6rpx;
        right: -6rpx;
        width: 32rpx;
        height: 32rpx;
        @include flex-center;
        background: $bg-input;
      }
    }

    .add-box-text {
      font-size: $font-sm;
      color: $text-placeholder;
    }
  }

  .mixed-media-wrap {
    width: 100%;
  }

  .mixed-media-grid {
    display: flex;
    flex-wrap: wrap;
    gap: $space-sm;
    align-items: flex-start;
  }

  .mixed-tile {
    position: relative;
    width: 140rpx;
    height: 140rpx;
    flex: none;
  }

  .mixed-tile-inner {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: $radius-md;
    overflow: hidden;
    background-color: $bg-input;
  }

  .mixed-thumb {
    width: 100%;
    height: 100%;
    vertical-align: top;
  }

  .mixed-thumb-video {
    display: block;
    pointer-events: none;
  }

  .mixed-video-badge {
    position: absolute;
    right: 8rpx;
    bottom: 8rpx;
    width: 44rpx;
    height: 44rpx;
    @include flex-center;
    background-color: rgba(0, 0, 0, 0.45);
    border-radius: 50%;
    pointer-events: none;
  }

  .mixed-del {
    position: absolute;
    top: 4rpx;
    right: 4rpx;
    z-index: 3;
    width: 50rpx;
    height: 50rpx;
    @include flex-center;
    background-color: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    &::before,
    &::after {
      content: '';
      position: absolute;
      width: 30rpx;
      height: 4rpx;
      background-color: #fff;
      border-radius: 2rpx;
    }
    &::before {
      transform: rotate(45deg);
    }
    &::after {
      transform: rotate(-45deg);
    }
  }

  .single-media-picker {
    :deep(.icon-del),
    :deep(.icon-clear),
    :deep(.file-picker__box-content__close),
    :deep(.uni-file-picker__close) {
      width: 40rpx !important;
      height: 40rpx !important;
    }

    :deep(.progress-box),
    :deep(.file-picker__progress),
    :deep(.uni-progress),
    :deep(progress) {
      display: none !important;
    }
  }
</style>
