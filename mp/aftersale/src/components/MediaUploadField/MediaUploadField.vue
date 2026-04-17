<template>
  <view>
    <!-- 标签 -->
    <view class="shipping-label">
      <text>{{ label }}</text>
      <!-- 提示文案 -->
      <text class="upload-tip">{{ tip }}</text>
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
                <uni-icons
                  type="videocam"
                  size="20"
                  :color="themeColor.primaryContrast"
                ></uni-icons>
              </view>
            </template>
            <image v-else class="mixed-thumb" mode="aspectFill" :src="tileImageSrc(item)" />
          </view>
          <view v-if="delIcon" class="mixed-del" @click.stop="removeMixedAt(index)"> </view>
        </view>
        <!-- 添加图片 -->
        <view
          v-if="mixedFileList.length < limitNum"
          class="file-picker-add-box"
          @click="chooseMixedMedia"
        >
          <view class="add-box-icon-wrap">
            <uni-icons type="camera" size="26" :color="themeColor.info"></uni-icons>
            <view class="add-box-plus">
              <uni-icons type="plusempty" size="10" :color="themeColor.info"></uni-icons>
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
  import { themeColor } from '@/constants/theme'
  import { uploadCustomerFile } from '@/api/file'
  import { validateFaultMediaSelection } from '@/utils/repairMediaLimits'
  import { isVideoMediaItem } from '@/utils/workOrderFileIds'

  const props = withDefaults(
    defineProps<{
      // 文件列表
      modelValue: any
      // 标签
      label: string
      // 提示文案
      tip: string
      // 文件类型
      fileMediatype?: string
      // 展示模式
      mode?: 'grid' | 'list'
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
      fileMediatype: 'image',
      mode: 'grid',
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

  // 定义事件
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

  // 限制数量
  const limitNum = computed(() => Number(props.limit ?? 1) || 1)

  /** chooseMedia 存在时用宫格混合选择（小程序/App 等）；否则退回 uni-file-picker */
  const useMixedMediaGrid = computed(
    () => props.fileMediatype === 'all' && typeof uni.chooseMedia === 'function'
  )

  // 混合文件列表
  const mixedFileList = computed(() =>
    Array.isArray(props.modelValue) ? (props.modelValue as Record<string, unknown>[]) : []
  )

  /**
   * 混合文件项唯一键
   * @param item 文件项
   * @param index 索引
   * @returns 唯一键
   */
  const mixedItemKey = (item: Record<string, unknown>, index: number) => {
    const id = item.fileId ?? item.id
    const p = pickLocalPath(item)
    return `${typeof id === 'number' ? id : ''}-${p}-${index}`
  }

  /**
   * 是否为视频文件项
   * @param item 文件项
   * @returns 是否为视频文件项
   */
  const tileIsVideo = (item: Record<string, unknown>) => isVideoMediaItem(item)

  /**
   * 视频文件项封面
   * @param item 文件项
   * @returns 封面
   */
  const tileVideoPoster = (item: Record<string, unknown>) => {
    const poster = String(item.videoPoster ?? '').trim()
    return poster
  }

  /**
   * 图片文件项源
   * @param item 文件项
   * @returns 源
   */
  const tileImageSrc = (item: Record<string, unknown>) => {
    const raw =
      item.url ?? item.previewUrl ?? item.fileUrl ?? item.path ?? item.filePath ?? item.tempFilePath
    const s = normalizePreviewUrl(raw)
    return s || pickLocalPath(item)
  }

  /**
   * 视频文件项播放源
   * @param item 文件项
   * @returns 播放源
   */
  const tileVideoPlaySrc = (item: Record<string, unknown>) => {
    const raw =
      item.url ?? item.previewUrl ?? item.fileUrl ?? item.path ?? item.filePath ?? item.tempFilePath
    return normalizePreviewUrl(raw) || pickLocalPath(item)
  }

  /**
   * 批量上传并合并进列表（与 uni-file-picker 选择回调共用逻辑）
   * @param pending 待上传文件列表
   * @returns void
   */
  const uploadAndMergeFiles = async (pending: Record<string, unknown>[]): Promise<void> => {
    if (!pending.length) return
    const nextList = [...(Array.isArray(props.modelValue) ? props.modelValue : [])] as Record<
      string,
      unknown
    >[]

    uni.showLoading({ title: '上传中...' })
    try {
      for (const file of pending) {
        const localPath = pickLocalPath(file)
        if (!localPath) continue
        const uploaded = await uploadCustomerFile(localPath)
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
    } catch (err: unknown) {
      const msg =
        (err as { message?: string })?.message || (err as { msg?: string })?.msg || '上传失败'
      uni.showToast({ title: msg, icon: 'none', duration: 1500 })
    } finally {
      uni.hideLoading()
    }
  }

  /**
   * 选择混合媒体
   * @returns void
   */
  const chooseMixedMedia = (): void => {
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

  /**
   * 从路径获取扩展名
   * @param p 路径
   * @returns 扩展名
   */
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

  /**
   * 混合媒体选择成功
   * @param tempFiles 临时文件列表
   * @returns void
   */
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

  /**
   * 删除混合媒体
   * @param index 索引
   * @returns void
   */
  const removeMixedAt = (index: number): void => {
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

  /**
   * 预览混合媒体
   * @param item 文件项
   * @returns void
   */
  const previewMixedItem = (item: Record<string, unknown>): void => {
    if (tileIsVideo(item)) {
      const src = tileVideoPlaySrc(item)
      if (!src) return
      if (typeof uni.previewMedia === 'function') {
        uni.previewMedia({
          sources: [{ url: src, type: 'video' }],
          fail: () => {
            uni.showToast({ title: '无法预览视频', icon: 'none', duration: 1500 })
          }
        })
      }
      return
    }
    const urls = mixedFileList.value
      .filter((x) => !tileIsVideo(x))
      .map((x) => tileImageSrc(x))
      .filter(Boolean)
    const cur = tileImageSrc(item)
    if (!urls.length || !cur) return
    uni.previewImage({
      urls,
      current: cur
    })
  }

  // 提示文案
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

  /**
   * 获取本地路径
   * @param item 文件项
   * @returns 本地路径
   */
  const pickLocalPath = (item: Record<string, unknown>): string => {
    const path = item.path ?? item.filePath ?? item.tempFilePath ?? item.previewUrl ?? item.url
    return String(path ?? '').trim()
  }

  /**
   * 规范化预览 URL
   * @param url URL
   * @returns 规范化后的 URL
   */
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

  /**
   * 选择文件
   * @param e 事件
   * @returns void
   */
  const handleSelect = async (e: unknown): Promise<void> => {
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

  /**
   * 删除文件
   * @param e 事件
   * @returns void
   */
  const handleDelete = (e: unknown): void => {
    emit('delete', e)
  }
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
      background-color: $primary-contrast;
      border-radius: 2rpx;
    }
    &::before {
      transform: rotate(45deg);
    }
    &::after {
      transform: rotate(-45deg);
    }
  }

  // 单独上传图片场景：统一右上角删除图标尺寸，并隐藏上传进度条
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
