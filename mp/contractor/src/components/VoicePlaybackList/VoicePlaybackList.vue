<template>
  <view v-if="items.length" class="voice-input-wrapper">
    <view class="voice-list">
      <view
        v-for="(item, index) in items"
        :key="item.url + '-' + index"
        class="voice-item"
        :class="{ 'voice-item--playing': playingIndex === index }"
      >
        <view class="play-btn" @click="playVoice(item, index)">
          <uni-icons
            :type="playingIndex === index ? 'circle-filled' : 'circle'"
            size="20"
            color="#f26604"
          />
        </view>
        <view class="progress-bar">
          <view
            class="progress-inner"
            :style="{ width: (playingIndex === index ? playProgress : 0) + '%' }"
          />
        </view>
        <text class="duration">{{ formatDisplayDuration(item, index) }}″</text>
        <view v-if="deletable" class="delete-btn" @click.stop="removeItem(index)">
          <uni-icons type="closeempty" size="14" color="#999" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
  import { ref, onUnmounted, nextTick } from 'vue'

  /** 与报修页录音条目一致：duration 为毫秒（可选，用于解码前展示） */
  export interface VoicePlaybackItem {
    url: string
    duration?: number
  }

  const props = withDefaults(
    defineProps<{
      items: VoicePlaybackItem[]
      /** 是否显示删除（与报修页 VoiceInputField 拆分时由父级更新列表） */
      deletable?: boolean
    }>(),
    {
      items: () => [],
      deletable: false
    }
  )

  const emit = defineEmits<{
    (e: 'remove', index: number): void
  }>()
  // 播放索引
  const playingIndex = ref(-1)
  // 播放进度
  const playProgress = ref(0)
  // 忽略下次音频停止
  const ignoreNextAudioStop = ref(false)
  // 忽略下次音频停止计时器
  let ignoreNextAudioStopTimer: ReturnType<typeof setTimeout> | null = null
  // 上一次成功发起播放的地址，用于同一段重播时 seek(0)，避免清空 src 触发各端 onError
  const lastVoicePlayPath = ref('')

  // 音频上下文
  const innerAudioContext =
    uni.createInnerAudioContext && typeof uni.createInnerAudioContext === 'function'
      ? uni.createInnerAudioContext()
      : null

  /**
   * 清除忽略下次音频停止
   * @returns void
   */
  const clearIgnoreAudioStop = () => {
    // 如果忽略下次音频停止计时器存在，则清除忽略下次音频停止计时器
    if (ignoreNextAudioStopTimer) {
      clearTimeout(ignoreNextAudioStopTimer)
      ignoreNextAudioStopTimer = null
    }
    // 设置忽略下次音频停止为false
    ignoreNextAudioStop.value = false
  }

  /**
   * 格式化时长（秒）
   * @param ms - 毫秒
   * @returns 时长（秒）
   */
  const formatDurationSec = (ms: number) => {
    // 如果毫秒不存在或小于等于0，则返回0
    if (!ms || ms <= 0) return '0'
    return String(Math.max(0, Math.round(ms / 1000)))
  }

  /**
   * 格式化显示时长
   * @param item - 语音项
   * @param index - 索引
   * @returns 显示时长
   */
  const formatDisplayDuration = (item: VoicePlaybackItem, index: number) => {
    // 如果音频上下文存在且正在播放，则返回解码时长
    if (innerAudioContext && playingIndex.value === index) {
      const d = innerAudioContext.duration
      // 如果解码时长存在且大于0且不为NaN，则返回解码时长
      if (typeof d === 'number' && d > 0 && !Number.isNaN(d)) {
        // 返回解码时长
        return String(Math.max(0, Math.round(d)))
      }
    }
    // 返回时长
    return formatDurationSec(item.duration ?? 0)
  }

  /**
   * 获取播放总时长（秒）
   * @param index - 索引
   * @returns 播放总时长（秒）
   */
  const getPlayTotalSec = (index: number): number => {
    // 获取音频上下文时长
    const ctx = innerAudioContext?.duration
    // 如果音频上下文时长存在且大于0且不为NaN，则返回音频上下文时长
    if (typeof ctx === 'number' && ctx > 0 && !Number.isNaN(ctx)) {
      return ctx
    }
    // 获取语音项
    const item = props.items[index]
    // 获取录音时长
    const recorded = item?.duration && item.duration > 0 ? item.duration / 1000 : 0
    // 如果录音时长大于0，则返回录音时长，否则返回0
    return recorded > 0 ? recorded : 0
  }

  /**
   * 音频上下文事件
   * @returns void
   */
  if (innerAudioContext) {
    /**
     * 音频时间更新事件
     * @returns void
     */
    innerAudioContext.onTimeUpdate(() => {
      // 获取播放索引
      const idx = playingIndex.value
      // 如果播放索引小于0，则返回
      if (idx < 0) return
      // 获取播放总时长
      const total = getPlayTotalSec(idx)
      // 如果播放总时长小于等于0，则返回
      if (total <= 0) return
      // 获取当前播放时间
      const cur = innerAudioContext.currentTime || 0
      // 如果当前播放时间不存在或小于等于0，则返回
      if (!cur || cur <= 0) return
      // 计算播放进度
      playProgress.value = Math.min(100, Math.max(0, (cur / total) * 100))
    })
    /**
     * 音频结束事件
     * @returns void
     */
    innerAudioContext.onEnded(() => {
      // 停止播放
      playingIndex.value = -1
      playProgress.value = 0
    })
    /**
     * 音频停止事件
     * @returns void
     */
    innerAudioContext.onStop(() => {
      // 如果忽略下次音频停止，则清除忽略下次音频停止
      if (ignoreNextAudioStop.value) {
        clearIgnoreAudioStop()
        return
      }
      // 停止播放
      playingIndex.value = -1
      playProgress.value = 0
    })
    innerAudioContext.onError(() => {
      // 停止播放
      playingIndex.value = -1
      playProgress.value = 0
    })
  }

  /**
   * 计划清除忽略下次音频停止
   * @returns void
   */
  const scheduleClearIgnoreStop = () => {
    // 如果忽略下次音频停止计时器存在，则清除忽略下次音频停止计时器
    if (ignoreNextAudioStopTimer) {
      // 清除忽略下次音频停止计时器
      clearTimeout(ignoreNextAudioStopTimer)
      ignoreNextAudioStopTimer = null
    }
    // 设置忽略下次音频停止计时器
    ignoreNextAudioStopTimer = setTimeout(() => {
      // 清除忽略下次音频停止计时器
      ignoreNextAudioStopTimer = null
      if (ignoreNextAudioStop.value) ignoreNextAudioStop.value = false
    }, 320)
  }

  /**
   * 播放语音
   * @param item - 语音项
   * @param index - 索引
   * @returns void
   */
  const playVoice = async (item: VoicePlaybackItem, index: number) => {
    // 如果语音项不存在，则返回
    if (!item.url) return
    if (!innerAudioContext) {
      uni.showToast({ title: '当前环境不支持播放', icon: 'none' })
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
    const path = item.url
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
   * 删除一条（确认后交给父级更新 items）
   * @param index - 索引
   */
  const removeItem = (index: number) => {
    if (!props.deletable) return
    if (index < 0 || index >= props.items.length) return
    uni.showModal({
      title: '删除语音',
      content: '确定删除这条语音吗？',
      confirmText: '删除',
      cancelText: '取消',
      success: (res) => {
        if (!res.confirm) return
        if (index < 0 || index >= props.items.length) return
        if (playingIndex.value === index) {
          clearIgnoreAudioStop()
          try {
            innerAudioContext?.stop()
          } catch {
            /* noop */
          }
        } else if (playingIndex.value > index) {
          playingIndex.value--
        }
        emit('remove', index)
      }
    })
  }

  onUnmounted(() => {
    lastVoicePlayPath.value = ''
    clearIgnoreAudioStop()
    innerAudioContext?.destroy()
  })
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  /* 与 VoiceInputField 语音列表同一套布局与样式 */
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
          background-color: #d9dfe6;
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
