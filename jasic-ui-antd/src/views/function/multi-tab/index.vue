<script setup lang="ts">
/**
 * 菜单演示页：多 Tab 同路由不同 query 时展示当前 query，用于验证页签与路由同步。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';

// 演示多开 Tab：读取当前路由 query
const route = useRoute();
const { routerPushByKey } = useRouterPush();

// 当前路由 query 的 JSON 展示（多 Tab 演示页）
const routeQuery = computed(() => JSON.stringify(route.query));
</script>

<template>
  <div>
    <!-- 功能演示页：multi-tab/index.vue -->
    <LookForward>
      <div>
        <AButton @click="routerPushByKey('function_tab')">{{ $t('page.function.multiTab.backTab') }}</AButton>
        <div class="py-24px">{{ $t('page.function.multiTab.routeParam') }}: {{ routeQuery }}</div>
      </div>
    </LookForward>
  </div>
</template>

<style scoped></style>
