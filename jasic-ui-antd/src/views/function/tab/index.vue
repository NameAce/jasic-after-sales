<script setup lang="ts">
/**
 * 菜单演示 — 页签标题：自定义 tab 文案并跳转带 query 的路由以验证多 Tab 行为。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { ref } from 'vue';
import { useTabStore } from '@/store/modules/tab';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';

// Tab Store 与按 key 路由跳转封装
const tabStore = useTabStore();
const { routerPushByKey } = useRouterPush();

// 自定义 Tab 标题输入
const tabLabel = ref('');

/**
 * 作用：将当前 Tab 标题设为输入框内容。
 * @param 无
 * @returns {void} 无
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function changeTabLabel() {
  tabStore.setTabLabel(tabLabel.value);
}

/**
 * 作用：恢复当前 Tab 标题为路由默认。
 * @param 无
 * @returns {void} 无
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetTabLabel() {
  tabStore.resetTabLabel();
}
</script>

<template>
  <ASpace direction="vertical" :size="16">
    <!-- 功能演示页：tab/index.vue -->
    <ACard :title="$t('page.function.tab.tabOperate.title')" :bordered="false" size="small" class="card-wrapper">
      <ADivider orientation="left">{{ $t('page.function.tab.tabOperate.addTab') }}</ADivider>
      <AButton @click="routerPushByKey('home')">{{ $t('page.function.tab.tabOperate.addTabDesc') }}</AButton>

      <ADivider orientation="left">{{ $t('page.function.tab.tabOperate.closeTab') }}</ADivider>
      <ASpace :size="16">
        <AButton @click="tabStore.removeActiveTab">
          {{ $t('page.function.tab.tabOperate.closeCurrentTab') }}
        </AButton>
        <AButton @click="tabStore.removeTabByRouteName('home')">
          {{ $t('page.function.tab.tabOperate.closeAboutTab') }}
        </AButton>
      </ASpace>

      <ADivider orientation="left">{{ $t('page.function.tab.tabOperate.addMultiTab') }}</ADivider>
      <ASpace :size="16" wrap class="m-0!">
        <AButton @click="routerPushByKey('function_multi-tab')">
          {{ $t('page.function.tab.tabOperate.addMultiTabDesc1') }}
        </AButton>
        <AButton @click="routerPushByKey('function_multi-tab', { query: { a: '1' } })">
          {{ $t('page.function.tab.tabOperate.addMultiTabDesc2') }}
        </AButton>
      </ASpace>
    </ACard>
    <ACard :title="$t('page.function.tab.tabTitle.title')" :bordered="false" size="small" class="card-wrapper">
      <ADivider orientation="left">{{ $t('page.function.tab.tabTitle.changeTitle') }}</ADivider>
      <AInputSearch
        v-model:value="tabLabel"
        :enter-button="$t('page.function.tab.tabTitle.change')"
        class="max-w-240px"
        @search="changeTabLabel"
      />

      <ADivider orientation="left">{{ $t('page.function.tab.tabTitle.resetTitle') }}</ADivider>
      <AButton @click="resetTabLabel">{{ $t('page.function.tab.tabTitle.reset') }}</AButton>
    </ACard>
  </ASpace>
</template>

<style scoped></style>
