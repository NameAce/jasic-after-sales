<script setup lang="ts">
/**
 * 多页签栏：与 tabStore 同步当前路由页签，横向滚动定位激活项，并提供刷新/全内容区切换。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { nextTick, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useElementBounding } from '@vueuse/core';
import { PageTab } from '@sa/materials';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useTabStore } from '@/store/modules/tab';
import { isPC } from '@/utils/agent';
import BetterScroll from '@/components/custom/better-scroll.vue';
import ContextMenu from './context-menu.vue';

defineOptions({
  name: 'GlobalTab'
});

const route = useRoute();
const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const tabStore = useTabStore();

const bsWrapper = ref<HTMLElement>();
const { width: bsWrapperWidth, left: bsWrapperLeft } = useElementBounding(bsWrapper);
const bsScroll = ref<InstanceType<typeof BetterScroll>>();
const tabRef = ref<HTMLElement>();
// 非 PC 时 BetterScroll 需开启 click 以响应触摸
const isPCFlag = isPC();

const TAB_DATA_ID = 'data-tab-id';

type TabNamedNodeMap = NamedNodeMap & {
  [TAB_DATA_ID]: Attr;
};

/**
 * 激活页签变化后，将其滚入横向标签栏可视区域中部
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function scrollToActiveTab() {
  await nextTick();
  if (!tabRef.value) return;

  const { children } = tabRef.value;

  for (let i = 0; i < children.length; i += 1) {
    const child = children[i];

    const { value: tabId } = (child.attributes as TabNamedNodeMap)[TAB_DATA_ID];

    if (tabId === tabStore.activeTabId) {
      const { left, width } = child.getBoundingClientRect();
      const clientX = left + width / 2;

      setTimeout(() => {
        scrollByClientX(clientX);
      }, 50);

      break;
    }
  }
}

/**
 * 按视口 clientX 计算与容器中心的偏差，驱动 BetterScroll 横向滚动
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function scrollByClientX(clientX: number) {
  const currentX = clientX - bsWrapperLeft.value;
  const deltaX = currentX - bsWrapperWidth.value / 2;

  if (bsScroll.value?.instance) {
    const { maxScrollX, x: leftX, scrollBy } = bsScroll.value.instance;

    const rightX = maxScrollX - leftX;
    const update = deltaX > 0 ? Math.max(-deltaX, rightX) : Math.min(-deltaX, -leftX);

    scrollBy(update, 0, 300);
  }
}

/**
 * 固定保留页签（如首页）在右键菜单中禁用「关闭当前/左侧」等项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getContextMenuDisabledKeys(tabId: string) {
  const disabledKeys: App.Global.DropdownKey[] = [];

  if (tabStore.isTabRetain(tabId)) {
    const homeDisable: App.Global.DropdownKey[] = ['closeCurrent', 'closeLeft'];
    disabledKeys.push(...homeDisable);
  }

  return disabledKeys;
}

/**
 * 关闭单个页签；若主题配置为 close 时重置路由缓存
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleCloseTab(tab: App.Global.Tab) {
  await tabStore.removeTab(tab.id);

  if (themeStore.resetCacheStrategy === 'close') {
    routeStore.resetRouteCache(tab.routeKey);
  }
}

/**
 * 通过全局 reloadFlag 触发当前页整页重载（带短延迟以配合动画）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function refresh() {
  appStore.reloadPage(500);
}

/**
 * 点击标签栏空白处时去掉焦点，避免键盘焦点留在已隐藏控件上
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function removeFocus() {
  (document.activeElement as HTMLElement)?.blur();
}

/**
 * 根据当前路由初始化 tabStore（含固定首页等）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function init() {
  tabStore.initTabStore(route);
}

// 路由全路径变化时向 store 注册/更新页签
watch(
  () => route.fullPath,
  () => {
    tabStore.addTab(route);
  }
);
// 激活 tab id 变化时将该项滚入标签栏可视区
watch(
  () => tabStore.activeTabId,
  () => {
    scrollToActiveTab();
  }
);

// init
init();
</script>

<template>
  <!-- 多页签栏：切换、关闭与右键菜单 -->
  <DarkModeContainer class="size-full flex-y-center px-16px shadow-tab">
    <div ref="bsWrapper" class="h-full flex-1-hidden">
      <BetterScroll ref="bsScroll" :options="{ scrollX: true, scrollY: false, click: !isPCFlag }" @click="removeFocus">
        <div
          ref="tabRef"
          class="h-full flex pr-18px"
          :class="[themeStore.tab.mode === 'chrome' ? 'items-end' : 'items-center gap-12px']"
        >
          <ContextMenu
            v-for="tab in tabStore.tabs"
            :key="tab.id"
            :tab-id="tab.id"
            :disabled-keys="getContextMenuDisabledKeys(tab.id)"
          >
            <PageTab
              :[TAB_DATA_ID]="tab.id"
              :mode="themeStore.tab.mode"
              :dark-mode="themeStore.darkMode"
              :active="tab.id === tabStore.activeTabId"
              :active-color="themeStore.themeColor"
              :closable="!tabStore.isTabRetain(tab.id)"
              @click="tabStore.switchRouteByTab(tab)"
              @close="handleCloseTab(tab)"
            >
              <template #prefix>
                <SvgIcon
                  :icon="tab.icon"
                  :local-icon="tab.localIcon"
                  class="inline-block align-text-bottom text-16px"
                />
              </template>
              <div class="max-w-240px ellipsis-text">{{ tab.label }}</div>
            </PageTab>
          </ContextMenu>
        </div>
      </BetterScroll>
    </div>
    <ReloadButton :loading="!appStore.reloadFlag" @click="refresh" />
    <FullScreen :full="appStore.fullContent" @click="appStore.toggleFullContent" />
  </DarkModeContainer>
</template>

<style scoped></style>
