/**
 * 建单入口模式：与 jasic-ui 工单列表建单入口一致，供路由 query、列表按钮与弹窗分流。
 *
 * @see jasic-ui `workOrder/index.vue` 建单入口
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */

// 建单入口：代客户填写
export const CREATE_ENTRY_PROXY = 'PROXY_SELF' as const;
// 建单入口：报修一级（二级网点场景）
export const CREATE_ENTRY_UPSTREAM_FIRST = 'UPSTREAM_FIRST' as const;
// 建单入口：报修佳士（一级网点场景）
export const CREATE_ENTRY_UPSTREAM_HQ = 'UPSTREAM_HQ' as const;

export type CreateEntryMode =
  | typeof CREATE_ENTRY_PROXY
  | typeof CREATE_ENTRY_UPSTREAM_FIRST
  | typeof CREATE_ENTRY_UPSTREAM_HQ;

/**
 * 作用：根据当前网点类型编码返回可选建单入口选项。
 * @param currentTypeCode - 当前公司/网点类型编码（如 SITE_FIRST、SITE_SECOND）
 * @returns 下拉选项列表（value + label）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getCreateEntryOptions(currentTypeCode?: string) {
  const options: { value: CreateEntryMode; label: string }[] = [{ value: CREATE_ENTRY_PROXY, label: '代客户填写' }];
  const tc = String(currentTypeCode || '');
  if (tc === 'SITE_SECOND') {
    options.push({ value: CREATE_ENTRY_UPSTREAM_FIRST, label: '报修一级' });
  } else if (tc === 'SITE_FIRST') {
    options.push({ value: CREATE_ENTRY_UPSTREAM_HQ, label: '报修佳士' });
  }
  return options;
}
