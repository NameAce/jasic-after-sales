/**
 * 列表中 ATag 语义色：与 Ant Design Tag 预设色一致，封装启用/停用、布尔正反、工单主状态等映射，全站列表复用。
 *
 * @see https://ant.design/components/tag-cn
 */

/** 启用 / 正常 / 有效 — 与「停用 / 失效」成对出现时用红绿对比 */
export function tagColorEnabled(enabled: boolean | number): 'success' | 'error' {
  const on = enabled === true || Number(enabled) === 1;
  return on ? 'success' : 'error';
}

/** 正面「是」— success；反面用中性灰，不用红色（如：是否默认） */
export function tagColorPositiveNeutral(yes: boolean | number): 'success' | 'default' {
  const y = yes === true || Number(yes) === 1;
  return y ? 'success' : 'default';
}

/** 是否转单列：已转单与主状态列区分（cyan / 默认灰） */
export function tagColorTransferTransferred(transferred: boolean | number): 'cyan' | 'default' {
  const t = transferred === true || Number(transferred) === 1;
  return t ? 'cyan' : 'default';
}

/** 工单主状态 → Tag color */
export function workOrderMainStatusTagColor(status: string | undefined): string {
  const s = String(status || '');
  const map: Record<string, string> = {
    PENDING_ASSIGN: 'warning',
    PENDING_TECH_ACCEPT: 'processing',
    IN_PROGRESS: 'processing',
    COMPLETED: 'success',
    CLOSED: 'default'
  };
  return map[s] || 'default';
}
