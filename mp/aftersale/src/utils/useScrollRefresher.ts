import { ref, nextTick, type Ref } from 'vue'

/** 收起 refresher 前等待（mp-weixin / iOS 需略长，否则刷新头可能不收起） */
const REFRESHER_STOP_DELAY_MS = 300

/**
 * 关闭 scroll-view 下拉刷新动画
 * @param triggered - 与 :refresher-triggered 绑定的 ref
 */
export async function stopScrollRefresher(triggered: Ref<boolean>) {
  if (!triggered.value) {
    triggered.value = true
    await nextTick()
  }
  triggered.value = false
  await nextTick()
  await new Promise<void>((resolve) => setTimeout(resolve, REFRESHER_STOP_DELAY_MS))
  triggered.value = false
}

/**
 * 配合 scroll-view 的 refresher-enabled / refresher-triggered / @refresherrefresh 使用
 * @param run - 下拉刷新默认执行的异步逻辑
 * @修改人 黄碧莲
 * @修改时间 2026-05-25
 */
export function useScrollRefresher(run: () => Promise<void>) {
  const refresherTriggered = ref(false)
  const refreshInFlight = ref(false)

  const onRefresherRestore = () => {
    refresherTriggered.value = false
    refreshInFlight.value = false
  }

  const executeRefresh = async (overrideRun?: () => Promise<void>) => {
    if (refreshInFlight.value) return
    refreshInFlight.value = true
    if (!refresherTriggered.value) {
      refresherTriggered.value = true
    }
    try {
      await (overrideRun ?? run)()
    } catch (e) {
      console.error('[useScrollRefresher] refresh failed', e)
    } finally {
      refreshInFlight.value = false
      await stopScrollRefresher(refresherTriggered)
    }
  }

  const onRefresherRefresh = () => executeRefresh()
  const runWithRefresherFeedback = (overrideRun?: () => Promise<void>) => executeRefresh(overrideRun)

  return {
    refresherTriggered,
    onRefresherRefresh,
    onRefresherRestore,
    runWithRefresherFeedback
  }
}
