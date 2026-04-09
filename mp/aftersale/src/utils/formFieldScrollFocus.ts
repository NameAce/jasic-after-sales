import { nextTick, type Ref } from 'vue'

/** 与 FormItemAnchor 一致，需为合法 id 字符 */
export function formFieldAnchorId(fieldKey: string): string {
  const safe = String(fieldKey).replace(/[^a-zA-Z0-9_-]/g, '_')
  return `ff-anchor-${safe}`
}

/**
 * uni-forms validate() 失败时 reject 的数组项形如 { key, errorMessage }
 */
export function getFirstInvalidFieldKey(err: unknown): string | null {
  if (!Array.isArray(err)) return null
  for (const item of err) {
    if (item && typeof item === 'object' && 'key' in item) {
      const k = (item as { key: unknown }).key
      if (typeof k === 'string' && k.length > 0 && k !== 'invalid') return k
    }
  }
  return null
}

/** 视口顶部留白（导航栏等），单位 px */
const SCROLL_TOP_OFFSET_PX = 120

/**
 * 将页面滚动到带 #ff-anchor-* 的锚点（适用于非 scroll-view 的页面级滚动）
 */
export function scrollPageToFormFieldKey(fieldKey: string): void {
  const selector = `#${formFieldAnchorId(fieldKey)}`
  nextTick(() => {
    const q = uni.createSelectorQuery()
    q.selectViewport().scrollOffset()
    q.select(selector).boundingClientRect()
    q.exec((res: unknown) => {
      const arr = res as [{ scrollTop?: number } | null, { top?: number } | null] | null
      if (!Array.isArray(arr) || arr.length < 2) return
      const scroll = arr[0]
      const rect = arr[1]
      if (
        !scroll ||
        typeof scroll.scrollTop !== 'number' ||
        !rect ||
        typeof rect.top !== 'number'
      ) {
        return
      }
      const top = scroll.scrollTop + rect.top - SCROLL_TOP_OFFSET_PX
      uni.pageScrollTo({
        scrollTop: Math.max(0, top),
        duration: 240
      })
    })
  })
}

/**
 * uni-forms 校验失败时：滚动到第一个未通过字段
 */
export function scrollToFirstInvalidUniFormField(err: unknown): void {
  const key = getFirstInvalidFieldKey(err)
  if (key) scrollPageToFormFieldKey(key)
}

/**
 * 用于 `scroll-view` 的 `:scroll-into-view`，将视图滚到子节点 `id`（与 formFieldAnchorId 一致）
 */
export function triggerScrollIntoView(scrollIntoViewRef: Ref<string>, fieldKey: string): void {
  scrollIntoViewRef.value = ''
  nextTick(() => {
    scrollIntoViewRef.value = formFieldAnchorId(fieldKey)
  })
}
