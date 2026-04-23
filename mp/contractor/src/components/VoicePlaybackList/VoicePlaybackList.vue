<template>
  <view v-if="items.length" class="voice-input-wrapper">
    <view class="voice-list">
      <view
        v-for="(item, index) in items"
        :key="(item.url || '') + '-' + index"
        class="voice-item"
        :class="{ 'voice-item--playing': playingIndex === index }"
      >
        <view class="play-btn" @click="playVoice(item, index)">
          <uni-icons
            :type="playingIndex === index ? 'circle-filled' : 'circle'"
            size="20"
            :color="themeColor.primary"
          />
        </view>
        <view class="progress-bar">
          <view
            class="progress-inner"
            :style="{ width: (playingIndex === index ? playProgress : 0) + '%' }"
          />
        </view>
        <text class="duration">{{ formatDisplayDuration(item, index) }}″</text>
        <view v-if="deletable" class="delete-btn" @click.stop="onRemoveClick(index)">
          <uni-icons type="closeempty" size="14" color="#999" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, watch, onUnmounted, nextTick } from 'vue'
  import { themeColor } from '@/theme/colors'

  /** 播放条目：duration 为毫秒（可选，元数据未就绪时的显示回退） */
  export interface VoicePlaybackItem {
    url: string
    duration?: number
  }

  const props = withDefaults(
    defineProps<{
      items: VoicePlaybackItem[]
      /** 是否展示删除按钮，点击后通过 `remove` 事件将 index 交给父级确认/提交 */
      deletable?: boolean
      /**
       * 远程 http(s) 语音是否先 downloadFile 到本地再播，本地/平台路径自动直通
       * （部分端 InnerAudioContext 直播远程 URL 的兼容性差，默认开启以对齐 VoiceInputField 的本地 tempFilePath 行为）
       */
      downloadable?: boolean
    }>(),
    {
      items: () => [],
      deletable: false,
      downloadable: true
    }
  )

  const emit = defineEmits<{
    (e: 'remove', index: number): void
  }>()

  const playingIndex = ref(-1)
  const playProgress = ref(0)
  /**
   * 切段播放前会 stop()，各端 onStop 可能晚于新 play()，导致误清 UI；
   * 对下一次 onStop 打标忽略。
   */
  const ignoreNextAudioStop = ref(false)
  let ignoreNextAudioStopTimer: ReturnType<typeof setTimeout> | null = null
  /** 上一次成功发起播放的地址，用于同段重播时 seek(0)，避免清空 src 触发各端 onError */
  const lastVoicePlayPath = ref('')

  /** 原始 url → 可播放的本地 tempFilePath（或原样 url） */
  const resolvedPlaySrcMap = ref<Record<string, string>>({})

  const isRemoteHttpUrl = (url: string) => /^https?:\/\//i.test(url.trim())

  const resolvePlayableSrc = async (rawUrl: string): Promise<string | null> => {
    const url = String(rawUrl ?? '').trim()
    if (!url) return null
    const cached = resolvedPlaySrcMap.value[url]
    if (cached) return cached

    if (!props.downloadable || !isRemoteHttpUrl(url)) {
      resolvedPlaySrcMap.value = { ...resolvedPlaySrcMap.value, [url]: url }
      return url
    }

    const download = (
      uni as unknown as {
        downloadFile?: (o: {
          url: string
          success?: (res: { statusCode: number; tempFilePath: string }) => void
          fail?: (err: unknown) => void
        }) => void
      }
    ).downloadFile

    if (typeof download !== 'function') {
      resolvedPlaySrcMap.value = { ...resolvedPlaySrcMap.value, [url]: url }
      return url
    }

    try {
      const local = await new Promise<string>((resolve, reject) => {
        download({
          url,
          success: (res) => {
            if (res?.statusCode === 200 && res.tempFilePath) resolve(res.tempFilePath)
            else reject(new Error(`downloadFile statusCode=${res?.statusCode}`))
          },
          fail: (err) => reject(err)
        })
      })
      resolvedPlaySrcMap.value = { ...resolvedPlaySrcMap.value, [url]: local }
      return local
    } catch (e) {
      console.error('download voice failed', e)
      uni.showToast({ title: '语音下载失败', icon: 'none', duration: 1500 })
      return null
    }
  }

  const innerAudioContext =
    uni.createInnerAudioContext && typeof uni.createInnerAudioContext === 'function'
      ? uni.createInnerAudioContext()
      : null

  const clearIgnoreAudioStop = () => {
    if (ignoreNextAudioStopTimer) {
      clearTimeout(ignoreNextAudioStopTimer)
      ignoreNextAudioStopTimer = null
    }
    ignoreNextAudioStop.value = false
  }

  const formatDurationSec = (ms: number) => {
    if (!ms || ms <= 0) return '0'
    return String(Math.max(0, Math.round(ms / 1000)))
  }

  /**
   * 列表时长：正在播放时优先用解码时长（与 currentTime/进度条同源），否则用元数据
   */
  const formatDisplayDuration = (item: VoicePlaybackItem, index: number) => {
    if (innerAudioContext && playingIndex.value === index) {
      const d = innerAudioContext.duration
      if (typeof d === 'number' && d > 0 && !Number.isNaN(d)) {
        return String(Math.max(0, Math.round(d)))
      }
    }
    return formatDurationSec(item.duration ?? 0)
  }

  /** 与 innerAudioContext.currentTime 同一套时间轴；元数据仅作回退 */
  const getPlayTotalSec = (index: number): number => {
    const ctx = innerAudioContext?.duration
    if (typeof ctx === 'number' && ctx > 0 && !Number.isNaN(ctx)) {
      return ctx
    }
    const item = props.items[index]
    const recorded = item?.duration && item.duration > 0 ? item.duration / 1000 : 0
    return recorded > 0 ? recorded : 0
  }

  const stopAndResetPlayback = () => {
    clearIgnoreAudioStop()
    try {
      innerAudioContext?.stop()
    } catch {
      /* noop */
    }
    playingIndex.value = -1
    playProgress.value = 0
    lastVoicePlayPath.value = ''
  }

  if (innerAudioContext) {
    innerAudioContext.onTimeUpdate(() => {
      const idx = playingIndex.value
      if (idx < 0) return
      const total = getPlayTotalSec(idx)
      if (total <= 0) return
      const cur = innerAudioContext.currentTime || 0
      if (!cur || cur <= 0) return
      playProgress.value = Math.min(100, Math.max(0, (cur / total) * 100))
    })
    innerAudioContext.onEnded(() => {
      playingIndex.value = -1
      playProgress.value = 0
    })
    innerAudioContext.onStop(() => {
      if (ignoreNextAudioStop.value) {
        clearIgnoreAudioStop()
        return
      }
      playingIndex.value = -1
      playProgress.value = 0
    })
    innerAudioContext.onError(() => {
      playingIndex.value = -1
      playProgress.value = 0
    })
  }

  const scheduleClearIgnoreStop = () => {
    if (ignoreNextAudioStopTimer) {
      clearTimeout(ignoreNextAudioStopTimer)
      ignoreNextAudioStopTimer = null
    }
    ignoreNextAudioStopTimer = setTimeout(() => {
      ignoreNextAudioStopTimer = null
      if (ignoreNextAudioStop.value) ignoreNextAudioStop.value = false
    }, 320)
  }

  const playVoice = async (item: VoicePlaybackItem, index: number) => {
    if (!item.url) return
    if (!innerAudioContext) {
      uni.showToast({ title: '当前环境不支持播放', icon: 'none', duration: 1500 })
      return
    }
    if (playingIndex.value === index) {
      clearIgnoreAudioStop()
      innerAudioContext.stop()
      return
    }
    ignoreNextAudioStop.value = true
    scheduleClearIgnoreStop()
    try {
      innerAudioContext.stop()
    } catch {
      /* noop */
    }
    await nextTick()
    playingIndex.value = index
    playProgress.value = 0
    const resolved = await resolvePlayableSrc(item.url)
    if (!resolved) {
      playingIndex.value = -1
      playProgress.value = 0
      return
    }
    const path = resolved
    const ac = innerAudioContext as {
      src: string
      play: () => void
      seek?: (position: number) => void
    }
    const replaySame = lastVoicePlayPath.value === path && !!path
    if (replaySame && typeof ac.seek === 'function') {
      try {
        ac.seek(0)
      } catch {
        ac.src = path
      }
      lastVoicePlayPath.value = path
      ac.play()
    } else {
      lastVoicePlayPath.value = path
      ac.src = path
      ac.play()
    }
  }

  /**
   * 只抛事件，具体的确认弹窗与列表变更交给父级（VoiceInputField 等）。
   * 本地仅做播放态兜底：父级提交删除后，items 长度变化会触发 watcher 统一停止播放。
   */
  const onRemoveClick = (index: number) => {
    if (!props.deletable) return
    if (index < 0 || index >= props.items.length) return
    emit('remove', index)
  }

  /**
   * items 长度缩短即视为有条目被移除，统一停止播放；避免因 index 偏移导致
   * 正在播放样式错位或继续驱动已不存在的条目。
   */
  let prevItemsLen = props.items.length
  watch(
    () => props.items.length,
    (len) => {
      if (len < prevItemsLen) stopAndResetPlayback()
      prevItemsLen = len
    }
  )

  onUnmounted(() => {
    lastVoicePlayPath.value = ''
    clearIgnoreAudioStop()
    innerAudioContext?.destroy()
  })
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  /* 与 VoiceInputField 录音条下方的列表同一套视觉 */
  .voice-input-wrapper {
    width: 100%;

    .voice-list {
      @include flex-column;
      gap: $space-sm;

      .voice-item {
        @include flex-row;
        align-items: center;
        padding: $space-sm $space-md;
        background-color: $bg-input;
        border-radius: $radius-xl;
        gap: $space-md;

        .play-btn {
          width: 48rpx;
          height: 48rpx;
          @include flex-center;
        }

        .progress-bar {
          flex: 1;
          height: 8rpx;
          background-color: $surface-track;
          border-radius: 4rpx;
          overflow: hidden;

          .progress-inner {
            height: 100%;
            background-color: $primary;
            width: 0;
            transition: width 0.1s linear;
          }
        }

        .duration {
          font-size: $font-sm;
          color: $text-secondary;
          min-width: 40rpx;
          text-align: right;
        }

        .delete-btn {
          padding: $space-xs;
          @include flex-center;
        }

        &.voice-item--playing {
          background-color: rgba($primary, 0.08);
          box-shadow: inset 0 0 0 2rpx rgba($primary, 0.35);
        }
      }
    }
  }
</style>
