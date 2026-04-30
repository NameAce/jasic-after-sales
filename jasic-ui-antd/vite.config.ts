import process from 'node:process';
import { URL, fileURLToPath } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import { setupVitePlugins } from './build/plugins';
import { createViteProxy, getBuildTime } from './build/config';

/** Vite 开发与生产构建入口 */
export default defineConfig(configEnv => {
  // 读取当前 mode 下 .env / .env.[mode] 中的 VITE_* 变量
  const viteEnv = loadEnv(configEnv.mode, process.cwd()) as unknown as Env.ViteEnv;

  // 构建时间，注入为全局常量供运行时展示或排查
  const buildTime = getBuildTime();

  // 仅本地 dev（非 preview）时走代理，避免 preview 误用开发代理
  const enableProxy = configEnv.command === 'serve' && !configEnv.isPreview;

  return {
    // 静态资源 base，需与部署子路径、环境变量 VITE_BASE_URL 一致
    base: viteEnv.VITE_BASE_URL,
    resolve: {
      alias: {
        '~': fileURLToPath(new URL('./', import.meta.url)),
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    // 全局注入 SCSS，各组件无需重复 @use 变量/混入
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler',
          additionalData: `@use "@/styles/scss/global.scss" as *;`
        }
      }
    },
    // 插件配置，包含各种 vite 插件和自定义插件
    plugins: setupVitePlugins(viteEnv, buildTime),
    // 全局注入构建时间常量，方便运行时展示或排查
    define: {
      BUILD_TIME: JSON.stringify(buildTime)
    },
    // 开发服务器配置
    server: {
      // 开发服务器主机地址
      host: '0.0.0.0',
      // 开发服务器端口
      port: 9527,
      // 开发服务器是否自动打开浏览器
      open: true,
      // 开发服务器代理配置
      proxy: createViteProxy(viteEnv, enableProxy)
    },
    // 预览服务器配置
    preview: {
      port: 9725
    },
    // 生产构建配置
    build: {
      // 是否生成构建报告（大小统计）
      reportCompressedSize: false,
      sourcemap: viteEnv.VITE_SOURCE_MAP === 'Y',
      // 配置 CommonJS 打包选项
      commonjsOptions: {
        ignoreTryCatch: false
      }
    }
  };
});
