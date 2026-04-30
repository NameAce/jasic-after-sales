/**
 * 主题与外观：布局/色板/水印设置、antd 主题 token、暗色与灰度等辅助模式及本地持久化。
 */
import { computed, effectScope, onScopeDispose, ref, toRefs, watch } from 'vue';
import type { Ref } from 'vue';
import { useEventListener, usePreferredColorScheme } from '@vueuse/core';
import { defineStore } from 'pinia';
import { getPaletteColorByNumber } from '@sa/color';
import { localStg } from '@/utils/storage';
import { SetupStoreId } from '@/enum';
import {
  addThemeVarsToGlobal,
  createThemeToken,
  getAntdTheme,
  initThemeSettings,
  toggleAuxiliaryColorModes,
  toggleCssDarkMode
} from './shared';

export const useThemeStore = defineStore(SetupStoreId.Theme, () => {
  // 用于集中管理 watch，store 卸载时停止
  const scope = effectScope();
  // 操作系统偏好配色（用于 themeScheme 为 auto 时）
  const osTheme = usePreferredColorScheme();

  // 当前主题设置（布局、色板、水印等）
  const settings: Ref<App.Theme.ThemeSetting> = ref(initThemeSettings());

  /**
   * 将主题 store 重置为初始状态。
   *
   * @returns {void} 无返回值
   */
  function resetStore() {
    const themeStore = useThemeStore();

    themeStore.$reset();
  }

  // 派生出的主题色板（主色、功能色、info 是否跟随主色）
  const themeColors = computed(() => {
    const { themeColor, otherColor, isInfoFollowPrimary } = settings.value;
    const colors: App.Theme.ThemeColor = {
      primary: themeColor,
      ...otherColor,
      info: isInfoFollowPrimary ? themeColor : otherColor.info
    };
    return colors;
  });

  // 是否处于暗色界面（含跟随系统 auto）
  const darkMode = computed(() => {
    if (settings.value.themeScheme === 'auto') {
      return osTheme.value === 'dark';
    }
    return settings.value.themeScheme === 'dark';
  });

  // 是否开启灰度模式
  const grayscaleMode = computed(() => settings.value.grayscale);

  // 是否开启色弱模式
  const colourWeaknessMode = computed(() => settings.value.colourWeakness);

  // 供 Ant Design Vue ConfigProvider 使用的主题 token
  const antdTheme = computed(() => getAntdTheme(themeColors.value, darkMode.value));

  // 当前主题设置的 JSON 字符串（用于复制导出等）
  const settingsJson = computed(() => JSON.stringify(settings.value));

  /**
   * 设置浅色 / 深色 / 跟随系统 的主题方案。
   *
   * @param themeScheme - 主题方案枚举
   * @returns {void} 无返回值
   */
  function setThemeScheme(themeScheme: UnionKey.ThemeScheme) {
    settings.value.themeScheme = themeScheme;
  }

  /**
   * 开启或关闭灰度显示。
   *
   * @param isGrayscale - 是否灰度
   * @returns {void} 无返回值
   */
  function setGrayscale(isGrayscale: boolean) {
    settings.value.grayscale = isGrayscale;
  }

  /**
   * 开启或关闭色弱辅助显示。
   *
   * @param isColourWeakness - 是否色弱模式
   * @returns {void} 无返回值
   */
  function setColourWeakness(isColourWeakness: boolean) {
    settings.value.colourWeakness = isColourWeakness;
  }

  /**
   * 在 light → dark → auto 之间循环切换主题方案。
   *
   * @returns {void} 无返回值
   */
  function toggleThemeScheme() {
    const themeSchemes: UnionKey.ThemeScheme[] = ['light', 'dark', 'auto'];

    const index = themeSchemes.findIndex(item => item === settings.value.themeScheme);

    const nextIndex = index === themeSchemes.length - 1 ? 0 : index + 1;

    const nextThemeScheme = themeSchemes[nextIndex];

    setThemeScheme(nextThemeScheme);
  }

  /**
   * 设置整体布局模式（如纵向、混合等）。
   *
   * @param mode - 布局模式
   * @returns {void} 无返回值
   */
  function setThemeLayout(mode: UnionKey.ThemeLayoutMode) {
    settings.value.layout.mode = mode;
  }

  /**
   * 更新主题色；开启推荐色时会从色板取规范色值。
   *
   * @param key - 颜色键（如 primary、success）
   * @param color - 用户选择的颜色值
   * @returns {void} 无返回值
   */
  function updateThemeColors(key: App.Theme.ThemeColorKey, color: string) {
    let colorValue = color;

    if (settings.value.recommendColor) {
      // get a color palette by provided color and color name, and use the suitable color

      colorValue = getPaletteColorByNumber(color, 500, true);
    }

    if (key === 'primary') {
      settings.value.themeColor = colorValue;
    } else {
      settings.value.otherColor[key] = colorValue;
    }
  }

  /**
   * 将主题 CSS 变量写入文档根节点，供全局样式使用。
   *
   * @returns {void} 无返回值
   */
  function setupThemeVarsToGlobal() {
    const { themeTokens, darkThemeTokens } = createThemeToken(
      themeColors.value,
      settings.value.tokens,
      settings.value.recommendColor
    );
    addThemeVarsToGlobal(themeTokens, darkThemeTokens);
  }

  /**
   * 设置横向混合布局时一级菜单是否反向排列。
   *
   * @param reverse - 是否反向
   * @returns {void} 无返回值
   */
  function setLayoutReverseHorizontalMix(reverse: boolean) {
    settings.value.layout.reverseHorizontalMix = reverse;
  }

  /**
   * 生产环境下将当前主题设置持久化到本地存储。
   *
   * @returns {void} 无返回值
   */
  function cacheThemeSettings() {
    const isProd = import.meta.env.PROD;

    if (!isProd) return;

    localStg.set('themeSettings', settings.value);
  }

  // 关闭或刷新页面前缓存主题
  useEventListener(window, 'beforeunload', () => {
    cacheThemeSettings();
  });

  scope.run(() => {
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

    // 主题色变化时刷新 CSS 变量并缓存主色
    watch(
      themeColors,
      val => {
        setupThemeVarsToGlobal();
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
    ...toRefs(settings.value),
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
