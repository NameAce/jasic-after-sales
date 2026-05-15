/**
 * 国际化入口（中文单语）：`$t` 从 `zh-cn` 表取值并支持 `{key}` 插值；`getLocale`/`setLocale` 为兼容占位。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { App } from 'vue';
import zhCN from './langs/zh-cn';

type InterpolationParams = Record<string, unknown>;

/**
 * 作用：按点分路径从对象上安全取值。
 * @param obj 任意对象
 * @param path 如 `a.b.c`
 * @returns {unknown} 路径上的值，不存在为 undefined
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function getByPath(obj: unknown, path: string): unknown {
  return path.split('.').reduce<unknown>((acc, seg) => {
    if (acc === null || acc === undefined) return undefined;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const anyAcc = acc as any;
    return anyAcc[seg];
  }, obj);
}

/**
 * 作用：将模板中 `{key}` 替换为参数对象对应值。
 * @param template 文案模板
 * @param params 插值参数
 * @returns {string} 替换后的字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function interpolate(template: string, params?: InterpolationParams): string {
  if (!params) return template;

  return template.replace(/\{(\w+)\}/g, (_match, key: string) => {
    const val = params[key];
    if (val === null || val === undefined) return '';
    return String(val);
  });
}

/**
 * 中文-only 文案查找函数（替代 vue-i18n）
 *
 * 仅保留“从 key 取中文文案 + 支持 `{xxx}` 变量插值”的能力，不做任何语言切换。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const $t = ((key: App.I18n.I18nKey, ...args: unknown[]) => {
  // vue-i18n signature 里支持很多重载，这里做“能跑”的最小兼容：
  // 1) $t(key)
  // 2) $t(key, { xxx })
  // 3) $t(key, defaultMsg, { xxx })  （少量场景可能用）
  // 4) $t(key, plural, options) / $t(key, list, options)（本项目当前不需要真正 plural，只取字符串）
  const raw = getByPath(zhCN, key as string);

  const namedParams = args.at(-1);
  const params =
    namedParams && typeof namedParams === 'object' && !Array.isArray(namedParams)
      ? (namedParams as InterpolationParams)
      : undefined;

  // defaultMsg 作为第一个 string 参数
  const defaultMsg = args.find(item => typeof item === 'string') as string | undefined;

  if (typeof raw === 'string') return interpolate(raw, params);
  if (typeof defaultMsg === 'string') return interpolate(defaultMsg, params);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return String(key as any);
}) as App.I18n.$T;

// 保留导出以兼容历史引用；当前不做语言切换
/**
 * 作用：返回固定语言标识（本项目仅中文）。
 * @returns {'zh-CN'}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getLocale() {
  return 'zh-CN' as App.I18n.LangType;
}

/**
 * 作用：兼容历史 API；当前为多语言占位，不执行切换。
 * @param _locale 语言类型（忽略）
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function setLocale(_locale: App.I18n.LangType) {
  // no-op
}
