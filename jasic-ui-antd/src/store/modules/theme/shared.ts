/**
 * 主题 store 共享：默认设置合并、antd ConfigProvider token、CSS 变量与暗色 class 切换等。
 * @修改人 黄碧莲
 * @修改时间 2026-05-20
 */
import { theme as antdTheme } from 'ant-design-vue';
import type { ConfigProviderProps } from 'ant-design-vue';
import { getColorPalette } from '@sa/color';
import { getRgbOfColor } from '@sa/utils';
import { defu } from 'defu';
import { toggleHtmlClass } from '@/utils/common';
import { localStg } from '@/utils/storage';
import { readLegacyThemeSettings, readScopedPublishOverrideFlag, readScopedThemeSettings, writeScopedPublishOverrideFlag } from '@/utils/theme-storage-scope';
import { overrideThemeSettings, themeSettings } from '@/theme/settings';
import { themeVars } from '@/theme/vars';

const DARK_CLASS = 'dark';

/**
 * 作用：按「用户 + 当前角色维度」读取并合并本地缓存的主题设置（分区键缺失时降级旧版全局 `themeSettings`，便于升级平滑）。
 *
 * - 开发与生产均在存在缓存时与 `themeSettings` 默认值合并；
 * - 生产环境按存储分区写入 `themePublishOverrideFlag`，在构建版本 BUILD_TIME 变化时对**该分区**叠加 `overrideThemeSettings`；
 * - 清空某分区可自行删除 localStorage 中 `themeSettings__scope__*` 与 `themePublishOverrideFlag__scope__*` 条目。
 *
 * @param scopeId - 由 {@link buildThemeStorageScopeId} 计算的存储分区标识
 * @returns {App.Theme.ThemeSetting} 生效的主题配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-20
 */
export function resolveThemeSettingsForScope(scopeId: string): App.Theme.ThemeSetting {
  const isProd = import.meta.env.PROD;
  /** 优先读分区快照，兼容旧版本未分区的单个 `themeSettings` */
  const localSettings = readScopedThemeSettings(scopeId) ?? readLegacyThemeSettings();

  if (!isProd) {
    return localSettings ? defu(localSettings, themeSettings) : themeSettings;
  }

  // 生产：合并缓存后对「当前分区」检查发版默认值；独立于全局旧的 overrideThemeFlag，避免切换身份 hydrate 时被其它分区提前置位干扰
  let merged = defu(localSettings, themeSettings);

  const scopedPublish = readScopedPublishOverrideFlag(scopeId);
  if (scopedPublish !== BUILD_TIME) {
    merged = defu(overrideThemeSettings, merged);
    writeScopedPublishOverrideFlag(scopeId, BUILD_TIME);
    /** 与历史键对齐：仍存在读全局标记的运维/脚本时可见当前构建已过发版默认值流程 */
    localStg.set('overrideThemeFlag', BUILD_TIME);
  }

  return merged;
}

/**
 * 作用：根据主题色与 token 表生成亮色/暗色两套 CSS 变量用数据结构。
 * @param colors 主题五色配置
 * @param tokens 可选覆盖的 token 片段
 * @param recommended 是否使用推荐色算法
 * @returns {{ themeTokens; darkThemeTokens }} 亮/暗 token
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function createThemeToken(
  colors: App.Theme.ThemeColor,
  tokens?: App.Theme.ThemeSetting['tokens'],
  recommended = false
) {
  const paletteColors = createThemePaletteColors(colors, recommended);

  const { light, dark } = tokens || themeSettings.tokens;

  const themeTokens: App.Theme.ThemeTokenCSSVars = {
    colors: {
      ...paletteColors,
      nprogress: paletteColors.primary,
      ...light.colors
    },
    boxShadow: {
      ...light.boxShadow
    }
  };

  const darkThemeTokens: App.Theme.ThemeTokenCSSVars = {
    colors: {
      ...themeTokens.colors,
      ...dark?.colors
    },
    boxShadow: {
      ...themeTokens.boxShadow,
      ...dark?.boxShadow
    }
  };

  return {
    themeTokens,
    darkThemeTokens
  };
}

/**
 * 作用：由基础色生成 ant design 风格的完整色阶键值（含 50–950）。
 * @param colors 各语义色 hex
 * @param recommended 是否启用推荐算法
 * @returns {App.Theme.ThemePaletteColor} palette 变量表
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function createThemePaletteColors(colors: App.Theme.ThemeColor, recommended = false) {
  const colorKeys = Object.keys(colors) as App.Theme.ThemeColorKey[];
  const colorPaletteVar = {} as App.Theme.ThemePaletteColor;

  colorKeys.forEach(key => {
    const colorMap = getColorPalette(colors[key], recommended);

    colorPaletteVar[key] = colorMap.get(500)!;

    colorMap.forEach((hex, number) => {
      colorPaletteVar[`${key}-${number}`] = hex;
    });
  });

  return colorPaletteVar;
}

/**
 * 作用：将主题 token 转为内联 CSS 变量赋值串（`:root` / `.dark` 注入用）。
 * @param tokens 亮色或暗色基础 token
 * @returns {string} `key: value;` 拼接结果
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function getCssVarByTokens(tokens: App.Theme.BaseToken) {
  const styles: string[] = [];

  function removeVarPrefix(value: string) {
    return value.replace('var(', '').replace(')', '');
  }

  function removeRgbPrefix(value: string) {
    return value.replace('rgb(', '').replace(')', '');
  }

  for (const [key, tokenValues] of Object.entries(themeVars)) {
    for (const [tokenKey, tokenValue] of Object.entries(tokenValues)) {
      let cssVarsKey = removeVarPrefix(tokenValue);
      let cssValue = tokens[key][tokenKey];

      if (key === 'colors') {
        cssVarsKey = removeRgbPrefix(cssVarsKey);
        const { r, g, b } = getRgbOfColor(cssValue);
        cssValue = `${r} ${g} ${b}`;
      }

      styles.push(`${cssVarsKey}: ${cssValue}`);
    }
  }

  const styleStr = styles.join(';');

  return styleStr;
}

/**
 * 作用：把亮/暗 token 写入全局 `<style id="theme-vars">`，供全站 CSS 使用。
 * @param tokens 亮色 token
 * @param darkTokens 暗色 token
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function addThemeVarsToGlobal(tokens: App.Theme.BaseToken, darkTokens: App.Theme.BaseToken) {
  const cssVarStr = getCssVarByTokens(tokens);
  const darkCssVarStr = getCssVarByTokens(darkTokens);

  const css = `
    :root {
      ${cssVarStr}
    }
  `;

  const darkCss = `
    html.${DARK_CLASS} {
      ${darkCssVarStr}
    }
  `;

  const styleId = 'theme-vars';

  const style = document.querySelector(`#${styleId}`) || document.createElement('style');

  style.id = styleId;

  style.textContent = css + darkCss;

  document.head.appendChild(style);
}

/**
 * 作用：在 html 根节点上切换 `dark` class，联动暗色 CSS 变量。
 * @param darkMode 是否暗色
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function toggleCssDarkMode(darkMode = false) {
  const { add, remove } = toggleHtmlClass(DARK_CLASS);

  if (darkMode) {
    add();
  } else {
    remove();
  }
}

/**
 * 作用：切换灰度/色弱辅助显示（html filter）。
 * @param grayscaleMode 是否灰度
 * @param colourWeakness 是否色弱反色
 * @returns {void}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function toggleAuxiliaryColorModes(grayscaleMode = false, colourWeakness = false) {
  const htmlElement = document.documentElement;
  htmlElement.style.filter = [grayscaleMode ? 'grayscale(100%)' : '', colourWeakness ? 'invert(80%)' : '']
    .filter(Boolean)
    .join(' ');
}

/**
 * 作用：生成 ant-design-vue ConfigProvider 的 theme 配置（算法与 token）。
 * @param colors 主题五色
 * @param darkMode 是否暗色算法
 * @returns {ConfigProviderProps['theme']} antd theme 对象
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getAntdTheme(colors: App.Theme.ThemeColor, darkMode: boolean) {
  const { defaultAlgorithm, darkAlgorithm } = antdTheme;

  const { primary, info, success, warning, error } = colors;

  const theme: ConfigProviderProps['theme'] = {
    token: {
      colorPrimary: primary,
      colorInfo: info,
      colorSuccess: success,
      colorWarning: warning,
      colorError: error
    },
    algorithm: [darkMode ? darkAlgorithm : defaultAlgorithm],
    components: {
      Button: {
        controlHeightSM: 28
      },
      Menu: {
        colorSubItemBg: 'transparent'
      }
    }
  };

  return theme;
}
