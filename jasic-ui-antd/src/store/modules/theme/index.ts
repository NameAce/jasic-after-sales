/**
 * 主题与外观：布局/色板/水印设置、antd 主题 token、暗色与灰度等辅助模式及本地持久化。
 * @修改人 黄碧莲
 * @修改时间 2026-05-20
 */
import { computed, effectScope, onScopeDispose, reactive, toRefs, watch } from 'vue';
import { useEventListener, usePreferredColorScheme } from '@vueuse/core';
import { defineStore } from 'pinia';
import { getPaletteColorByNumber } from '@sa/color';
import { localStg } from '@/utils/storage';
import {
  buildThemeStorageScopeId,
  clearScopedThemeCache,
  migrateLegacyThemeToScoped,
  persistActiveThemeScopeId,
  writeScopedThemeColor,
  writeScopedThemeSettings
} from '@/utils/theme-storage-scope';
import { themeSettings as defaultThemeSettings } from '@/theme/settings';
import { SetupStoreId } from '@/enum';
import { useAuthStore } from '../auth';
import {
  addThemeVarsToGlobal,
  createThemeToken,
  getAntdTheme,
  resolveThemeSettingsForScope,
  toggleAuxiliaryColorModes,
  toggleCssDarkMode
} from './shared';

export const useThemeStore = defineStore(SetupStoreId.Theme, () => {
  const authStore = useAuthStore();
  // 用于集中管理 watch，store 卸载时停止
  const scope = effectScope();
  // 操作系统偏好配色（用于 themeScheme 为 auto 时）
  const osTheme = usePreferredColorScheme();

  /**
   * 与鉴权用户信息联动的存储分区：
   * - `userId` 区分账号；
   * - `roleKeys`（与路由权限同源）区分同一账号下登录角色集合；
   * - 用户信息异步到达时 roleKeys 从空变为有值会触发重新 hydrate。
   */
  const themeStorageScopeId = computed(() => {
    const uid = authStore.userInfo?.userId ?? '';
    const roles = authStore.roleKeys;
    const roleList = Array.isArray(roles) ? roles : [];
    return buildThemeStorageScopeId(uid, roleList);
  });

  /**
   * 从本地分区合并主题并写回内存；使用 reactive + Object.assign，保证切换角色后 toRefs 仍响应。
   *
   * @param scopeId - 当前存储分区
   * @returns {void}
   */
  function hydrateThemeSettings(scopeId: string) {
    migrateLegacyThemeToScoped(scopeId);
    const next = resolveThemeSettingsForScope(scopeId);
    Object.assign(settings, next);
    persistActiveThemeScopeId(scopeId);
  }

  const initialScopeId = themeStorageScopeId.value;
  migrateLegacyThemeToScoped(initialScopeId);

  // 当前主题设置（布局、色板、水印等）；首次按当前分区 hydrate，登录角色变化时由 watch 再拉取
  const settings = reactive<App.Theme.ThemeSetting>(resolveThemeSettingsForScope(initialScopeId));
  persistActiveThemeScopeId(initialScopeId);

  /**
   * 将当前身份下的主题恢复为项目默认（清除该用户+角色分区的本地缓存）。
   *
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-20
   */
  function resetStore() {
    const scopeId = themeStorageScopeId.value;
    clearScopedThemeCache(scopeId);
    Object.assign(settings, defaultThemeSettings);
    cacheThemeSettings();
  }

  // 派生出的主题色板（主色、功能色、info 是否跟随主色）
  const themeColors = computed(() => {
    const { themeColor, otherColor, isInfoFollowPrimary } = settings;
    const colors: App.Theme.ThemeColor = {
      primary: themeColor,
      ...otherColor,
      info: isInfoFollowPrimary ? themeColor : otherColor.info
    };
    return colors;
  });

  // 是否处于暗色界面（含跟随系统 auto）
  const darkMode = computed(() => {
    if (settings.themeScheme === 'auto') {
      return osTheme.value === 'dark';
    }
    return settings.themeScheme === 'dark';
  });

  // 是否开启灰度模式
  const grayscaleMode = computed(() => settings.grayscale);

  // 是否开启色弱模式
  const colourWeaknessMode = computed(() => settings.colourWeakness);

  // 供 Ant Design Vue ConfigProvider 使用的主题 token
  const antdTheme = computed(() => getAntdTheme(themeColors.value, darkMode.value));

  // 当前主题设置的 JSON 字符串（用于复制导出等）
  const settingsJson = computed(() => JSON.stringify(settings));

  /**
   * 设置浅色 / 深色 / 跟随系统 的主题方案。
   *
   * @param themeScheme - 主题方案枚举
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function setThemeScheme(themeScheme: UnionKey.ThemeScheme) {
    settings.themeScheme = themeScheme;
  }

  /**
   * 开启或关闭灰度显示。
   *
   * @param isGrayscale - 是否灰度
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function setGrayscale(isGrayscale: boolean) {
    settings.grayscale = isGrayscale;
  }

  /**
   * 开启或关闭色弱辅助显示。
   *
   * @param isColourWeakness - 是否色弱模式
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function setColourWeakness(isColourWeakness: boolean) {
    settings.colourWeakness = isColourWeakness;
  }

  /**
   * 在 light → dark → auto 之间循环切换主题方案。
   *
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function toggleThemeScheme() {
    const themeSchemes: UnionKey.ThemeScheme[] = ['light', 'dark', 'auto'];

    const index = themeSchemes.findIndex(item => item === settings.themeScheme);

    const nextIndex = index === themeSchemes.length - 1 ? 0 : index + 1;

    const nextThemeScheme = themeSchemes[nextIndex];

    setThemeScheme(nextThemeScheme);
  }

  /**
   * 设置整体布局模式（如纵向、混合等）。
   *
   * @param mode - 布局模式
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function setThemeLayout(mode: UnionKey.ThemeLayoutMode) {
    settings.layout.mode = mode;
  }

  /**
   * 更新主题色；开启推荐色时会从色板取规范色值。
   *
   * @param key - 颜色键（如 primary、success）
   * @param color - 用户选择的颜色值
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function updateThemeColors(key: App.Theme.ThemeColorKey, color: string) {
    let colorValue = color;

    if (settings.recommendColor) {
      colorValue = getPaletteColorByNumber(color, 500, true);
    }

    if (key === 'primary') {
      settings.themeColor = colorValue;
    } else {
      settings.otherColor[key] = colorValue;
    }
  }

  /**
   * 将主题 CSS 变量写入文档根节点，供全局样式使用。
   *
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function setupThemeVarsToGlobal() {
    const { themeTokens, darkThemeTokens } = createThemeToken(
      themeColors.value,
      settings.tokens,
      settings.recommendColor
    );
    addThemeVarsToGlobal(themeTokens, darkThemeTokens);
  }

  /**
   * 设置横向混合布局时一级菜单是否反向排列。
   *
   * @param reverse - 是否反向
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-14
   */
  function setLayoutReverseHorizontalMix(reverse: boolean) {
    settings.layout.reverseHorizontalMix = reverse;
  }

  /**
   * 将当前完整主题设置写入本地存储（按当前登录用户+角色分区）。
   *
   * @returns {void} 无返回值
   * @修改人 黄碧莲
   * @修改时间 2026-05-20
   */
  function cacheThemeSettings() {
    const scopeId = themeStorageScopeId.value;
    writeScopedThemeSettings(scopeId, JSON.parse(JSON.stringify(settings)) as App.Theme.ThemeSetting);
    persistActiveThemeScopeId(scopeId);
  }

  // 关闭或刷新页面前再次写入，避免极少数环境下 watch 未及时落盘
  useEventListener(window, 'beforeunload', () => {
    cacheThemeSettings();
  });

  scope.run(() => {
    // 登录态 / 角色集合变化时切换分区，从本地重新合并该身份下的主题偏好
    watch(themeStorageScopeId, scopeId => {
      hydrateThemeSettings(scopeId);
    });

    // 任意主题项变更时立即持久化到当前角色分区
    watch(settings, cacheThemeSettings, { deep: true });

    // 暗色开关变化时同步 html 上的 dark class
    watch(
      darkMode,
      val => {
        toggleCssDarkMode(val);
      },
      { immediate: true }
    );

    // 灰度、色弱变化时更新 body 上的辅助 class
    watch(
      [grayscaleMode, colourWeaknessMode],
      val => {
        toggleAuxiliaryColorModes(val[0], val[1]);
      },
      { immediate: true }
    );

    // 主题色变化时刷新 CSS 变量并缓存主色（分区 + 全局各写一份：全局供首屏 loading 等 Pinia 就绪前读取）
    watch(
      themeColors,
      val => {
        setupThemeVarsToGlobal();
        const sid = themeStorageScopeId.value;
        writeScopedThemeColor(sid, val.primary);
        localStg.set('themeColor', val.primary);
      },
      { immediate: true }
    );
  });

  // 卸载时停止 theme 相关 watch
  onScopeDispose(() => {
    scope.stop();
  });

  return {
    ...toRefs(settings),
    darkMode,
    themeColors,
    antdTheme,
    settingsJson,
    setGrayscale,
    setColourWeakness,
    resetStore,
    toggleThemeScheme,
    setThemeScheme,
    updateThemeColors,
    setThemeLayout,
    setLayoutReverseHorizontalMix
  };
});
