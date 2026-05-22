/**
 * 承修方小程序（网点/总部工单处理、派工）：index。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

import { createPinia } from 'pinia'
import persist from 'pinia-plugin-persistedstate'

// 创建 pinia 实例
const pinia = createPinia()
// 使用持久化存储插件
pinia.use(persist)

// 默认导出，给 main.ts 使用
export default pinia

// 模块统一导出
export * from './modules/user'
export * from './modules/app'
