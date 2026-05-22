<script setup lang="ts">
/**
 * 遗留业务首页项目动态列表（latestHistoryTodos，可深链工单详情）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '@/hooks/business/auth';
import { formatDateTime } from '@/utils/datetime';
import { $t } from '@/locales';
import { useBusinessHomeDashboard } from '../composables/use-business-home-dashboard';

defineOptions({
  name: 'ProjectNews'
});

interface NewsItem {
  id: number | string;
  content: string;
  time: string;
  workOrderId?: number;
}

const router = useRouter();
const { hasAuth } = useAuth();
const { loading, latestHistoryTodos } = useBusinessHomeDashboard();

/**
 * 作用：拼接标题与摘要为列表展示文案。
 * @param row - 动态行
 * @returns 展示字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildContent(row: { title?: string; summary?: string }) {
  const title = String(row?.title || '').trim();
  const summary = String(row?.summary || '').trim();
  if (title && summary) return `${title} - ${summary}`;
  return title || summary || '-';
}

/** 首页最新动态（来自首页接口 latestHistoryTodos） */
const newses = computed<NewsItem[]>(() =>
  latestHistoryTodos.value.map(row => ({
    id: row.id || `${row.bizType || ''}-${row.bizId || ''}`,
    content: buildContent(row),
    time: formatDateTime(row.createTime, ''),
    workOrderId: hasAuth('workorder:list') ? Number(row.routeValue || row.bizId || 0) || undefined : undefined
  }))
);

/**
 * 作用：点击动态项跳转工单详情或消息中心历史箱。
 * @param item - 列表项
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openNewsItem(item: NewsItem) {
  if (item.workOrderId) {
    router.push({
      name: 'after-sales_work-order',
      query: { detailId: String(item.workOrderId), fromNotify: '1' }
    });
    return;
  }
  router.push({ path: '/notify', query: { box: 'HISTORY' } });
}
</script>

<template>
    <ACard
    :title="$t('page.home.projectNews.title')"
    :bordered="false"
    size="small"
    class="card-wrapper"
    :loading="loading"
  >
      <!-- 项目动态 -->
    <AList :data-source="newses" :locale="{ emptyText: $t('page.home.projectNews.empty') }">
      <template #renderItem="{ item }">
        <AListItem class="cursor-pointer" @click="openNewsItem(item)">
          <AListItemMeta :title="item.content" :description="item.time">
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
