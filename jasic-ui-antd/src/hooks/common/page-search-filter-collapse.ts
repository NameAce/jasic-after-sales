/**
 * 列表页搜索区筛选项折叠：默认仅展示前 4 项；多于 4 项时通过 `showSearchFilterExpandToggle` 显示展开按钮，展开后显示全部。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { type MaybeRef, computed, reactive, ref, unref } from 'vue';

/** 默认展示的筛选项个数（第 5 项起可折叠隐藏）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const PAGE_SEARCH_FILTER_MAX_VISIBLE = 4;

/**
 * @param filterItemCount - 与筛选项一一对应的数量（通常等于 `ACol` 个数，或行内 `AFormItem` 中可折叠项个数）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function usePageSearchFilterCollapse(filterItemCount: MaybeRef<number>) {
  const expanded = ref(false);

  const total = computed(() => unref(filterItemCount));

  /** 仅当筛选项个数大于 4 时为 true，用于控制展开按钮是否渲染
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  const showExpandToggle = computed(() => total.value > PAGE_SEARCH_FILTER_MAX_VISIBLE);

  function toggleExpanded() {
    expanded.value = !expanded.value;
  }

  function isFilterHidden(index: number) {
    return !expanded.value && index >= PAGE_SEARCH_FILTER_MAX_VISIBLE;
  }

  return reactive({
    searchFilterExpanded: expanded,
    showSearchFilterExpandToggle: showExpandToggle,
    toggleSearchFilterExpand: toggleExpanded,
    isSearchFilterHidden: isFilterHidden
  });
}
