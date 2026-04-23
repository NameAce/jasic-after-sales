<template>
  <!-- 语音输入（按住录音 + 录音列表） -->
  <uni-forms-item name="voice">
    <template #label>
      <view class="voice-label">
        <text class="label-text">{{ label }}</text>
      </view>
    </template>
    <view class="voice-input-wrapper">
      <!-- 「按住说话」条 + 锚定在条上沿的半圆遮罩 -->
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
      <!-- 录音列表：委托给 VoicePlaybackList 渲染播放与删除按钮 -->
      <VoicePlaybackList
        v-if="innerList.length"
        :items="playbackItems"
        deletable
        @remove="onRemoveVoice"
      />
      <view v-else class="voice-placeholder">
        <text>暂无录音</text>
      </view>
    </view>
  </uni-forms-item>
</template>

<script setup lang="ts">
  import { ref, computed, onUnmounted } from 'vue'
  import { uploadSystemFile } from '@/api/file'
  import type { VoicePlaybackItem } from '@/components/VoicePlaybackList/VoicePlaybackList.vue'

  /** 语音条目：tempFilePath 为录制后本地临时文件，url 为上传回服务端的可访问地址 */
  export interface VoiceItem {
    tempFilePath: string
    duration: number
    fileId?: number
    url?: string
  }

  /** 最短有效录音时长（毫秒），低于此不入库 */
  const MIN_DURATION_MS = 600
  /** 单次最长录音（毫秒），与 RecorderManager.start duration 对齐 */
  const MAX_DURATION_MS = 60000
  /** 上滑取消：相对按下位置向上位移超过该值（px）视为取消区 */
  const SLIDE_CANCEL_PX = 72

  const props = withDefaults(
    defineProps<{
      modelValue?: VoiceItem[]
      label?: string
    }>(),
    {
      modelValue: () => [],
      label: '语音说明'
    }
  )

  const emit = defineEmits<{
    (e: 'update:modelValue', value: VoiceItem[]): void
  }>()

  const innerList = computed({
    get: () => props.modelValue || [],
    set: (val) => emit('update:modelValue', val)
  })

  /**
   * 传给 VoicePlaybackList 的条目：优先用本次录制的本地 tempFilePath（可直接播放），
   * 回退到上传后的 url（走组件内部下载逻辑）。
   */
  const playbackItems = computed<VoicePlaybackItem[]>(() =>
    innerList.value.map((v) => ({
      url: v.tempFilePath || v.url || '',
      duration: v.duration
    }))
  )

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

  const recorderManager = uni.getRecorderManager ? uni.getRecorderManager() : null

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

  const overlayMainHint = computed(() =>
    slideCancelActive.value ? '松开手指，取消发送' : '松开 发送'
  )

  const overlaySubHint = computed(() => (slideCancelActive.value ? '' : '手指上滑，取消发送'))

  /** 从触摸事件取纵向坐标（各端 clientY / pageY 不一致） */
  const getTouchClientY = (e: {
    touches?: { clientY?: number; pageY?: number }[]
    changedTouches?: { clientY?: number; pageY?: number }[]
  }): number | null => {
    const t = (e.touches && e.touches[0]) || (e.changedTouches && e.changedTouches[0])
    if (!t) return null
    const y = t.clientY ?? t.pageY
    return typeof y === 'number' && !Number.isNaN(y) ? y : null
  }

  const updateSlideCancelByClientY = (clientY: number) => {
    if (recordTouchStartY === null) return
    slideCancelActive.value = recordTouchStartY - clientY > SLIDE_CANCEL_PX
  }

  const clearSlideGesture = () => {
    slideCancelActive.value = false
    recordTouchStartY = null
  }

  const clearRecordingTimer = () => {
    if (recordingTimer) {
      clearInterval(recordingTimer)
      recordingTimer = null
    }
    recordingElapsedSec.value = 0
  }

  const startRecordingTimer = () => {
    clearRecordingTimer()
    recordingElapsedSec.value = 0
    recordingTimer = setInterval(() => {
      recordingElapsedSec.value++
    }, 1000)
  }

  /** 录音真正开始时短震（App / 小程序等）；H5 无能力时静默跳过 */
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

  /** 申请录音权限（小程序等）；不支持时直接通过 */
  const ensureRecordPermission = (): Promise<boolean> => {
    return new Promise((resolve) => {
      const auth = (
        uni as unknown as {
          authorize?: (o: {
            scope: string
            success?: () => void
            fail?: (err: unknown) => void
          }) => void
        }
      ).authorize
      if (typeof auth !== 'function') {
        resolve(true)
        return
      }
      auth({
        scope: 'scope.record',
        success: () => resolve(true),
        fail: () => {
          uni.showModal({
            title: '需要录音权限',
            content: '请允许使用麦克风后再试，可在设置中开启',
            confirmText: '去设置',
            success: (r) => {
              if (r.confirm && typeof uni.openSetting === 'function') {
                uni.openSetting({})
              }
            }
          })
          resolve(false)
        }
      })
    })
  }

  /** 申请权限 → 若仍按住则 start；重复 start 由 recordSessionPending 防住 */
  const startRecord = async () => {
    if (!recorderManager) {
      uni.showToast({ title: '当前环境不支持录音', icon: 'none', duration: 1500 })
      return
    }
    if (recordSessionPending.value) return

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
      uni.showToast({ title: '无法开始录音', icon: 'none', duration: 1500 })
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

    recorderManager.onStop(async (res: any) => {
      endRecordInteraction()
      isRecording.value = false
      recordSessionPending.value = false
      cancelPendingStart.value = false
      clearRecordingTimer()

      if (discardNextRecording.value) {
        discardNextRecording.value = false
        clearSlideGesture()
        uni.showToast({ title: '已取消', icon: 'none', duration: 1500 })
        return
      }

      clearSlideGesture()

      const duration = typeof res.duration === 'number' ? res.duration : 0
      const tempFilePath = res.tempFilePath as string | undefined
      if (!tempFilePath) {
        uni.showToast({ title: '未生成录音文件', icon: 'none', duration: 1500 })
        return
      }
      if (duration < MIN_DURATION_MS) {
        uni.showToast({ title: '录音时间太短', icon: 'none', duration: 1500 })
        return
      }
      uni.showLoading({ title: '上传中...' })
      try {
        const uploaded = await uploadSystemFile(tempFilePath)
        const record: VoiceItem = {
          tempFilePath,
          duration,
          fileId: uploaded.fileId,
          url: uploaded.previewUrl
        }
        emit('update:modelValue', [...innerList.value, record])
      } catch (err: unknown) {
        const msg =
          (err as { message?: string })?.message || (err as { msg?: string })?.msg || '语音上传失败'
        uni.showToast({ title: msg, icon: 'none', duration: 1500 })
      } finally {
        uni.hideLoading()
      }
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
      uni.showToast({ title: '录音失败，请重试', icon: 'none', duration: 1500 })
    })
  }

  /**
   * VoicePlaybackList 抛上来的删除请求：录制阶段由本组件承担确认弹窗与 v-model 提交，
   * 统一入口以便与「暂无录音」占位、录制流程状态一同管理。
   */
  const onRemoveVoice = (index: number) => {
    if (index < 0 || index >= innerList.value.length) return
    uni.showModal({
      title: '删除语音',
      content: '确定删除这条语音吗？',
      confirmText: '删除',
      cancelText: '取消',
      success: (res) => {
        if (!res.confirm) return
        if (index < 0 || index >= innerList.value.length) return
        const list = [...innerList.value]
        list.splice(index, 1)
        emit('update:modelValue', list)
      }
    })
  }

  onUnmounted(() => {
    detachMouseRecordListeners()
    clearRecordingTimer()
    if (recorderManager && (isRecording.value || recordSessionPending.value)) {
      try {
        recorderManager.stop()
      } catch {
        /* noop */
      }
    }
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
  }
</style>
