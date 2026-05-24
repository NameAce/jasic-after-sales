import { ref, nextTick, type Ref } from 'vue'

/** 收起 refresher 前等待（mp-weixin / iOS 需略长，否则刷新头可能不收起） */
const REFRESHER_STOP_DELAY_MS = 120

/**
 * 在接口完成后关闭 scroll-view 下拉刷新动画
 * @param triggered - 与 :refresher-triggered 绑定的 ref
 */
async function stopScrollRefresher(triggered: Ref<boolean>) {
  await nextTick()
  triggered.value = false
  await new Promise<void>((resolve) => setTimeout(resolve, REFRESHER_STOP_DELAY_MS))
  triggered.value = false
}

/**
 * 配合 scroll-view 的 refresher-enabled / refresher-triggered / @refresherrefresh 使用
 *
 * 说明：须在 `await run()`（列表接口）完成后再将 refresher-triggered 置回 false，
 * 否则刷新头无法正确收起；finally 内 nextTick + 短延迟与微信基础库常见写法一致。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useScrollRefresher(run: () => Promise<void>) {
  const refresherTriggered = ref(false)

  const onRefresherRefresh = async () => {
    if (refresherTriggered.value) return
    refresherTriggered.value = true
    try {
      await run()
    } catch (e) {
      console.error('[useScrollRefresher] refresh failed', e)
    } finally {
      await stopScrollRefresher(refresherTriggered)
    }
  }

  return { refresherTriggered, onRefresherRefresh }
}
