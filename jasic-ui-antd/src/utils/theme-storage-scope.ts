/**
 * 主题本地存储分区：按「用户 + 当前角色标识集合」拆分主题缓存，避免多账号在同一浏览器共用一套外观配置。
 *
 * 范围 ID 语义：
 * - 未登录：`userId` 视为 `guest`，`roleKeys` 为空；
 * - 已登录：使用后端下发的 `roleKey` 列表（忽略空串），排序后拼接，与用户 ID 一同构成稳定分区键；
 * - 用户信息异步到达时，`roleKeys` 从空变为有值会令范围变化，上层应随之重新 hydrate 主题。
 *
 * @修改人 黄碧莲
 * @修改时间 2026-05-20
 */

import { localStg } from '@/utils/storage';

/** localStorage key 前缀，与 `@/utils/storage` 保持一致，便于同环境多套部署隔离 */
const storagePrefix = import.meta.env.VITE_STORAGE_PREFIX || '';

/**
 * 由用户 ID 与角色 key 列表生成主题存储分区字符串（可读、仅作内部语义，不参与直接作为 DOM / URL）。
 *
 * @param userId - 后端用户 ID（空则按访客处理）
 * @param roleKeys - 后端角色 `roleKey` 数组，需与权限判定口径一致（已去重则更佳）
 * @returns {string} 分区语义串
 */
export function buildThemeStorageScopeId(userId: string, roleKeys: readonly string[]): string {
  const uid = String(userId || '').trim() || 'guest';
  const roles = Array.from(new Set(roleKeys.filter(Boolean))).sort();
  /** 选用 ASCII 分段符，降低与 roleKey 内容冲突的概率 */
  return `${uid}\u001e${roles.join('\u001e')}`;
}

/**
 * 将分区串编码为可作 localStorage key 后缀的片段（避免特殊字符或与前缀拼接歧义）。
 *
 * @param scopeId - 分区语义串
 * @returns {string} Base64 URL 风格片段（无补齐 `=`）
 */
function encodeScopeKeySegment(scopeId: string): string {
  return btoa(unescape(encodeURIComponent(scopeId)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/**
 * 拼接分区化主题项在 localStorage 中的完整物理键。
 *
 * @param baseKey - 逻辑键名，如 `themeSettings`、`themeColor`
 * @param scopeId - {@link buildThemeStorageScopeId} 产物
 * @returns {string} 带环境前缀的最终键名
 */
function scopedPhysicalKey(baseKey: string, scopeId: string): string {
  return `${storagePrefix}${baseKey}__scope__${encodeScopeKeySegment(scopeId)}`;
}

/**
 * 读取当前分区是否已对某构建版本执行过「发版默认主题」叠加（生产环境与全局 BUILD_TIME 配合）。
 *
 * @param scopeId - 当前分区
 * @returns {string | null} 已记录的构建时间标记
 */
export function readScopedPublishOverrideFlag(scopeId: string): string | null {
  const raw = localStorage.getItem(scopedPhysicalKey('themePublishOverrideFlag', scopeId));
  if (!raw) return null;
  try {
    return JSON.parse(raw) as string;
  } catch {
    return null;
  }
}

/**
 * 标记当前分区已完成对给定构建版本的默认主题叠加，避免在同一会话切换身份时不应重复套用。
 *
 * @param scopeId - 当前分区
 * @param buildTime - 通常为 Vite 注入的 BUILD_TIME
 * @returns {void}
 */
export function writeScopedPublishOverrideFlag(scopeId: string, buildTime: string): void {
  localStorage.setItem(
    scopedPhysicalKey('themePublishOverrideFlag', scopeId),
    JSON.stringify(buildTime)
  );
}

/**
 * 读取分区下的主题完整配置 JSON；无数据时返回 null。
 *
 * @param scopeId - 当前分区
 * @returns {App.Theme.ThemeSetting | null} 反序列化结果
 */
export function readScopedThemeSettings(scopeId: string): App.Theme.ThemeSetting | null {
  const raw = localStorage.getItem(scopedPhysicalKey('themeSettings', scopeId));
  if (!raw) return null;
  try {
    return JSON.parse(raw) as App.Theme.ThemeSetting;
  } catch {
    return null;
  }
}

/**
 * 将主题完整配置写入当前分区（覆盖写入）。
 *
 * @param scopeId - 当前分区
 * @param value - 主题配置对象
 * @returns {void}
 */
export function writeScopedThemeSettings(scopeId: string, value: App.Theme.ThemeSetting): void {
  localStorage.setItem(scopedPhysicalKey('themeSettings', scopeId), JSON.stringify(value));
}

/**
 * 读取分区下的主色字符串；与 `localStg` 写入格式一致使用 JSON.stringify 包裹，便于与历史键解析方式统一。
 *
 * @param scopeId - 当前分区
 * @returns {string | null} 主色 hex 等
 */
export function readScopedThemeColor(scopeId: string): string | null {
  const raw = localStorage.getItem(scopedPhysicalKey('themeColor', scopeId));
  if (!raw) return null;
  try {
    return JSON.parse(raw) as string;
  } catch {
    return null;
  }
}

/**
 * 分区写入主色，供首屏 loading 与主题 store 同步（另保留一份非分区 `themeColor` 供挂载前插件读取，见主题 store）。
 *
 * @param scopeId - 当前分区
 * @param color - 主色
 * @returns {void}
 */
export function writeScopedThemeColor(scopeId: string, color: string): void {
  localStorage.setItem(scopedPhysicalKey('themeColor', scopeId), JSON.stringify(color));
}

/**
 * 读取「无角色维度」旧版主题配置，仅用于升级兼容；新数据以分区键为准。
 *
 * @returns {App.Theme.ThemeSetting | null} 旧版缓存
 */
export function readLegacyThemeSettings(): App.Theme.ThemeSetting | null {
  return localStg.get('themeSettings');
}

/**
 * 读取旧版全局主色（供兼容 `setupLoading` 等在 Pinia 就绪前读取的场景）。
 *
 * @returns {string | null} 主色
 */
export function readLegacyThemeColor(): string | null {
  return localStg.get('themeColor');
}

/** 与 `theme/settings.ts` 中 `themeColor` 默认主色一致 */
const DEFAULT_THEME_COLOR = '#646cff';

/**
 * 记录最近一次生效的主题分区，供 Pinia 未就绪时的首屏 loading 按「上次登录角色」取主色。
 *
 * @param scopeId - {@link buildThemeStorageScopeId} 产物
 * @returns {void}
 */
export function persistActiveThemeScopeId(scopeId: string): void {
  localStg.set('themeActiveScopeId', scopeId);
}

/**
 * 读取最近一次生效的主题分区标识。
 *
 * @returns {string | null} 分区语义串
 */
export function readActiveThemeScopeId(): string | null {
  const raw = localStg.get('themeActiveScopeId');
  return raw ? String(raw) : null;
}

/**
 * 清除指定分区下的主题缓存（重置为默认前调用）。
 *
 * @param scopeId - 当前分区
 * @returns {void}
 */
export function clearScopedThemeCache(scopeId: string): void {
  localStorage.removeItem(scopedPhysicalKey('themeSettings', scopeId));
  localStorage.removeItem(scopedPhysicalKey('themeColor', scopeId));
  localStorage.removeItem(scopedPhysicalKey('themePublishOverrideFlag', scopeId));
}

/**
 * 将旧版全局主题快照一次性迁入当前分区（分区无数据且存在 legacy 时执行，避免升级后丢配置）。
 *
 * @param scopeId - 当前分区
 * @returns {void}
 */
export function migrateLegacyThemeToScoped(scopeId: string): void {
  if (readScopedThemeSettings(scopeId)) return;

  const legacy = readLegacyThemeSettings();
  if (!legacy) return;

  writeScopedThemeSettings(scopeId, legacy);
  const legacyColor = readLegacyThemeColor();
  if (legacyColor) {
    writeScopedThemeColor(scopeId, legacyColor);
  }
}

/**
 * 解析首屏 loading 使用的主色：优先「上次登录用户+角色」分区，其次旧版全局键，最后项目默认色。
 *
 * @returns {string} 主色 hex
 */
export function resolveThemeColorForBoot(): string {
  const activeScopeId = readActiveThemeScopeId();
  if (activeScopeId) {
    const scopedColor = readScopedThemeColor(activeScopeId);
    if (scopedColor) return scopedColor;

    const scopedSettings = readScopedThemeSettings(activeScopeId);
    if (scopedSettings?.themeColor) return scopedSettings.themeColor;
  }
  return readLegacyThemeColor() || DEFAULT_THEME_COLOR;
}
