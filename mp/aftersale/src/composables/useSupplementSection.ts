import { ref } from 'vue'

/**
 * 补充说明区块折叠（默认收起）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useSupplementSection(initialOpen = false) {
  const showSupplementSection = ref(initialOpen)

  const toggleSupplementSection = () => {
    showSupplementSection.value = !showSupplementSection.value
  }

  return { showSupplementSection, toggleSupplementSection }
}
