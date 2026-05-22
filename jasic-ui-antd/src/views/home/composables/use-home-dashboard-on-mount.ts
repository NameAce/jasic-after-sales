import { type Ref, onMounted } from 'vue';

/**
 * 首页看板挂载拉取：首次进入请求一次；页签栏「刷新」会 remount 当前页，
 * 但看板 state 为模块级单例且 loaded 仍为 true，须传 force 才会重新打首页聚合接口。
 */
export function useHomeDashboardOnMount(load: (force?: boolean) => Promise<void> | void, loaded: Ref<boolean>) {
  onMounted(() => {
    Promise.resolve(load(loaded.value)).catch(() => undefined);
  });
}
