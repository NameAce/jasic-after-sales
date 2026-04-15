import { nextTick, type Ref } from 'vue'

/**
 * 配合 scroll-view 的 scroll-into-view：先清空再指向子节点 id（与 FormItemAnchor 的 name 一致）
 */
export function triggerScrollIntoView(scrollIntoView: Ref<string>, fieldKey: string): void {
  scrollIntoView.value = ''
  nextTick(() => {
    scrollIntoView.value = fieldKey
  })
}
