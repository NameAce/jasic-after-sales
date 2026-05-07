/// <reference types="vite/client" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  /** 业务后端基础地址 */
  readonly VITE_HTTP: string
  /** 静态资源 OSS 域名（不含末尾斜杠） */
  readonly VITE_OSS_BASE: string
  /** 是否启用旧版状态动作回退 */
  readonly VITE_ENABLE_LEGACY_STATUS_ACTION_FALLBACK?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
