/**
 * 模板与运行时属性用的色值字符串（与 `src/styles/variables.scss`、`mp/aftersale` 的 `constants/theme.ts` 对齐）。
 * 双端公共 token 必须与 `mp/aftersale/src/constants/theme.ts` 保持键名与值一致；师傅端独有键列于末尾。
 *
 * 镜像口径：等价于 aftersale `src/constants/theme.ts`，不迁移、不镜像路径，
 * 只镜像公共 token 字面（目录命名与存放位置仅为建议，详见
 * `mp/MIRROR_FILE_PAIRS.md` 的「目录命名和存放位置仅做建议」条目）。
 */
export const themeColors = {
  // --- 双端公共 token ---
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

  // --- 师傅端独有 / 扩展 ---
  // 以下 token 仅在 contractor 使用，不镜像到 aftersale。
  /** 主色偏亮（渐变、装饰） */
  primaryLight: '#ff8533',
  /** 登录/绑定页深色顶栏渐变 */
  loginGradientStart: '#0f172a',
  loginGradientEnd: '#020617',
  /** 深色背景上的浅红提示 */
  rose200: '#fecaca',
  /** 语音条浅底、与 `$bg-muted-panel` 一致 */
  voicePanelBg: '#f9f9f9',
  voicePanelBorder: '#d9dfe6',
  /** 与 `$primary-tint-bg` / 浅橙提示区一致 */
  tagBrandBg: '#fff7ed',
  tagBrandBorder: '#ffedd5',
  tagBrandAccentBorder: '#fde4cf',
} as const

/** 与 `themeColors` 同引用，供沿用旧命名的组件使用 */
export const themeColor = themeColors

export type ThemeColorName = keyof typeof themeColors
