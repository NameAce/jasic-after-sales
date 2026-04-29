<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getNotifyTodoCount, getNotifyTodoPage, markNotifyMessageRead } from '@/service/api';

type RowData = Record<string, any>;
type TabKey = 'TODO' | 'HISTORY';

const loading = ref(false);
const todoCount = ref(0);
const listTotal = ref(0);
const rows = ref<RowData[]>([]);

const activeTab = ref<TabKey>('TODO');
const router = useRouter();
const route = useRoute();

function defaultPageState() {
  return { pageNum: 1, pageSize: 10 };
}

const pageState = reactive<Record<TabKey, { pageNum: number; pageSize: number }>>({
  TODO: defaultPageState(),
  HISTORY: defaultPageState()
});

const STATUS_META: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'warning' },
  READ: { label: '已读', color: 'default' },
  DONE: { label: '已处理', color: 'success' },
  INVALID: { label: '已失效', color: 'error' }
};

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true, width: 200 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', ellipsis: true, minWidth: 200 },
  { title: '工单号', dataIndex: 'bizNo', key: 'bizNo', width: 160, ellipsis: true },
  { title: '状态', dataIndex: 'todoStatus', key: 'todoStatus', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', dataIndex: 'actions', key: 'actions', width: 100 }
];

function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

async function refreshTodoCount() {
  const { data } = await getNotifyTodoCount();
  todoCount.value = Number(data?.count || 0);
}

async function loadList() {
  loading.value = true;
  const ps = pageState[activeTab.value];
  try {
    const pageRes = await getNotifyTodoPage({
      box: activeTab.value,
      pageNum: ps.pageNum,
      pageSize: ps.pageSize
    });
    const body = pageRes.data;
    rows.value = pickRows(body);
    listTotal.value = Number(body?.total ?? 0);
  } finally {
    loading.value = false;
  }
}

async function loadPage() {
  await Promise.all([refreshTodoCount(), loadList()]);
}

function statusLabel(status: string | undefined) {
  if (!status) return '-';
  return STATUS_META[status]?.label ?? status;
}

function statusColor(status: string | undefined) {
  if (!status) return 'default';
  return STATUS_META[status]?.color ?? 'default';
}

function handleTabChange(key: string | number) {
  activeTab.value = String(key) as TabKey;
  loadList();
  refreshTodoCount();
}

/** 与 ant-design-vue Pagination：改每页条数时回到第 1 页 */
function handleTableChange(page: number, pageSize?: number) {
  const ps = pageState[activeTab.value];
  if (pageSize !== undefined && pageSize !== ps.pageSize) {
    ps.pageSize = pageSize;
    ps.pageNum = 1;
  } else {
    ps.pageNum = page;
    if (pageSize !== undefined) ps.pageSize = pageSize;
  }
  loadList();
}

async function markRead(row: RowData) {
  const id = row.messageId ?? row.id;
  if (!id) return;
  await markNotifyMessageRead(id);
  await loadPage();
}

function resolveWorkOrderId(row: RowData) {
  const routeValueId = Number(row?.routeValue);
  if (Number.isFinite(routeValueId) && routeValueId > 0) return routeValueId;

  const bizId = Number(row?.bizId);
  if (Number.isFinite(bizId) && bizId > 0) return bizId;

  return null;
}

async function openMessage(row: RowData) {
  const workOrderId = resolveWorkOrderId(row);
  if (!workOrderId) {
    window.$message?.warning('当前消息缺少工单跳转信息');
    return;
  }

  const jump = () =>
    router.push({
      name: 'after-sales_work-order',
      query: { detailId: String(workOrderId), fromNotify: '1' }
    });

  if (row.todoStatus !== 'PENDING') {
    await jump();
    return;
  }

  const id = row.messageId ?? row.id;
  if (id) await markNotifyMessageRead(id);
  await jump();
  await loadPage();
}

onMounted(() => {
  const box = String(route.query.box || '').toUpperCase();
  if (box === 'HISTORY') {
    activeTab.value = 'HISTORY';
  } else if (box === 'TODO') {
    activeTab.value = 'TODO';
  }
  loadPage();
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard title="消息中心" :bordered="false" class="flex-col-stretch card-wrapper sm:flex-1-hidden">
      <div class="mb-12px">待办通知数：{{ todoCount }}</div>
      <ATabs :active-key="activeTab" size="small" class="mb-12px" @change="handleTabChange">
        <ATabPane key="TODO" tab="待处理" />
        <ATabPane key="HISTORY" tab="历史记录" />
      </ATabs>
      <ATable
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="{
          current: pageState[activeTab].pageNum,
          pageSize: pageState[activeTab].pageSize,
          total: listTotal,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handleTableChange
        }"
        row-key="id"
        size="small"
        :custom-row="record => ({ onClick: () => openMessage(record) })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'todoStatus'">
            <ATag :color="statusColor(record.todoStatus)">{{ statusLabel(record.todoStatus) }}</ATag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <AButton
              v-if="record.todoStatus === 'PENDING'"
              type="link"
              size="small"
              class="table-action-link--success"
              @click.stop="markRead(record)"
            >
              标记已读
            </AButton>
            <span v-else>-</span>
          </template>
        </template>
      </ATable>
    </ACard>
  </div>
</template>
