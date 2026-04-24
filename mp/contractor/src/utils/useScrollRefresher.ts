import { ref, nextTick } from 'vue'

/**
 * 配合 scroll-view 的 refresher-enabled / refresher-triggered / @refresherrefresh 使用
 *
 * 说明（尤其 mp-weixin）：在 `await run()` 后立即把 `refresher-triggered` 置回 false，
 * 部分基础库会出现刷新头无法收起或触发「异步事件处理」类告警；因此在 finally 里
 * 先 `nextTick` 再短延迟后关闭，与社区常见写法一致。
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
      await nextTick()
      await new Promise<void>((resolve) => setTimeout(resolve, 32))
      refresherTriggered.value = false
    }
  }

  return { refresherTriggered, onRefresherRefresh }
}
