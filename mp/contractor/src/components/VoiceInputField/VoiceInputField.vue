<template>
  <!-- 语音输入 -->
  <uni-forms-item name="voice">
    <!-- 标签 -->
    <template #label>
      <view class="voice-label">
        <text class="label-text">{{ label }}</text>
      </view>
    </template>
    <!-- 语音列表 -->
    <view class="voice-input-wrapper">
      <!-- 遮罩锚定在「按住说话」条上方 -->
      <view class="record-anchor">
        <view
          v-if="recordOverlayVisible"
          class="record-overlay"
          :class="{ 'record-overlay--cancel': slideCancelActive }"
          aria-hidden="true"
        >
          <!-- 直径 = 按住条宽度，整圆贴条上沿，父级裁切后仅见上半圆 -->
          <view class="record-overlay__semicircle-bg" />
          <view class="record-overlay__panel">
            <view class="record-overlay__mic-wrap">
              <view class="record-overlay__mic-ring">
                <uni-icons type="mic-filled" size="24" color="#fff"></uni-icons>
              </view>
            </view>
            <text class="record-overlay__hint-main">{{ overlayMainHint }}</text>
            <text v-if="overlaySubHint" class="record-overlay__hint-sub">{{ overlaySubHint }}</text>
          </view>
        </view>
        <view
          class="record-bar"
          :class="{
            'record-bar--press': pressActive,
            'record-bar--recording': isRecording
          }"
          @touchstart.prevent="onRecordTouchStart"
          @touchmove.prevent="onRecordTouchMove"
          @touchend.prevent="onPressEnd"
          @touchcancel="onPressEnd"
          @mousedown.prevent="onMouseDownRecord"
          @mouseleave="onMouseLeaveRecord"
        >
          <text class="record-bar__text">{{ recordBarIdleText }}</text>
        </view>
      </view>
      <!-- 语音列表（可与 VoicePlaybackList 拆分时关闭） -->
      <view v-if="innerList.length && showRecordedList" class="voice-list">
        <!-- 语音项 -->
        <view
          v-for="(item, index) in innerList"
          :key="item.tempFilePath || index"
          class="voice-item"
          :class="{ 'voice-item--playing': playingIndex === index }"
        >
          <!-- 播放按钮 -->
          <view class="play-btn" @click="playVoice(item, index)">
            <uni-icons
              :type="playingIndex === index ? 'circle-filled' : 'circle'"
              size="20"
              color="#f26604"
            ></uni-icons>
          </view>
          <!-- 进度条 -->
          <view class="progress-bar">
            <view
              class="progress-inner"
              :style="{ width: (playingIndex === index ? playProgress : 0) + '%' }"
            ></view>
          </view>
          <!-- 时长 -->
          <text class="duration">{{ formatDisplayDuration(item, index) }}″</text>
          <!-- 删除按钮 -->
          <view class="delete-btn" @click="removeVoice(index)">
            <uni-icons type="closeempty" size="14" color="#999"></uni-icons>
          </view>
        </view>
      </view>
      <!-- 语音占位符 -->
      <view v-else-if="showRecordedList" class="voice-placeholder">
        <text>暂无录音</text>
      </view>
    </view>
  </uni-forms-item>
</template>

<script setup lang="ts">
  import { ref, computed, onUnmounted, nextTick } from 'vue'

  /**
   * 语音项
   */
  export interface VoiceItem {
    tempFilePath: string
    duration: number
  }

  /** 最短有效录音时长（毫秒），低于此不入库 */
  const MIN_DURATION_MS = 600
  /** 单次最长录音（毫秒），与 RecorderManager.start duration 对齐 */
  const MAX_DURATION_MS = 60000
  /** 上滑取消：相对按下位置向上位移超过该值（px）视为取消区 */
  const SLIDE_CANCEL_PX = 72

  // 定义 props
  const props = withDefaults(
    defineProps<{
      // 语音列表
      modelValue?: VoiceItem[]
      // 标签
      label?: string
      /** 是否展示内置列表与「暂无录音」占位（与 VoicePlaybackList 拆分时可置为 false） */
      showRecordedList?: boolean
    }>(),
    {
      modelValue: () => [],
      label: '语音说明',
      showRecordedList: true
    }
  )

  // 定义事件
  const emit = defineEmits<{
    (e: 'update:modelValue', value: VoiceItem[]): void
  }>()

  // 语音列表
  const innerList = computed({
    get: () => props.modelValue || [],
    set: (val) => emit('update:modelValue', val)
  })

  /**
   * ---------- 按住录音状态（与 RecorderManager 回调配合）----------
   * pressActive：手指/鼠标是否仍按在「按住说话」条上（授权等待期间也依赖它决定是否继续 start）
   * recordSessionPending：已调用 recorder.start()，尚未 onStop（含尚未 onStart 的阶段）
   * isRecording：onStart 已回调，正在采集
   * cancelPendingStart：onStart 前已松手，须在 onStart 里立刻 stop，避免录满一整段
   * discardNextRecording：松手时处于上滑取消区，onStop 丢弃文件、不入库
   * 全屏遮罩：仅当按住条且引擎 pending/录音中；松手、上滑取消、onStop、onError 均会 endRecordInteraction 关闭
   */
  const pressActive = ref(false)
  const recordSessionPending = ref(false)
  const isRecording = ref(false)
  const cancelPendingStart = ref(false)
  const recordingElapsedSec = ref(0)
  let recordingTimer: ReturnType<typeof setInterval> | null = null

  let recordTouchStartY: number | null = null
  const slideCancelActive = ref(false)
  const discardNextRecording = ref(false)

  /** H5：鼠标模拟按住，需在 window 上跟手位移判断上滑取消 */
  let mouseRecordDown = false

  // 播放索引
  const playingIndex = ref(-1)
  // 播放进度
  const playProgress = ref(0)
  // 即将发起新一段播放前会 stop()，各端 onStop 可能晚于 play()，误清 UI；忽略这一次 onStop
  const ignoreNextAudioStop = ref(false)
  // 忽略停止计时器
  let ignoreNextAudioStopTimer: ReturnType<typeof setTimeout> | null = null
  /**
   * 清除忽略停止
   * @returns void
   */
  const clearIgnoreAudioStop = () => {
    if (ignoreNextAudioStopTimer) {
      clearTimeout(ignoreNextAudioStopTimer)
      ignoreNextAudioStopTimer = null
    }
    ignoreNextAudioStop.value = false
  }
  // 上一次成功发  起播放的地址，用于同一段重播时 seek(0)，避免清空 src 触发各端 onError
  const lastVoicePlayPath = ref('')
  // 录音管理器
  const recorderManager = uni.getRecorderManager ? uni.getRecorderManager() : null
  // 音频上下文
  const innerAudioContext =
    uni.createInnerAudioContext && typeof uni.createInnerAudioContext === 'function'
      ? uni.createInnerAudioContext()
      : null

  /** 录音条未录音时文案（对齐微信输入框） */
  const recordBarIdleText = computed(() => {
    if (isRecording.value || recordSessionPending.value) {
      return recordingElapsedSec.value > 0 ? `正在录音 ${recordingElapsedSec.value}s` : '正在录音…'
    }
    return '按住 说话'
  })

  /** 全屏遮罩：必须按住条，且已开始会话（pending）或正在录（isRecording） */
  const recordOverlayVisible = computed(
    () => pressActive.value && (recordSessionPending.value || isRecording.value)
  )

  /** 遮罩主提示 */
  const overlayMainHint = computed(() =>
    slideCancelActive.value ? '松开手指，取消发送' : '松开 发送'
  )

  /** 遮罩副提示 */
  const overlaySubHint = computed(() => (slideCancelActive.value ? '' : '手指上滑，取消发送'))

  /**
   * 从触摸事件取纵向坐标（各端 clientY / pageY 不一致）
   * @param e - 触摸事件
   * @returns Y 或 null
   */
  const getTouchClientY = (e: {
    touches?: { clientY?: number; pageY?: number }[]
    changedTouches?: { clientY?: number; pageY?: number }[]
  }): number | null => {
    const t = (e.touches && e.touches[0]) || (e.changedTouches && e.changedTouches[0])
    if (!t) return null
    const y = t.clientY ?? t.pageY
    return typeof y === 'number' && !Number.isNaN(y) ? y : null
  }

  /**
   * 根据当前触点 Y 更新是否处于上滑取消区
   * @param clientY - 视口 Y
   */
  const updateSlideCancelByClientY = (clientY: number) => {
    if (recordTouchStartY === null) return
    slideCancelActive.value = recordTouchStartY - clientY > SLIDE_CANCEL_PX
  }

  const clearSlideGesture = () => {
    slideCancelActive.value = false
    recordTouchStartY = null
  }

  /**
   * 清除录音计时器
   * @returns void
   */
  const clearRecordingTimer = () => {
    if (recordingTimer) {
      clearInterval(recordingTimer)
      recordingTimer = null
    }
    recordingElapsedSec.value = 0
  }

  /**
   * 开始录音计时器
   * @returns void
   */
  const startRecordingTimer = () => {
    clearRecordingTimer()
    recordingElapsedSec.value = 0
    recordingTimer = setInterval(() => {
      recordingElapsedSec.value++
    }, 1000)
  }

  /**
   * 录音真正开始时短震（App / 小程序等）；H5 无能力时静默跳过
   * @returns void
   */
  const vibrateRecordStart = () => {
    const v = (uni as unknown as { vibrateShort?: (opts?: { type?: string }) => void }).vibrateShort
    if (typeof v !== 'function') return
    try {
      v({ type: 'medium' })
    } catch {
      try {
        v()
      } catch {
        /* noop */
      }
    }
  }

  /**
   * 展示用：duration 为各端返回的毫秒数
   * @param ms - 毫秒数
   * @returns 时长
   */
  const formatDurationSec = (ms: number) => {
    // 如果毫秒数不存在或小于等于0，则返回0
    if (!ms || ms <= 0) return '0'
    // 返回时长
    return String(Math.max(0, Math.round(ms / 1000)))
  }

  /**
   * 列表时长：未播放用录音元数据；正在播放的条目优先用解码时长（与 currentTime / 进度条同源），避免条与数字不一致
   * @param item - 语音项
   * @param index - 索引
   * @returns 时长
   */
  const formatDisplayDuration = (item: VoiceItem, index: number) => {
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
    return formatDurationSec(item.duration) // 返回时长
  }

  /**
   * 申请录音权限（小程序等）；不支持时直接通过
   * @returns 是否授权
   */
  const ensureRecordPermission = (): Promise<boolean> => {
    // 返回是否授权
    return new Promise((resolve) => {
      // 授权
      const auth = (
        uni as unknown as {
          authorize?: (o: {
            scope: string
            success?: () => void
            fail?: (err: unknown) => void
          }) => void
        }
      ).authorize
      // 如果授权函数不存在，则返回true
      if (typeof auth !== 'function') {
        resolve(true)
        return
      }
      // 授权
      auth({
        scope: 'scope.record',
        success: () => resolve(true),
        fail: () => {
          // 显示模态框
          uni.showModal({
            title: '需要录音权限',
            content: '请允许使用麦克风后再试，可在设置中开启',
            confirmText: '去设置',
            success: (r) => {
              // 如果确认且打开设置函数存在，则打开设置
              if (r.confirm && typeof uni.openSetting === 'function') {
                uni.openSetting({})
              }
            }
          })
          // 返回false
          resolve(false)
        }
      })
    })
  }

  /**
   * 音频上下文事件
   * @returns void
   */
  if (innerAudioContext) {
    /**
     * 播放总时长（秒）：必须与 innerAudioContext.currentTime 同一套时间轴。
     * 优先用解码得到的 duration；元数据 item.duration 仅作 metadata 未就绪时的回退。
     */
    const getPlayTotalSec = (index: number): number => {
      // 获取音频上下文时长
      const ctx = innerAudioContext.duration
      // 如果音频上下文时长存在且大于0且不为NaN，则返回音频上下文时长
      if (typeof ctx === 'number' && ctx > 0 && !Number.isNaN(ctx)) {
        return ctx
      }
      // 获取语音项
      const item = innerList.value[index]
      // 获取录音时长
      const recorded = item?.duration && item.duration > 0 ? item.duration / 1000 : 0
      // 如果录音时长大于0，则返回录音时长，否则返回0
      return recorded > 0 ? recorded : 0
    }

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
      const cur = innerAudioContext.currentTime || 0
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
      // 如果忽略停止，则清除忽略停止
      if (ignoreNextAudioStop.value) {
        clearIgnoreAudioStop()
        return
      }
      // 停止播放
      playingIndex.value = -1
      playProgress.value = 0
    })
    /**
     * 音频错误事件
     * @returns void
     */
    innerAudioContext.onError(() => {
      // 停止播放
      playingIndex.value = -1
      playProgress.value = 0
    })
  }

  /** 申请权限 → 若仍按住则 start；重复 start 由 recordSessionPending 防住 */
  const startRecord = async () => {
    if (!recorderManager) {
      uni.showToast({ title: '当前环境不支持录音', icon: 'none' })
      return
    }
    if (recordSessionPending.value) return

    clearIgnoreAudioStop()
    innerAudioContext?.stop()
    cancelPendingStart.value = false

    const ok = await ensureRecordPermission()
    if (!ok) return
    if (!pressActive.value) return

    recordSessionPending.value = true
    try {
      recorderManager.start({
        format: 'mp3',
        duration: MAX_DURATION_MS,
        sampleRate: 44100,
        numberOfChannels: 1,
        encodeBitRate: 96000
      })
    } catch (e) {
      recordSessionPending.value = false
      console.error('recorder.start', e)
      uni.showToast({ title: '无法开始录音', icon: 'none' })
    }
  }

  /** 已 onStart：stop()；仅 pending：交给 onStart 里立刻 stop */
  const stopRecord = () => {
    if (!recorderManager) return
    if (isRecording.value) {
      try {
        recorderManager.stop()
      } catch {
        /* noop */
      }
      return
    }
    if (recordSessionPending.value) cancelPendingStart.value = true
  }

  /** 一次「按住」：复位上滑状态、标记按住、异步开始录音 */
  const armRecordPress = (startY: number | null) => {
    recordTouchStartY = startY
    slideCancelActive.value = false
    discardNextRecording.value = false
    pressActive.value = true
    startRecord()
  }

  const onRecordTouchStart = (e: { touches?: { clientY?: number; pageY?: number }[] }) => {
    armRecordPress(getTouchClientY(e))
  }

  const onRecordTouchMove = (e: { touches?: { clientY?: number; pageY?: number }[] }) => {
    if (!pressActive.value) return
    const y = getTouchClientY(e)
    if (y !== null && recordTouchStartY !== null) updateSlideCancelByClientY(y)
  }

  const onWindowMouseMoveRecord = (e: { clientY: number }) => {
    if (!mouseRecordDown || recordTouchStartY === null) return
    updateSlideCancelByClientY(e.clientY)
  }

  const onWindowMouseUpRecord = () => {
    if (!mouseRecordDown) return
    onPressEnd()
  }

  const detachMouseRecordListeners = () => {
    mouseRecordDown = false
    if (typeof window !== 'undefined') {
      window.removeEventListener('mousemove', onWindowMouseMoveRecord)
      window.removeEventListener('mouseup', onWindowMouseUpRecord)
    }
  }

  /** 松手/会话结束：关遮罩（pressActive）、卸鼠标监听；onStop/onError 也会调，防止漏 touchend */
  const endRecordInteraction = () => {
    pressActive.value = false
    detachMouseRecordListeners()
  }

  /** 松手：若在上滑取消区则 onStop 丢弃；并 stop 或取消 pending start */
  const onPressEnd = () => {
    if (slideCancelActive.value) discardNextRecording.value = true
    endRecordInteraction()
    stopRecord()
  }

  const onMouseDownRecord = (e: { clientY: number }) => {
    mouseRecordDown = true
    if (typeof window !== 'undefined') {
      window.addEventListener('mousemove', onWindowMouseMoveRecord)
      window.addEventListener('mouseup', onWindowMouseUpRecord)
    }
    armRecordPress(e.clientY)
  }

  const onMouseLeaveRecord = () => {
    if (mouseRecordDown) onWindowMouseUpRecord()
  }

  if (recorderManager) {
    recorderManager.onStart(() => {
      isRecording.value = true
      if (cancelPendingStart.value) {
        cancelPendingStart.value = false
        try {
          recorderManager.stop()
        } catch {
          /* noop */
        }
        return
      }
      startRecordingTimer()
      vibrateRecordStart()
    })

    recorderManager.onStop((res: any) => {
      endRecordInteraction()
      isRecording.value = false
      recordSessionPending.value = false
      cancelPendingStart.value = false
      clearRecordingTimer()

      if (discardNextRecording.value) {
        discardNextRecording.value = false
        clearSlideGesture()
        uni.showToast({ title: '已取消', icon: 'none' })
        return
      }

      clearSlideGesture()

      const duration = typeof res.duration === 'number' ? res.duration : 0
      const tempFilePath = res.tempFilePath as string | undefined
      if (!tempFilePath) {
        uni.showToast({ title: '未生成录音文件', icon: 'none' })
        return
      }
      if (duration < MIN_DURATION_MS) {
        uni.showToast({ title: '录音时间太短', icon: 'none' })
        return
      }
      const record: VoiceItem = { tempFilePath, duration }
      emit('update:modelValue', [...innerList.value, record])
    })

    recorderManager.onError((err: unknown) => {
      endRecordInteraction()
      isRecording.value = false
      recordSessionPending.value = false
      cancelPendingStart.value = false
      discardNextRecording.value = false
      clearSlideGesture()
      clearRecordingTimer()
      console.error('录音错误', err)
      uni.showToast({ title: '录音失败，请重试', icon: 'none' })
    })
  }

  /**
   * 计划清除忽略停止
   * @returns void
   */
  const scheduleClearIgnoreStop = () => {
    // 如果忽略停止计时器存在，则清除忽略停止计时器
    if (ignoreNextAudioStopTimer) {
      clearTimeout(ignoreNextAudioStopTimer)
      ignoreNextAudioStopTimer = null
    }
    // 设置忽略停止计时器
    ignoreNextAudioStopTimer = setTimeout(() => {
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
  const playVoice = async (item: VoiceItem, index: number) => {
    // 如果语音项不存在，则返回
    if (!item.tempFilePath) return
    // 如果音频上下文不存在，则显示提示
    if (!innerAudioContext) {
      uni.showToast({ title: '当前环境不支持播放', icon: 'none' })
      return
    }
    // 如果正在播放，则清除忽略停止
    if (playingIndex.value === index) {
      clearIgnoreAudioStop()
      innerAudioContext.stop()
      return
    }
    // 设置忽略停止
    ignoreNextAudioStop.value = true
    scheduleClearIgnoreStop()
    // 停止音频上下文
    try {
      innerAudioContext.stop()
    } catch {
      /* noop */
    }
    // 等待下一帧
    await nextTick()
    // 设置播放索引
    playingIndex.value = index
    playProgress.value = 0
    const path = item.tempFilePath
    // 获取音频上下文
    const ac = innerAudioContext as {
      src: string
      play: () => void
      seek?: (position: number) => void
    }
    // 如果上次播放路径与当前路径相同且路径存在，则重播
    const replaySame = lastVoicePlayPath.value === path && !!path
    // 如果重播相同且seek函数存在，则seek到0
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
   * 删除语音
   * @param index - 索引
   * @returns void
   */
  const removeVoice = (index: number) => {
    // 如果索引小于0或大于等于语音列表长度，则返回
    if (index < 0 || index >= innerList.value.length) return
    // 显示模态框
    uni.showModal({
      title: '删除语音',
      content: '确定删除这条语音吗？',
      confirmText: '删除',
      cancelText: '取消',
      success: (res) => {
        if (!res.confirm) return
        // 获取语音列表
        const list = [...innerList.value]
        // 如果索引小于0或大于等于语音列表长度，则返回
        if (index < 0 || index >= list.length) return
        // 如果正在播放，则清除忽略停止
        if (playingIndex.value === index) {
          clearIgnoreAudioStop()
          innerAudioContext?.stop()
        } else if (playingIndex.value > index) {
          // 播放索引减1
          playingIndex.value--
        }
        // 删除语音
        list.splice(index, 1)
        // 更新语音列表
        emit('update:modelValue', list)
      }
    })
  }

  /**
   * 卸载组件
   * @returns void
   */
  onUnmounted(() => {
    detachMouseRecordListeners()
    // 清空上次播放路径
    lastVoicePlayPath.value = ''
    // 清除忽略停止
    clearIgnoreAudioStop()
    // 清除录音计时器
    clearRecordingTimer()
    // 如果录音管理器存在且正在录音或录音会话等待，则停止录音
    if (recorderManager && (isRecording.value || recordSessionPending.value)) {
      try {
        recorderManager.stop()
      } catch {
        /* noop */
      }
    }
    // 销毁音频上下文
    innerAudioContext?.destroy()
  })
</script>

<style lang="scss" scoped>
  @use '@/styles/mixins.scss' as *;
  @use '@/styles/variables.scss' as *;

  .voice-label {
    width: 100%;
    margin-bottom: $space-xs;

    .label-text {
      font-size: $font-md;
      color: $text-main;
      font-weight: bold;
    }
  }

  .voice-input-wrapper {
    position: relative;
    width: 100%;
    overflow: visible;

    /* 与按住条同宽，遮罩自下向上锚定在条的上沿 */
    .record-anchor {
      position: relative;
      width: 100%;
      overflow: visible;
    }

    /* 直径 = 条宽 W：圆心在条顶边中点，半径 W/2，裁切框高 = 半径，仅见条上方的半圆 */
    .record-overlay {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 100%;
      width: 100%;
      aspect-ratio: 2 / 1;
      overflow: hidden;
      box-sizing: border-box;
      pointer-events: auto;

      &__semicircle-bg {
        position: absolute;
        left: 50%;
        bottom: 0;
        width: 100%;
        aspect-ratio: 1;
        border-radius: 50%;
        background-color: rgba(0, 0, 0, 0.55);
        transform: translate(-50%, 50%);
        transition: background-color 0.15s ease;
      }

      &--cancel &__semicircle-bg {
        background-color: rgba(0, 0, 0, 0.65);
      }

      &--cancel .record-overlay__mic-ring {
        background-color: rgba(220, 80, 60, 0.95);
        animation: none;
      }

      &__panel {
        position: relative;
        z-index: 1;
        @include flex-column;
        align-items: center;
        justify-content: center;
        box-sizing: border-box;
        width: 100%;
        height: 100%;
        gap: 12rpx;
        padding: 20rpx 32rpx 12rpx;
      }

      &__mic-wrap {
        @include flex-center;
        flex-shrink: 0;
      }

      &__mic-ring {
        width: min(140rpx, 32vw);
        height: min(140rpx, 32vw);
        border-radius: 50%;
        background-color: rgba(7, 193, 96, 0.95);
        @include flex-center;
        box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.22);
        animation: record-mic-pulse 1.1s ease-in-out infinite;
      }

      &__hint-main {
        font-size: 28rpx;
        color: #fff;
        font-weight: 500;
        letter-spacing: 1rpx;
        text-align: center;
        line-height: 1.35;
      }

      &__hint-sub {
        font-size: 22rpx;
        color: rgba(255, 255, 255, 0.72);
        text-align: center;
        line-height: 1.3;
      }
    }

    @keyframes record-mic-pulse {
      0%,
      100% {
        transform: scale(1);
      }
      50% {
        transform: scale(1.06);
      }
    }

    .record-bar {
      width: 100%;
      min-height: 88rpx;
      padding: 24rpx $space-md;
      margin-bottom: $space-sm;
      box-sizing: border-box;
      @include flex-center;
      background-color: #fff5ee;
      border-radius: 12rpx;
      border: 1rpx solid #fff5ee;
      transition:
        background-color 0.12s ease,
        transform 0.12s ease;

      &__text {
        font-size: 30rpx;
        color: #f26604;
        letter-spacing: 2rpx;
        user-select: none;
      }

      &--press {
        background-color: #fff5ee;
        transform: scale(0.99);
      }

      &--recording {
        background-color: #fff5ee;
      }
    }

    .voice-placeholder {
      padding: $space-md;
      text-align: center;
      color: $text-placeholder;
      font-size: $font-sm;
      background-color: #f9f9f9;
      border-radius: $radius-md;
    }

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
