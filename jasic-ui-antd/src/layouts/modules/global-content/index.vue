<script setup lang="ts">
/**
 * 主内容区：RouterView + KeepAlive，配合页签 id 作为 key，并在路由切换时滚动复位。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { LAYOUT_SCROLL_EL_ID } from '@sa/materials';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { useRouteStore } from '@/store/modules/route';
import { useTabStore } from '@/store/modules/tab';

defineOptions({
  name: 'GlobalContent'
});

interface Props {
  /**
   * Show padding for content
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  showPadding?: boolean;
}

withDefaults(defineProps<Props>(), {
  showPadding: true
});

const appStore = useAppStore();
const themeStore = useThemeStore();
const routeStore = useRouteStore();
const tabStore = useTabStore();

// 主题中开启页面切换动画时返回 animateMode，否则无过渡名
const transitionName = computed(() => (themeStore.page.animate ? themeStore.page.animateMode : ''));

/**
 * 将布局主滚动容器滚回顶部，避免切换路由后保留旧滚动位置
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetScroll() {
  const el = document.querySelector(`#${LAYOUT_SCROLL_EL_ID}`);

  el?.scrollTo({ left: 0, top: 0 });
}
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <!-- 主内容区：路由视图与 keep-alive -->
    <Transition
      :name="transitionName"
      mode="out-in"
      @before-leave="appStore.setContentXScrollable(true)"
      @after-leave="resetScroll"
      @after-enter="appStore.setContentXScrollable(false)"
    >
      <KeepAlive :include="routeStore.cacheRoutes" :exclude="routeStore.excludeCacheRoutes">
        <component
          :is="Component"
          v-if="appStore.reloadFlag"
          :key="tabStore.getTabIdByRoute(route)"
          :class="{ 'p-16px': showPadding }"
          class="flex flex-col flex-grow bg-layout transition-300"
        />
      </KeepAlive>
    </Transition>
  </RouterView>
</template>

<style></style>
