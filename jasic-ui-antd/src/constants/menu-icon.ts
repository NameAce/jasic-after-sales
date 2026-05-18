/**
 * 菜单图标：后端 icon 字符串的别名、集合前缀解析，以及按路由名覆盖图标（与 SvgIcon、路由 meta 一致）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export type MenuIconOverride = {
  /** Iconify 名称，例如 `mdi:clipboard-text-outline`
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  icon?: string;
  /** 本地 SVG：`src/assets/svg-icon` 下的文件名（不含后缀）
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  localIcon?: string;
  iconFontSize?: number;
};

/**
 * 接口返回的 icon 字符串别名（若后端存的是旧版图标名、简写等，可映射为 Iconify 全名）。
 * 命中后仍会经过 `VITE_MENU_ICON_API_COLLECTION`（仅当结果中不含 `:` 时补集合前缀）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const MENU_ICON_API_ALIASES: Record<string, string> = {
  // 示例：'user': 'mdi:account-outline',
};

/**
 * 将接口 / meta 中的 icon 转为 Iconify 可用的名称。
 * - 已含 `:` 的视为完整 Iconify id，只做别名表替换（若配置了与整串相等的 key）。
 * - 可在 `.env` 中设置 `VITE_MENU_ICON_API_COLLECTION`（如 `mdi`），对无集合前缀的短名自动补成 `mdi:xxx`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function resolveMenuIconFromApi(raw: string): string {
  const trimmed = String(raw).trim();
  if (!trimmed) return trimmed;

  const aliased = MENU_ICON_API_ALIASES[trimmed] ?? MENU_ICON_API_ALIASES[trimmed.toLowerCase()] ?? trimmed;

  const collection = (import.meta.env.VITE_MENU_ICON_API_COLLECTION || '').trim();
  if (collection && !aliased.includes(':')) {
    return `${collection}:${aliased}`;
  }

  return aliased;
}

/**
 * 侧边栏 / 菜单搜索 图标覆盖：键为路由 `name`，优先级高于接口与 elegant-router 的 meta。
 *
 * 示例：
 * `{ 'work-order': { icon: 'mdi:clipboard-list-outline' }, home: { localIcon: 'activity' } }`
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const MENU_ICON_OVERRIDES: Record<string, MenuIconOverride> = {
  // 在此按需追加，例如：
  // 'work-order': { icon: 'mdi:clipboard-text-outline' },
};
