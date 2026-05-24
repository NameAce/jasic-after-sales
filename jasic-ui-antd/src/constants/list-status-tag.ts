/**
 * 列表中 ATag 语义色：与 Ant Design Tag 预设色一致，封装启用/停用、布尔正反、工单主状态等映射，全站列表复用。
 *
 * @see https://ant.design/components/tag-cn
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */

/** 启用 / 正常 / 有效 — 与「停用 / 失效」成对出现时用红绿对比
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function tagColorEnabled(enabled: boolean | number): 'success' | 'error' {
  const on = enabled === true || Number(enabled) === 1;
  return on ? 'success' : 'error';
}

/** 正面「是」— success；反面用中性灰，不用红色（如：是否默认）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function tagColorPositiveNeutral(yes: boolean | number): 'success' | 'default' {
  const y = yes === true || Number(yes) === 1;
  return y ? 'success' : 'default';
}

/** 是否转单列：已转单与主状态列区分（cyan / 默认灰）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function tagColorTransferTransferred(transferred: boolean | number): 'cyan' | 'default' {
  const t = transferred === true || Number(transferred) === 1;
  return t ? 'cyan' : 'default';
}

/** 工单主状态 → Tag color
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
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

/** 常见系统角色 role_key → Tag 色（同一角色全站列表颜色一致）
 * @修改人 黄碧莲
 * @修改时间 2026-05-24
 */
const USER_ROLE_KEY_TAG_COLORS: Record<string, string> = {
  platform_admin: 'purple',
  admin: 'warning',
  'js-admin': 'orange',
  repairer: 'green',
  dispatcher: 'cyan'
};

/** 未命中预设 role_key 时按标识哈希分配的 Tag 色板（相邻角色尽量不同色）
 * @修改人 黄碧莲
 * @修改时间 2026-05-24
 */
const USER_ROLE_TAG_PALETTE = ['success', 'magenta', 'geekblue', 'volcano', 'green', 'gold', 'blue', 'error'] as const;

/**
 * 用户列表「角色」列 Tag 颜色：优先按 role_key 预设，否则按 role_key 哈希取色，保证同名角色颜色稳定。
 * @param roleKeyOrSeed - 角色 role_key；缺失时可传角色 id 或名称作为兜底种子
 * @修改人 黄碧莲
 * @修改时间 2026-05-24
 */
export function userRoleTagColor(roleKeyOrSeed: string | number | undefined): string {
  const key = String(roleKeyOrSeed ?? '')
    .trim()
    .toLowerCase();
  if (!key) return 'default';
  const preset = USER_ROLE_KEY_TAG_COLORS[key];
  if (preset) return preset;
  let hash = 0;
  for (let i = 0; i < key.length; i += 1) {
    // 角色 key 较短，用 imul 模拟 32 位累加即可，避免位运算触发 no-bitwise
    hash = Math.imul(31, hash) + key.charCodeAt(i);
  }
  return USER_ROLE_TAG_PALETTE[Math.abs(hash) % USER_ROLE_TAG_PALETTE.length];
}
