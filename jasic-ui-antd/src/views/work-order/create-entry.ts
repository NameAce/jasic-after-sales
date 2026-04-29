/** 与 jasic-ui `workOrder/index.vue` 建单入口一致 */
export const CREATE_ENTRY_PROXY = 'PROXY_SELF' as const;
export const CREATE_ENTRY_UPSTREAM_FIRST = 'UPSTREAM_FIRST' as const;
export const CREATE_ENTRY_UPSTREAM_HQ = 'UPSTREAM_HQ' as const;

export type CreateEntryMode =
  | typeof CREATE_ENTRY_PROXY
  | typeof CREATE_ENTRY_UPSTREAM_FIRST
  | typeof CREATE_ENTRY_UPSTREAM_HQ;

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
