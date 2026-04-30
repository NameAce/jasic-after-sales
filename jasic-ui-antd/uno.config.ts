import { defineConfig } from '@unocss/vite';
import transformerDirectives from '@unocss/transformer-directives';
import transformerVariantGroup from '@unocss/transformer-variant-group';
import presetUno from '@unocss/preset-uno';
import type { Theme } from '@unocss/preset-uno';
import { presetSoybeanAdmin } from '@sa/uno-preset';
import { themeVars } from './src/theme/vars';

export default defineConfig<Theme>({
  // 内容管道配置，排除 node_modules 和 dist 目录
  content: {
    pipeline: {
      exclude: ['node_modules', 'dist']
    }
  },
  // 主题配置，包含变量和字体大小
  theme: {
    ...themeVars,
    fontSize: {
      'icon-xs': '0.875rem',
      'icon-small': '1rem',
      icon: '1.125rem',
      'icon-large': '1.5rem',
      'icon-xl': '2rem'
    }
  },
  // 卡片包装器，包含圆角和阴影
  shortcuts: {
    'card-wrapper': 'rd-8px shadow-sm'
  },
  // 转换器配置，包含指令和变体组
  transformers: [transformerDirectives(), transformerVariantGroup()],
  // 预设配置，包含 UnoCSS 和 JasicUIAdmin 预设
  presets: [presetUno({ dark: 'class' }), presetSoybeanAdmin()]
});
