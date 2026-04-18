import { ref } from 'vue'

/**
 * 配合 scroll-view 的 refresher-enabled / refresher-triggered / @refresherrefresh 使用
 */
export function useScrollRefresher(run: () => Promise<void>) {
  const refresherTriggered = ref(false)

  const onRefresherRefresh = async () => {
    if (refresherTriggered.value) return
    refresherTriggered.value = true
    try {
      await run()
    } finally {
      refresherTriggered.value = false
    }
  }

  return { refresherTriggered, onRefresherRefresh }
}
