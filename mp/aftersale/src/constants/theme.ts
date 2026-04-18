/**
 * 小程序 / 组件模板用颜色字符串（与 `src/styles/variables.scss` 数值一致）。
 * 改主题时请同时更新 SCSS 与本文件。
 */
export const themeColors = {
  // --- 双端公共 token（与 mp/contractor/src/theme/colors.ts 同名同值） ---
  primary: '#f26604',
  primaryContrast: '#ffffff',
  textBg: '#fff',
  textDark: '#0f172a',
  textMain: '#303133',
  textSecondary: '#606266',
  textPlaceholder: '#909399',
  textMuted: '#94a3b8',
  textSubtle: '#666666',
  textLabel: '#64748b',
  textBody: '#334155',
  textIconWeak: '#999999',
  iconSecondary: '#9ca3af',
  iconSlateLight: '#cbd5e1',
  borderNeutral: '#e5e7eb',
  borderSlate: '#e2e8f0',
  bgCard: '#ffffff',
  bgPage: '#f6f6f8',
  bgInput: '#f8fafc',
  primaryTintBg: '#fff7ed',
  successSolid: '#16a34a',
  warrantyOut: '#dc2626',
  warningAmber: '#f59e0b',
  danger: '#fa3534',
  dangerEmphasis: '#ef4444',
  info: '#909399',
  vipBadgeBg: '#facc15',

  // --- 用户端扩展区（当前无专有 token，后续新增请集中在此） ---
} as const

/** 与 `themeColors` 同引用，供沿用旧命名的组件/页面使用 */
export const themeColor = themeColors

export type ThemeColorName = keyof typeof themeColors
