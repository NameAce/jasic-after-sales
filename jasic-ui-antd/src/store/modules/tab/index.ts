/**
 * 多页签：与当前路由同步的 tabs、固定/首页顺序、切换与关闭策略及本地缓存键。
 */
import { computed, ref } from 'vue';
import { useEventListener } from '@vueuse/core';
import { defineStore } from 'pinia';
import type { RouteKey } from '@elegant-router/types';
import { router } from '@/router';
import { useRouteStore } from '@/store/modules/route';
import { useRouterPush } from '@/hooks/common/router';
import { localStg } from '@/utils/storage';
import { SetupStoreId } from '@/enum';
import { useThemeStore } from '../theme';
import {
  extractTabsByAllRoutes,
  filterTabsById,
  filterTabsByIds,
  findTabByRouteName,
  getAllTabs,
  getDefaultHomeTab,
  getFixedTabIds,
  getTabByRoute,
  getTabIdByRoute,
  isTabInTabs,
  updateTabByI18nKey,
  updateTabsByI18nKey
} from './shared';

/** 多页签状态：打开列表、激活项、与路由联动及本地缓存 */
export const useTabStore = defineStore(SetupStoreId.Tab, () => {
  const routeStore = useRouteStore();
  const themeStore = useThemeStore();
  const { routerPush } = useRouterPush(false);

  // 除首页外的已打开页签列表
  const tabs = ref<App.Global.Tab[]>([]);

  // 固定的首页标签元数据
  const homeTab = ref<App.Global.Tab>();

  /**
   * 根据路由配置生成默认首页 tab。
   *
   * @returns {void} 无返回值
   */
  function initHomeTab() {
    homeTab.value = getDefaultHomeTab(router, routeStore.routeHome);
  }

  // 首页 + 业务页签的完整列表（用于渲染 tab 栏）
  const allTabs = computed(() => getAllTabs(tabs.value, homeTab.value));

  // 当前高亮页签 id
  const activeTabId = ref<string>('');

  /**
   * 设置当前激活的页签 id。
   *
   * @param id - 页签 id
   * @returns {void} 无返回值
   */
  function setActiveTabId(id: string) {
    activeTabId.value = id;
  }

  /**
   * 应用启动时恢复本地缓存的页签并追加当前路由对应 tab。
   *
   * @param currentRoute - 当前路由上的 tab 元信息
   * @returns {void} 无返回值
   */
  function initTabStore(currentRoute: App.Global.TabRoute) {
    const storageTabs = localStg.get('globalTabs');

    if (themeStore.tab.cache && storageTabs) {
      const extractedTabs = extractTabsByAllRoutes(router, storageTabs);
      tabs.value = updateTabsByI18nKey(extractedTabs);
    }

    addTab(currentRoute);
  }

  /**
   * 为给定路由追加页签（首页不重复入栈），并可切换为激活态。
   *
   * @param route - 目标路由 tab 信息
   * @param active - 是否激活新页签，默认 true
   * @returns {void} 无返回值
   */
  function addTab(route: App.Global.TabRoute, active = true) {
    const tab = getTabByRoute(route);

    const isHomeTab = tab.id === homeTab.value?.id;

    if (!isHomeTab && !isTabInTabs(tab.id, tabs.value)) {
      tabs.value.push(tab);
    }

    if (active) {
      setActiveTabId(tab.id);
    }
  }

  /**
   * 关闭指定页签；若关闭的是当前激活项则跳转到最后一个或首页。
   *
   * @param tabId - 页签 id
   * @returns {Promise<void>} 无返回值
   */
  async function removeTab(tabId: string) {
    const isRemoveActiveTab = activeTabId.value === tabId;
    const updatedTabs = filterTabsById(tabId, tabs.value);

    function update() {
      tabs.value = updatedTabs;
    }

    if (!isRemoveActiveTab) {
      update();
      return;
    }

    const activeTab = updatedTabs.at(-1) || homeTab.value;

    if (activeTab) {
      await switchRouteByTab(activeTab);
      update();
    }
  }

  /**
   * 关闭当前激活的页签。
   *
   * @returns {Promise<void>} 无返回值
   */
  async function removeActiveTab() {
    await removeTab(activeTabId.value);
  }

  /**
   * 根据路由 name 查找并关闭对应页签。
   *
   * @param routeName - 路由 name
   * @returns {Promise<void>} 无返回值
   */
  async function removeTabByRouteName(routeName: RouteKey) {
    const tab = findTabByRouteName(routeName, tabs.value);
    if (!tab) return;

    await removeTab(tab.id);
  }

  /**
   * 批量关闭页签，保留固定页与 excludes 中的 id。
   *
   * @param excludes - 额外保留的页签 id
   * @returns {Promise<void>} 无返回值
   */
  async function clearTabs(excludes: string[] = []) {
    const remainTabIds = [...getFixedTabIds(tabs.value), ...excludes];
    const removedTabsIds = tabs.value.map(tab => tab.id).filter(id => !remainTabIds.includes(id));

    const isRemoveActiveTab = removedTabsIds.includes(activeTabId.value);
    const updatedTabs = filterTabsByIds(removedTabsIds, tabs.value);

    function update() {
      tabs.value = updatedTabs;
    }

    if (!isRemoveActiveTab) {
      update();
      return;
    }

    const activeTab = updatedTabs[updatedTabs.length - 1] || homeTab.value;

    await switchRouteByTab(activeTab);
    update();
  }

  /**
   * 跳转到页签记录的全路径并同步激活 id。
   *
   * @param tab - 目标页签
   * @returns {Promise<void>} 无返回值
   */
  async function switchRouteByTab(tab: App.Global.Tab) {
    const fail = await routerPush(tab.fullPath);
    if (!fail) {
      setActiveTabId(tab.id);
    }
  }

  /**
   * 关闭指定页签左侧的所有可关闭页签。
   *
   * @param tabId - 锚点页签 id
   * @returns {Promise<void>} 无返回值
   */
  async function clearLeftTabs(tabId: string) {
    const tabIds = tabs.value.map(tab => tab.id);
    const index = tabIds.indexOf(tabId);
    if (index === -1) return;

    const excludes = tabIds.slice(index);
    await clearTabs(excludes);
  }

  /**
   * 关闭指定页签右侧的所有可关闭页签。
   *
   * @param tabId - 锚点页签 id
   * @returns {Promise<void>} 无返回值
   */
  async function clearRightTabs(tabId: string) {
    const isHomeTab = tabId === homeTab.value?.id;
    if (isHomeTab) {
      clearTabs();
      return;
    }

    const tabIds = tabs.value.map(tab => tab.id);
    const index = tabIds.indexOf(tabId);
    if (index === -1) return;

    const excludes = tabIds.slice(0, index + 1);
    await clearTabs(excludes);
  }

  /**
   * 为页签设置临时展示标题（如带动态参数的路由）。
   *
   * @param label - 新标题文案
   * @param tabId - 目标页签 id，默认当前激活
   * @returns {void} 无返回值
   */
  function setTabLabel(label: string, tabId?: string) {
    const id = tabId || activeTabId.value;

    const tab = tabs.value.find(item => item.id === id);
    if (!tab) return;

    tab.oldLabel = tab.label;
    tab.newLabel = label;
  }

  /**
   * 清除页签上通过 setTabLabel 设置的临时标题。
   *
   * @param tabId - 目标页签 id，默认当前激活
   * @returns {void} 无返回值
   */
  function resetTabLabel(tabId?: string) {
    const id = tabId || activeTabId.value;

    const tab = tabs.value.find(item => item.id === id);
    if (!tab) return;

    tab.newLabel = undefined;
  }

  /**
   * 判断页签是否为首页或固定不可关闭项。
   *
   * @param tabId - 页签 id
   * @returns {boolean} 是否应保留
   */
  function isTabRetain(tabId: string) {
    if (tabId === homeTab.value?.id) return true;

    const fixedTabIds = getFixedTabIds(tabs.value);

    return fixedTabIds.includes(tabId);
  }

  /**
   * 语言变更后刷新所有页签上的 i18n 标题。
   *
   * @returns {void} 无返回值
   */
  function updateTabsByLocale() {
    tabs.value = updateTabsByI18nKey(tabs.value);

    if (homeTab.value) {
      homeTab.value = updateTabByI18nKey(homeTab.value);
    }
  }

  /**
   * 在开启 tab 缓存配置时将当前页签列表写入本地存储。
   *
   * @returns {void} 无返回值
   */
  function cacheTabs() {
    if (!themeStore.tab.cache) return;

    localStg.set('globalTabs', tabs.value);
  }

  // 关闭或刷新页面前持久化页签
  useEventListener(window, 'beforeunload', () => {
    cacheTabs();
  });

  return {
    // 对外暴露的完整页签列表（含首页）
    tabs: allTabs,
    activeTabId,
    initHomeTab,
    initTabStore,
    addTab,
    removeTab,
    removeActiveTab,
    removeTabByRouteName,
    clearTabs,
    clearLeftTabs,
    clearRightTabs,
    switchRouteByTab,
    setTabLabel,
    resetTabLabel,
    isTabRetain,
    updateTabsByLocale,
    getTabIdByRoute,
    cacheTabs
  };
});
