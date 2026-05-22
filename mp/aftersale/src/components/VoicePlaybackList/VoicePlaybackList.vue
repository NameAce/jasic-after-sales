<template>
  <!-- 售后客户端小程序（报修、工单、地址）组件 VoicePlaybackList -->
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
  import { themeColor } from '@/constants/theme'

  /**
 * 播放条目：duration 为毫秒（可选，元数据未就绪时的显示回退）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  export interface VoicePlaybackItem {
    url: string
    duration?: number
  }

  const props = withDefaults(
    defineProps<{
      items: VoicePlaybackItem[]
      /**
 * 是否展示删除按钮，点击后通过 `remove` 事件将 index 交给父级确认/提交
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
      deletable?: boolean
      /**
       * 远程 http(s) 语音是否先 downloadFile 到本地再播，本地/平台路径自动直通
       * （部分端 InnerAudioContext 直播远程 URL 的兼容性差，默认开启以对齐 VoiceInputField 的本地 tempFilePath 行为）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 当前播放已播秒数（onTimeUpdate 写入，供模板响应式刷新）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const playHeadSec = ref(0)
  /**
   * 切段播放前会 stop()，各端 onStop 可能晚于新 play()，导致误清 UI；
   * 对下一次 onStop 打标忽略。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const ignoreNextAudioStop = ref(false)
  let ignoreNextAudioStopTimer: ReturnType<typeof setTimeout> | null = null
  /**
 * 上一次成功发起播放的地址，用于同段重播时 seek(0)，避免清空 src 触发各端 onError
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const lastVoicePlayPath = ref('')

  /**
 * 原始 url → 可播放的本地 tempFilePath（或原样 url）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const resolvedPlaySrcMap = ref<Record<string, string>>({})

  /**
   * 解码得到的时长（秒），按「业务 url」索引。
   * 详情接口语音附件常无 duration 元数据，未播放时须展示该段总时长。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const resolvedDurationSecByUrl = ref<Record<string, number>>({})

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

  const metaDurationSec = (item: VoicePlaybackItem) =>
    item.duration && item.duration > 0 ? Math.max(0, Math.round(item.duration / 1000)) : 0

  const rememberDecodedSec = (logicalUrl: string, durationSec: number) => {
    const u = String(logicalUrl ?? '').trim()
    if (!u || !durationSec || durationSec <= 0 || Number.isNaN(durationSec)) return
    const sec = Math.max(0, Math.round(durationSec))
    if (resolvedDurationSecByUrl.value[u] === sec) return
    resolvedDurationSecByUrl.value = { ...resolvedDurationSecByUrl.value, [u]: sec }
  }

  /**
   * 列表时长：未播放为总时长；播放中为已播秒数（随 currentTime 递增）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const formatDisplayDuration = (item: VoicePlaybackItem, index: number) => {
    if (playingIndex.value === index) {
      return String(playHeadSec.value)
    }
    const meta = metaDurationSec(item)
    if (meta > 0) return String(meta)
    const cached = resolvedDurationSecByUrl.value[String(item.url ?? '').trim()]
    if (cached && cached > 0) return String(cached)
    return '0'
  }

  /**
 * 与 innerAudioContext.currentTime 同一套时间轴；元数据仅作回退
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const getPlayTotalSec = (index: number): number => {
    const ctx = innerAudioContext?.duration
    if (typeof ctx === 'number' && ctx > 0 && !Number.isNaN(ctx)) {
      return ctx
    }
    const item = props.items[index]
    const recorded = metaDurationSec(item)
    if (recorded > 0) return recorded
    const cached = item?.url ? resolvedDurationSecByUrl.value[String(item.url).trim()] : 0
    return cached && cached > 0 ? cached : 0
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
    playHeadSec.value = 0
    lastVoicePlayPath.value = ''
  }

  if (innerAudioContext) {
    innerAudioContext.onTimeUpdate(() => {
      const idx = playingIndex.value
      if (idx < 0) return
      const cur = innerAudioContext.currentTime || 0
      playHeadSec.value = Math.max(0, Math.round(cur))
      const total = getPlayTotalSec(idx)
      if (total <= 0) return
      if (!cur || cur <= 0) return
      playProgress.value = Math.min(100, Math.max(0, (cur / total) * 100))
    })
    innerAudioContext.onEnded(() => {
      playingIndex.value = -1
      playProgress.value = 0
      playHeadSec.value = 0
    })
    innerAudioContext.onStop(() => {
      if (ignoreNextAudioStop.value) {
        clearIgnoreAudioStop()
        return
      }
      playingIndex.value = -1
      playProgress.value = 0
      playHeadSec.value = 0
    })
    innerAudioContext.onError(() => {
      playingIndex.value = -1
      playProgress.value = 0
      playHeadSec.value = 0
    })
    innerAudioContext.onCanplay(() => {
      const idx = playingIndex.value
      if (idx < 0) return
      const logical = String(props.items[idx]?.url ?? '').trim()
      if (!logical) return
      const d = innerAudioContext.duration
      if (typeof d === 'number' && d > 0 && !Number.isNaN(d)) {
        rememberDecodedSec(logical, d)
      }
    })
  }

  /**
 * 无 duration 元数据时静默解码，使未播放态也显示该段总秒数
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  let voiceDurationProbeSeq = 0
  async function probeDecodedDurationSec(playableSrc: string, logicalUrl: string): Promise<void> {
    const create = uni.createInnerAudioContext
    if (typeof create !== 'function' || !playableSrc) return
    const ctx = create() as ReturnType<typeof uni.createInnerAudioContext> & {
      obeyMuteSwitch?: boolean
    }
    try {
      ctx.volume = 0
      if (typeof ctx.obeyMuteSwitch === 'boolean') {
        ctx.obeyMuteSwitch = false
      }
    } catch {
      /* noop */
    }
    await new Promise<void>((resolve) => {
      let finished = false
      const done = () => {
        if (finished) return
        finished = true
        try {
          ctx.stop()
        } catch {
          /* noop */
        }
        try {
          ctx.destroy()
        } catch {
          /* noop */
        }
        resolve()
      }
      const tryCapture = (): boolean => {
        if (finished) return true
        try {
          const d = ctx.duration
          if (typeof d === 'number' && d > 0 && !Number.isNaN(d)) {
            rememberDecodedSec(logicalUrl, d)
            return true
          }
        } catch {
          /* noop */
        }
        return false
      }
      const finishOk = () => {
        if (finished) return
        tryCapture()
        clearTimeout(timer)
        done()
      }
      const timer = setTimeout(finishOk, 12000)

      let playKicks = 0
      const kickPlay = () => {
        if (finished || playKicks >= 3) return
        playKicks += 1
        try {
          ctx.play()
        } catch {
          /* noop */
        }
      }

      const onProgress = () => {
        if (tryCapture()) {
          clearTimeout(timer)
          done()
        }
      }

      ctx.onCanplay(() => {
        onProgress()
        kickPlay()
      })
      ctx.onPlay(onProgress)
      ctx.onTimeUpdate(onProgress)
      ctx.onError(() => finishOk())

      try {
        ctx.src = playableSrc
      } catch {
        finishOk()
        return
      }

      void nextTick(() => {
        if (!finished && !tryCapture()) kickPlay()
      })
      setTimeout(() => {
        if (!finished && !tryCapture()) kickPlay()
      }, 400)
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
    playHeadSec.value = 0
    const resolved = await resolvePlayableSrc(item.url)
    if (!resolved) {
      playingIndex.value = -1
      playProgress.value = 0
      playHeadSec.value = 0
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
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  const onRemoveClick = (index: number) => {
    if (!props.deletable) return
    if (index < 0 || index >= props.items.length) return
    emit('remove', index)
  }

  /**
   * items 长度缩短即视为有条目被移除，统一停止播放；避免因 index 偏移导致
   * 正在播放样式错位或继续驱动已不存在的条目。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
  let prevItemsLen = props.items.length
  watch(
    () => props.items.length,
    (len) => {
      if (len < prevItemsLen) stopAndResetPlayback()
      prevItemsLen = len
    }
  )

  watch(
    () => props.items.map((i) => `${String(i.url ?? '').trim()}:${i.duration ?? 0}`).join('|'),
    () => {
      const seq = ++voiceDurationProbeSeq
      void (async () => {
        for (const item of props.items) {
          if (seq !== voiceDurationProbeSeq) return
          const logical = String(item.url ?? '').trim()
          if (!logical) continue
          if (metaDurationSec(item) > 0) continue
          if (resolvedDurationSecByUrl.value[logical]) continue
          const playable = await resolvePlayableSrc(logical)
          if (seq !== voiceDurationProbeSeq || !playable) return
          await probeDecodedDurationSec(playable, logical)
        }
      })()
    },
    { flush: 'post', immediate: true }
  )

  onUnmounted(() => {
    voiceDurationProbeSeq += 1
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
