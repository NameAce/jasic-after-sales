import { ref, nextTick, type Ref } from 'vue'

/** 收起 refresher 前等待（mp-weixin / iOS 需略长，否则刷新头可能不收起） */
const REFRESHER_STOP_DELAY_MS = 300

/** 防止多次 stop 交叉改 triggered，后发起的 stop 作废前一轮尾部写入 */
let stopScrollRefresherGeneration = 0

/**
 * 关闭 scroll-view 下拉刷新动画
 *
 * 微信约定：用户下拉不会自动把 refresher-triggered 变为 true；结束时若一直是 false，
 * 再赋 false 不会触发 restore。因此结束时若仍为 false，须先 true 再 false 完成复位。
 *
 * @param triggered - 与 :refresher-triggered 绑定的 ref
 */
export async function stopScrollRefresher(triggered: Ref<boolean>) {
  const generation = ++stopScrollRefresherGeneration
  if (!triggered.value) {
    triggered.value = true
    await nextTick()
  }
  triggered.value = false
  await nextTick()
  await new Promise<void>((resolve) => setTimeout(resolve, REFRESHER_STOP_DELAY_MS))
  if (generation !== stopScrollRefresherGeneration) return
  triggered.value = false
}

/**
 * 配合 scroll-view 的 refresher-enabled / refresher-triggered / @refresherrefresh 使用
 *
 * 要点（见 DCloud/微信社区）：
 * - 用户下拉只触发 @refresherrefresh，不会自动把 triggered 设为 true，须在回调内手动 true；
 * - 置 true 可能再次触发 @refresherrefresh：重复回调须收起动画，否则会一直转圈并挡住 Tab；
 * - 加载完成后 triggered 由 true 变 false 才会收起动画，并应配合 @refresherrestore。
 *
 * @param run - 下拉刷新默认执行的异步逻辑
 * @修改人 黄碧莲
 * @修改时间 2026-05-25
 */
export function useScrollRefresher(run: () => Promise<void>) {
  const refresherTriggered = ref(false)
  const refreshInFlight = ref(false)
  /** 刷新进行中又收到 refresherrefresh 时，首轮结束后补跑一次 */
  const refreshPending = ref(false)
  /** 与 reset / Tab 切换配合：作废进行中的 executeRefresh，避免 finally 再次改 triggered */
  let executeGeneration = 0

  /**
   * 强制复位 refresher 与并发锁（Tab 切换、切走页面、视图 v-if 切换时调用，避免动画残留）
   */
  const resetScrollRefresher = () => {
    executeGeneration++
    stopScrollRefresherGeneration++
    refreshInFlight.value = false
    refreshPending.value = false
    refresherTriggered.value = false
  }

  /** 基础库复位 refresher 时兜底，避免动画卡住 */
  const onRefresherRestore = () => {
    resetScrollRefresher()
  }

  const executeRefresh = async (overrideRun?: () => Promise<void>) => {
    if (refreshInFlight.value) {
      // 微信可能因手动置 true 再次触发 refresherrefresh；须收起本轮动画，否则会卡住并影响 Tab
      refreshPending.value = true
      await stopScrollRefresher(refresherTriggered)
      return
    }

    const generation = ++executeGeneration
    refreshInFlight.value = true
    refreshPending.value = false
    if (!refresherTriggered.value) {
      refresherTriggered.value = true
    }

    try {
      await (overrideRun ?? run)()
    } catch (e) {
      console.error('[useScrollRefresher] refresh failed', e)
    } finally {
      if (generation === executeGeneration) {
        refreshInFlight.value = false
        await stopScrollRefresher(refresherTriggered)
        if (generation === executeGeneration && refreshPending.value) {
          refreshPending.value = false
          await executeRefresh(overrideRun)
        }
      }
    }
  }

  const onRefresherRefresh = () => executeRefresh()

  const runWithRefresherFeedback = (overrideRun?: () => Promise<void>) => executeRefresh(overrideRun)

  return {
    refresherTriggered,
    onRefresherRefresh,
    onRefresherRestore,
    runWithRefresherFeedback,
    resetScrollRefresher
  }
}
