/**
 * 与侧栏菜单 `getGlobalMenuByBaseRoute` 一致的展示标题：后端菜单在关闭 VITE_BACKEND_MENU_USE_I18N 时使用 meta.title。
 */
import type { RouteLocationNormalizedLoaded } from 'vue-router';
import { $t, getLocale } from '@/locales';

const useBackendMenuI18n = import.meta.env.VITE_BACKEND_MENU_USE_I18N === 'Y';

export function getRouteMenuTitle(route: Pick<RouteLocationNormalizedLoaded, 'meta' | 'name'>): string {
  const meta = route.meta ?? {};
  const i18nKey = meta.i18nKey as App.I18n.I18nKey | null | undefined;
  const title = meta.title != null && String(meta.title) !== '' ? String(meta.title) : '';
  const name = route.name != null ? String(route.name) : '';
  const fallbackLabel = (title || name) as string;
  let normalizedFallbackLabel = fallbackLabel;
  if (name === 'home' && fallbackLabel.trim().toLowerCase() === 'home') {
    normalizedFallbackLabel = String(getLocale()).toLowerCase().startsWith('zh') ? '首页' : 'home';
  }
  if (i18nKey && useBackendMenuI18n) {
    return String($t(i18nKey));
  }
  return normalizedFallbackLabel;
}
