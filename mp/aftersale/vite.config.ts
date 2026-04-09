import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import AutoImport from 'unplugin-auto-import/vite'

// process.env.NODE_ENV // 应用运行的模式，比如vite.config.ts里
// import.meta.env.VITE_HTTP // src下的vue文件或其他ts文件里

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    AutoImport({
      imports: [
        'vue', // 自动引入 Vue API
        'vue-i18n', // 如果你用了
        'pinia', // 如果你用了
        {
          '@/stores': ['useUserStore'],
        },
      ],
      dts: 'src/auto-imports.d.ts', // 自动生成类型文件
      dtsMode: 'overwrite', // 避免从配置中移除条目后仍残留在 d.ts 里
      eslintrc: {
        enabled: true,
        filepath: './.eslintrc-auto-import.json',
        globalsPropValue: true,
      },
    }),
    uni.default(),
  ],
  build: {
    // 开发阶段启用源码映射：https://uniapp.dcloud.net.cn/tutorial/migration-to-vue3.html#需主动开启-sourcemap
    sourcemap: process.env.NODE_ENV === 'development',
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler', // 或 'modern'，建议用 'modern-compiler'
        // 依赖（node_modules）里的 Sass 弃用提示太吵：统一静默依赖告警
        quietDeps: true,
        // 临时静默 Sass 弃用告警（等上游依赖迁移后可移除）
        silenceDeprecations: ['legacy-js-api', 'global-builtin'],
        additionalData: `
          @use "@/styles/variables.scss" as *;
          @use "@/styles/mixins.scss" as *;
        `,
      },
    },
  },
})
