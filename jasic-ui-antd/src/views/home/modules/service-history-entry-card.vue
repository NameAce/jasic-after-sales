<script setup lang="ts">
/**
 * 服务主体首页历史参与入口：样式对齐 git「通知动态」(project-news)，
 * 使用 ACard + AList + 头像列表项，整行点击跳转 viewScope=HISTORY 工单列表。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { $t } from '@/locales';
import { navigateHomeRoute } from '../composables/home-route-helpers';
import { useServiceDashboard } from '../composables/use-service-dashboard';

defineOptions({
  name: 'ServiceHistoryEntryCard'
});

interface HistoryListItem {
  id: string;
  title: string;
  description: string;
}

const router = useRouter();
const { loading, historyEntry } = useServiceDashboard();

const entry = computed(() => historyEntry.value);

/** 将 historyEntry 转为与通知动态一致的列表数据源（主行展示说明文案，当前为单条入口） */
const listItems = computed<HistoryListItem[]>(() => {
  const row = entry.value;
  if (!row) return [];
  const content = String(row.description || '查看当前服务公司历史参与但当前不再承接的工单').trim();
  return [
    {
      id: 'history-entry',
      title: content || '-',
      description: '点击进入历史参与工单列表'
    }
  ];
});

/**
 * 作用：点击列表项，按后端 routeTarget 进入历史参与工单列表。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openHistoryItem() {
  navigateHomeRoute(router, entry.value?.routeTarget);
}
</script>

<template>
  <!-- 历史工单入口卡片 -->
  <ACard :title="entry?.title || '历史参与'" :bordered="false" size="small" class="card-wrapper" :loading="loading">
    <AList :data-source="listItems" :locale="{ emptyText: $t('page.home.projectNews.empty') }">
      <template #renderItem="{ item }">
        <AListItem class="cursor-pointer" @click="openHistoryItem">
          <AListItemMeta :title="item.title" :description="item.description">
            <template #avatar>
              <img
                src="https://jasic-after.oss-cn-shenzhen.aliyuncs.com/uniapp/contractor/worker.png"
                class="size-48px rd-1/2 object-cover"
              />
            </template>
          </AListItemMeta>
        </AListItem>
      </template>
    </AList>
  </ACard>
</template>

<style scoped></style>
