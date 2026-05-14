import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { getRouteMenuTitle } from '@/utils/route-menu-title';

/**
 * 当前路由在侧栏菜单中的展示标题（与菜单 label 规则一致）。
 */
export function useRouteMenuTitle() {
  const route = useRoute();
  return computed(() => getRouteMenuTitle(route));
}
