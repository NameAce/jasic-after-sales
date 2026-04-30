/**
 * Pinia 中 setup 语法 Store 的 `$id` 枚举，与 `resetSetupStore` 插件白名单一致。
 */
export enum SetupStoreId {
  App = 'app-store',
  Theme = 'theme-store',
  Auth = 'auth-store',
  Route = 'route-store',
  Tab = 'tab-store'
}
