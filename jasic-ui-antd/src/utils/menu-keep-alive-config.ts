/**
 * 菜单页面缓存配置（纯前端，不改后端表结构）。
 * - 持久化：写入 sys_menu.remark 内嵌标记 `[keepAlive:1|0]`（经现有菜单保存接口落库）
 * - 运行时：localStorage 按菜单 id 缓存，供 /auth/menus 路由树（VO 无 remark）读取
 * @修改人 黄碧莲
 * @修改时间 2026-05-27
 */

/** remark 内嵌标记，与业务备注共存 */
const KEEP_ALIVE_REMARK_PATTERN = /\[keepAlive:([01])\]/;

const MENU_KEEP_ALIVE_STORAGE_KEY = 'jasic-menu-keep-alive-v1';

type KeepAliveStorageMap = Record<string, boolean>;

/**
 * 从菜单 remark 解析是否开启页面缓存。
 *
 * @param remark - 菜单备注原文
 * @returns true/false；无标记时返回 undefined
 */
export function parseKeepAliveFromRemark(remark?: string | null): boolean | undefined {
  const text = String(remark ?? '');
  const match = text.match(KEEP_ALIVE_REMARK_PATTERN);
  if (!match) return undefined;
  return match[1] === '1';
}

/**
 * 去掉 remark 中的缓存标记，供界面展示其它备注内容。
 *
 * @param remark - 菜单备注原文
 * @returns 去除标记后的备注
 */
export function stripKeepAliveRemarkMarker(remark?: string | null): string {
  return String(remark ?? '')
    .replace(KEEP_ALIVE_REMARK_PATTERN, '')
    .replace(/\n{2,}/g, '\n')
    .trim();
}

/**
 * 将页面缓存开关合并进 remark（保留原有备注文本）。
 *
 * @param remark - 用户备注（不含标记）
 * @param enabled - 是否缓存
 * @returns 写入库的 remark
 */
export function mergeKeepAliveIntoRemark(remark: string, enabled: boolean): string {
  const base = stripKeepAliveRemarkMarker(remark);
  const marker = `[keepAlive:${enabled ? 1 : 0}]`;
  if (!base) return marker;
  return `${base}\n${marker}`;
}

function readStorageMap(): KeepAliveStorageMap {
  try {
    const raw = localStorage.getItem(MENU_KEEP_ALIVE_STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as KeepAliveStorageMap;
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function writeStorageMap(map: KeepAliveStorageMap) {
  localStorage.setItem(MENU_KEEP_ALIVE_STORAGE_KEY, JSON.stringify(map));
}

/**
 * 从 localStorage 读取菜单是否缓存（/auth/menus 无 remark 时的运行时兜底）。
 *
 * @param menuId - 菜单主键
 * @returns 是否缓存；未配置返回 undefined
 */
export function getMenuKeepAliveFromStorage(menuId?: string | number | null): boolean | undefined {
  if (menuId === undefined || menuId === null || menuId === '') return undefined;
  const map = readStorageMap();
  const key = String(menuId);
  if (!Object.prototype.hasOwnProperty.call(map, key)) return undefined;
  return Boolean(map[key]);
}

/**
 * 保存菜单页面缓存配置到 localStorage，登录后动态路由可立即生效。
 *
 * @param menuId - 菜单主键
 * @param enabled - 是否缓存
 */
export function setMenuKeepAliveToStorage(menuId: string | number, enabled: boolean) {
  const map = readStorageMap();
  map[String(menuId)] = enabled;
  writeStorageMap(map);
}

/**
 * 综合 remark 与 localStorage 解析菜单是否缓存。
 *
 * @param options.menuId - 菜单 id
 * @param options.remark - 菜单 remark
 * @returns 是否缓存；均未配置时 undefined
 */
export function resolveMenuKeepAliveFlag(options: {
  menuId?: string | number | null;
  remark?: string | null;
}): boolean | undefined {
  const fromRemark = parseKeepAliveFromRemark(options.remark);
  if (fromRemark !== undefined) return fromRemark;
  return getMenuKeepAliveFromStorage(options.menuId);
}
