import { type Ref, onMounted } from 'vue';

/**
 * 作用：首页看板挂载时触发数据拉取；首次进入请求一次，页签栏「刷新」remount 时若 state 已 loaded 须由 load(force) 处理。
 * @param load - 看板加载函数，签名与 loadHqDashboard(force) 等一致
 * @param loaded - 是否已加载过（模块级单例 state 的 loaded）
 * @returns void（在 onMounted 中注册副作用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useHomeDashboardOnMount(load: (force?: boolean) => Promise<void> | void, loaded: Ref<boolean>) {
  onMounted(() => {
    // loaded 为 true 时仍调用 load，由 load 内部根据 force 默认 false 决定是否跳过；remount 场景由首页 index 传 force
    Promise.resolve(load(loaded.value)).catch(() => undefined);
  });
}
