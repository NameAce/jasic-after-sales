/**
 * 全局应用布局状态：断点、主题抽屉、侧栏折叠、整页重载、全内容区与横向滚动控制等。
 */
import { effectScope, nextTick, onScopeDispose, ref, watch } from 'vue';
import { breakpointsTailwind, useBreakpoints, useEventListener, useTitle } from '@vueuse/core';
import { defineStore } from 'pinia';
import { useBoolean } from '@sa/hooks';
import { router } from '@/router';
import { localStg } from '@/utils/storage';
import { SetupStoreId } from '@/enum';
import { $t } from '@/locales';
import { setDayjsLocale } from '@/locales/dayjs';
import { useRouteStore } from '../route';
import { useThemeStore } from '../theme';

export const useAppStore = defineStore(SetupStoreId.App, () => {
  const themeStore = useThemeStore();
  const routeStore = useRouteStore();
  // 副作用作用域，便于在 store 销毁时统一停止 watch
  const scope = effectScope();
  // 基于 Tailwind 断点的响应式工具
  const breakpoints = useBreakpoints(breakpointsTailwind);
  const { bool: themeDrawerVisible, setTrue: openThemeDrawer, setFalse: closeThemeDrawer } = useBoolean();
  const { bool: reloadFlag, setBool: setReloadFlag } = useBoolean(true);
  const { bool: fullContent, toggle: toggleFullContent } = useBoolean();
  const { bool: contentXScrollable, setBool: setContentXScrollable } = useBoolean();
  const { bool: siderCollapse, setBool: setSiderCollapse, toggle: toggleSiderCollapse } = useBoolean();
  const {
    bool: mixSiderFixed,
    setBool: setMixSiderFixed,
    toggle: toggleMixSiderFixed
  } = useBoolean(localStg.get('mixSiderFixed') === 'Y');

  // 当前视口是否处于移动端布局（小于 sm 断点）
  const isMobile = breakpoints.smaller('sm');

  /**
   * 通过卸载再挂载路由视图实现整页刷新，并可按主题动画时长等待。
   *
   * @param duration - 刷新前等待的毫秒数（有页面动画时略长）
   * @returns {Promise<void>} 无返回值
   */
  async function reloadPage(duration = 300) {
    setReloadFlag(false);

    const d = themeStore.page.animate ? duration : 40;

    await new Promise(resolve => {
      setTimeout(resolve, d);
    });

    setReloadFlag(true);

    if (themeStore.resetCacheStrategy === 'refresh') {
      routeStore.resetRouteCache();
    }
  }

  // 固定中文环境：不再支持语言切换
  const locale = ref<App.I18n.LangType>('zh-CN');

  /**
   * 根据当前路由 meta 更新浏览器标签页标题。
   *
   * @returns {void} 无返回值
   */
  function updateDocumentTitleByLocale() {
    const { i18nKey, title } = router.currentRoute.value.meta;

    const documentTitle = i18nKey ? $t(i18nKey) : title;

    useTitle(documentTitle);
  }

  /**
   * 应用级初始化：同步 dayjs 语言、同步文档标题。
   *
   * @returns {void} 无返回值
   */
  function init() {
    setDayjsLocale();
    updateDocumentTitleByLocale();
  }

  scope.run(() => {
    // 监听移动端断点：进入移动端时备份布局并切竖排+收拢侧栏，退出时恢复
    watch(
      isMobile,
      newValue => {
        if (newValue) {
          // backup theme setting before is mobile
          localStg.set('backupThemeSettingBeforeIsMobile', {
            layout: themeStore.layout.mode,
            siderCollapse: siderCollapse.value
          });

          themeStore.setThemeLayout('vertical');
          setSiderCollapse(true);
        } else {
          // when is not mobile, recover the backup theme setting
          const backup = localStg.get('backupThemeSettingBeforeIsMobile');

          if (backup) {
            nextTick(() => {
              themeStore.setThemeLayout(backup.layout);
              setSiderCollapse(backup.siderCollapse);

              localStg.remove('backupThemeSettingBeforeIsMobile');
            });
          }
        }
      },
      { immediate: true }
    );
  });

  // 页面关闭前将混合侧栏固定状态写入本地存储
  useEventListener(window, 'beforeunload', () => {
    localStg.set('mixSiderFixed', mixSiderFixed.value ? 'Y' : 'N');
  });

  // store 卸载时停止 effectScope 内所有 watch
  onScopeDispose(() => {
    scope.stop();
  });

  // init
  init();

  return {
    isMobile,
    reloadFlag,
    reloadPage,
    fullContent,
    locale,
    themeDrawerVisible,
    openThemeDrawer,
    closeThemeDrawer,
    toggleFullContent,
    contentXScrollable,
    setContentXScrollable,
    siderCollapse,
    setSiderCollapse,
    toggleSiderCollapse,
    mixSiderFixed,
    setMixSiderFixed,
    toggleMixSiderFixed
  };
});
