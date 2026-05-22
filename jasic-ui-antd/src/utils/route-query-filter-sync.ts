import { type Ref, onActivated, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

/**
 * 首页 routeTarget 跳转后，将路由 query 同步到目标页 reactive 筛选对象。
 * 各业务页仅在「已有搜索表单项」上回显；无表单项的 query 键只参与列表请求。
 */

/** 将路由 query 中的字符串安全取出并 trim */
export function readRouteQueryString(query: Record<string, unknown>, key: string): string {
  const raw = query[key];
  if (raw === undefined || raw === null) return '';
  return String(raw).trim();
}

/** 将路由 query 中的数值解析为 number，非法时返回 undefined */
export function readRouteQueryNumber(query: Record<string, unknown>, key: string): number | undefined {
  const text = readRouteQueryString(query, key);
  if (!text) return undefined;
  const num = Number(text);
  return Number.isFinite(num) ? num : undefined;
}

type RouteQuerySyncOptions = {
  /** 从 route.query 写入本地筛选 */
  apply: () => void;
  /** 同步后刷新列表（可选） */
  reload?: () => void;
  /** 监听这些 query 字段变化时重新 apply + reload */
  watchQueryKeys?: string[];
  /** 为 true 时本次 watch 只 apply 不 reload（配合重置清路由） */
  skipReloadRef?: Ref<boolean>;
};

/**
 * 挂载、KeepAlive 激活、以及指定 query 变化时同步筛选并回显。
 */
export function useRouteQueryFilterSync(options: RouteQuerySyncOptions) {
  const route = useRoute();

  function runSync(reload = true) {
    options.apply();
    if (!reload || !options.reload) return;
    if (options.skipReloadRef?.value) {
      options.skipReloadRef.value = false;
      return;
    }
    options.reload();
  }

  onMounted(() => {
    runSync(true);
  });

  onActivated(() => {
    runSync(true);
  });

  if (options.watchQueryKeys?.length) {
    watch(
      () => options.watchQueryKeys!.map(key => route.query[key]),
      () => {
        runSync(true);
      }
    );
  }
}
