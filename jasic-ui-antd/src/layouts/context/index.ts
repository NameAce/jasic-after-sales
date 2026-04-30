import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useContext } from '@sa/hooks';
import { useRouteStore } from '@/store/modules/route';

export const { setupStore: setupMixMenuContext, useStore: useMixMenuContext } = useContext('mix-menu', useMixMenu);

/**
 * 混合布局下的一级/子级菜单上下文：根据当前路由同步选中的一级菜单与子菜单列表。
 * @returns 菜单数据、一级 key、子级列表及同步方法
 */
function useMixMenu() {
  const route = useRoute();
  const routeStore = useRouteStore();
  const { selectedKey } = useMenu();

  const activeFirstLevelMenuKey = ref('');

  /**
   * 作用：设置当前高亮的一级菜单 key。
   * @param key 路由 name 前缀段
   * @returns {void}
   */
  function setActiveFirstLevelMenuKey(key: string) {
    activeFirstLevelMenuKey.value = key;
  }

  /**
   * 作用：从当前 `selectedKey` 解析一级路由名并写入 `activeFirstLevelMenuKey`。
   * @returns {void}
   */
  function getActiveFirstLevelMenuKey() {
    const [firstLevelRouteName] = selectedKey.value.split('_');

    setActiveFirstLevelMenuKey(firstLevelRouteName);
  }

  // 与 routeStore.menus 同步的完整菜单树
  const allMenus = computed<App.Global.Menu[]>(() => routeStore.menus);

  // 去掉 children 的一级菜单列表（用于顶栏/侧栏一级展示）
  const firstLevelMenus = computed<App.Global.Menu[]>(() =>
    routeStore.menus.map(menu => {
      const { children: _, ...rest } = menu;

      return rest;
    })
  );

  // 当前一级 key 下展开的子菜单（混合布局右侧/抽屉）
  const childLevelMenus = computed<App.Global.Menu[]>(
    () => routeStore.menus.find(menu => menu.key === activeFirstLevelMenuKey.value)?.children || []
  );

  // 当前一级菜单是否仍存在可展示子节点
  const isActiveFirstLevelMenuHasChildren = computed(() => {
    if (!activeFirstLevelMenuKey.value) {
      return false;
    }

    const findItem = allMenus.value.find(item => item.key === activeFirstLevelMenuKey.value);

    return Boolean(findItem?.children?.length);
  });

  // 路由 name 变化时重算一级选中项（含初始 immediate）
  watch(
    () => route.name,
    () => {
      getActiveFirstLevelMenuKey();
    },
    { immediate: true }
  );

  return {
    allMenus,
    firstLevelMenus,
    childLevelMenus,
    isActiveFirstLevelMenuHasChildren,
    activeFirstLevelMenuKey,
    setActiveFirstLevelMenuKey,
    getActiveFirstLevelMenuKey
  };
}

/**
 * 作用：解析当前路由在菜单选中态下应对应的菜单 key（支持 hideInMenu + activeMenu）。
 * @returns {{ selectedKey }}
 */
export function useMenu() {
  const route = useRoute();

  // 隐藏菜单项时用 activeMenu 作为高亮 key，否则用当前 name
  const selectedKey = computed(() => {
    const { hideInMenu, activeMenu } = route.meta;
    const name = route.name as string;

    const routeName = (hideInMenu ? activeMenu : name) || name;

    return routeName;
  });

  return {
    selectedKey
  };
}
