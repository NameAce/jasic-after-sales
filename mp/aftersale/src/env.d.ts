/// <reference types="vite/client" />

/** 与根目录 `.env` 中 `VITE_HTTP` 一致，作为接口基址（如测试域） */
interface ImportMetaEnv {
  readonly VITE_HTTP: string
}

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
