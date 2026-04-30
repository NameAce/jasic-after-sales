<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getNotifyTodoPage } from '@/service/api';
import { useAuth } from '@/hooks/business/auth';
import { $t } from '@/locales';

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
// 动态列表加载态
const loading = ref(false);
// 首页展示的最新动态条目
const newses = ref<NewsItem[]>([]);
const { hasAuth } = useAuth();

/**
 * 作用：解析消息列表接口中的行数组。
 * @param data - 接口数据
 * @returns 行数组
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  if (Array.isArray(data?.rows)) return data.rows;
  return [];
}

/**
 * 作用：拼接标题与摘要为列表展示文案。
 * @param row - 原始行
 * @returns 展示字符串
 */
function buildContent(row: Record<string, any>) {
  const title = String(row?.title || '').trim();
  const summary = String(row?.summary || '').trim();
  if (title && summary) return `${title} - ${summary}`;
  return title || summary || '-';
}

/**
 * 作用：加载首页「最新动态」列表（来自消息历史）。
 * @param 无
 * @returns 返回 Promise，加载结束后结束
 */
async function loadNews() {
  loading.value = true;
  try {
    const res = await getNotifyTodoPage({ box: 'HISTORY', pageNum: 1, pageSize: 10 });
    const rows = pickRows(res.data);
    newses.value = rows.map((row: Record<string, any>) => ({
      id: row.id || row.messageId || `${row.bizType || ''}-${row.bizId || ''}`,
      content: buildContent(row),
      time: String(row.createTime || ''),
      workOrderId: hasAuth('workorder:list') ? Number(row.routeValue || row.bizId || 0) || undefined : undefined
    }));
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：点击动态项跳转工单或消息中心。
 * @param item - 列表项
 * @returns {void} 无
 */
function openNewsItem(item: NewsItem) {
  if (item.workOrderId) {
    router.push({ name: 'after-sales_work-order', query: { detailId: String(item.workOrderId), fromNotify: '1' } });
    return;
  }
  router.push({ path: '/notify', query: { box: 'HISTORY' } });
}

/**
 * 作用：挂载后拉取动态列表。
 * @param 无
 * @returns {void} 无
 */
onMounted(() => {
  loadNews();
});
</script>

<template>
  <ACard
    :title="$t('page.home.projectNews.title')"
    :bordered="false"
    size="small"
    class="card-wrapper"
    :loading="loading"
  >
    <AList :data-source="newses" :locale="{ emptyText: $t('page.home.projectNews.empty') }">
      <template #renderItem="{ item }">
        <AListItem class="cursor-pointer" @click="openNewsItem(item)">
          <AListItemMeta :title="item.content" :description="item.time">
            <template #avatar>
              <img
                src="/@fs/D:/companyProject/售后/jasic-after-sales/mp/aftersale/static/images/worker.png"
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
