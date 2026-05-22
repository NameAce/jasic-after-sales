<script setup lang="ts">
/**
 * 站内通知：待办与历史分页列表、角标数量与标记已读（对接 notify 接口）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getNotifyTodoCount, getNotifyTodoPage, markNotifyMessageRead } from '@/service/api';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { createAntTableActionColumn } from '@/utils/table-action-width';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { applyDateTimeColumnRender } from '@/utils/datetime';
import { readRouteQueryString, useRouteQueryFilterSync } from '@/utils/route-query-filter-sync';

type RowData = Record<string, any>;
type TabKey = 'TODO' | 'HISTORY';

/** 操作列：待办行「标记已读」单按钮横排估算 */

// 列表加载中
const loading = ref(false);
// 待办数量角标数据
const todoCount = ref(0);
// 当前 Tab 列表总条数
const listTotal = ref(0);
// 当前页表格行
const rows = ref<RowData[]>([]);

const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch
} = useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

// 当前消息箱 Tab（待处理/历史）
const activeTab = ref<TabKey>('TODO');
const router = useRouter();
const route = useRoute();
const pageMenuTitle = useRouteMenuTitle();

/**
 * 作用：页面内业务方法：defaultPageState。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function defaultPageState() {
  return { pageNum: 1, pageSize: 10 };
}

// 各 Tab 独立分页状态
const pageState = reactive<Record<TabKey, { pageNum: number; pageSize: number }>>({
  TODO: defaultPageState(),
  HISTORY: defaultPageState()
});

// 待办/消息状态与 Tag 展示元数据
const STATUS_META: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待处理', color: 'warning' },
  READ: { label: '已读', color: 'default' },
  DONE: { label: '已处理', color: 'success' },
  INVALID: { label: '已失效', color: 'error' }
};

// 消息中心表格列配置
const columns = applyDateTimeColumnRender([
  {
    title: '标题',
    dataIndex: 'title',
    key: 'title',
    ellipsis: true,
    width: 200
  },
  {
    title: '摘要',
    dataIndex: 'summary',
    key: 'summary',
    ellipsis: true,
    minWidth: 200
  },
  {
    title: '工单号',
    dataIndex: 'bizNo',
    key: 'bizNo',
    width: 160,
    ellipsis: true
  },
  { title: '状态', dataIndex: 'todoStatus', key: 'todoStatus', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  createAntTableActionColumn({ fixed: false, dataIndex: 'actions', width: 100 })
]);

/**
 * 作用：从分页接口响应解析列表数组。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 作用：页面内业务方法：refreshTodoCount。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function refreshTodoCount() {
  const { data } = await getNotifyTodoCount();
  todoCount.value = Number(data?.count || 0);
}

/**
 * 作用：加载当前 Tab/条件下表格数据。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadList() {
  clearListMsgs();
  loading.value = true;
  const ps = pageState[activeTab.value];
  try {
    const pageRes = await getNotifyTodoPage({
      box: activeTab.value,
      pageNum: ps.pageNum,
      pageSize: ps.pageSize
    });
    if (consumeFlatError(pageRes)) {
      rows.value = [];
      listTotal.value = 0;
      return;
    }
    const body = pageRes.data;
    rows.value = pickRows(body);
    listTotal.value = Number(body?.total ?? 0);
    refreshEmptySuccessMsg(pageRes, rows.value.length);
  } catch (e: unknown) {
    rows.value = [];
    listTotal.value = 0;
    setMsgFromCatch(e);
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：加载数据：loadPage。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadPage() {
  await Promise.all([refreshTodoCount(), loadList()]);
}

/**
 * 作用：页面内业务方法：statusLabel。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function statusLabel(status: string | undefined) {
  if (!status) return '-';
  return STATUS_META[status]?.label ?? status;
}

/**
 * 作用：页面内业务方法：statusColor。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function statusColor(status: string | undefined) {
  if (!status) return 'default';
  return STATUS_META[status]?.color ?? 'default';
}

/**
 * 作用：处理交互事件：handleTabChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleTabChange(key: string | number) {
  activeTab.value = String(key) as TabKey;
  loadList();
  refreshTodoCount();
}

/**
 * 作用：处理交互事件：handleTableChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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

/**
 * 作用：页面内业务方法：markRead。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function markRead(row: RowData) {
  const id = row.messageId ?? row.id;
  if (!id) return;
  await markNotifyMessageRead(id);
  await loadPage();
}

/**
 * 作用：页面内业务方法：resolveWorkOrderId。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveWorkOrderId(row: RowData) {
  const routeValueId = Number(row?.routeValue);
  if (Number.isFinite(routeValueId) && routeValueId > 0) return routeValueId;

  const bizId = Number(row?.bizId);
  if (Number.isFinite(bizId) && bizId > 0) return bizId;

  return null;
}

/**
 * 作用：页面内业务方法：openMessage。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
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

/** 首页通知动态等入口：按 query.box 回显待办/历史 Tab */
/**
 * 作用：应用配置或路由参数：applyFiltersFromRouteQuery。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyFiltersFromRouteQuery() {
  if (!('box' in route.query)) return;
  const box = readRouteQueryString(route.query, 'box').toUpperCase();
  if (box === 'HISTORY') activeTab.value = 'HISTORY';
  else if (box === 'TODO') activeTab.value = 'TODO';
}

useRouteQueryFilterSync({
  apply: applyFiltersFromRouteQuery,
  reload: loadPage,
  watchQueryKeys: ['box']
});
</script>

<template>
  <!-- 站内通知：待办/历史 Tab、角标数量；行点击跳转业务，待办可标记已读 -->
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :title="pageMenuTitle" :bordered="false" class="flex-col-stretch card-wrapper sm:flex-1-hidden">
      <div class="mb-12px">待办通知数：{{ todoCount }}</div>
      <!-- 待处理 vs 历史记录：分页状态按 Tab 独立维护 -->
      <ATabs :active-key="activeTab" size="small" class="mb-12px" @change="handleTabChange">
        <ATabPane key="TODO" tab="待处理" />
        <ATabPane key="HISTORY" tab="历史记录" />
      </ATabs>
      <ATable
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        :scroll="{ x: 'max-content' }"
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
            <APopconfirm
              v-if="record.todoStatus === 'PENDING'"
              title="确认将本条标记为已读？"
              @confirm="markRead(record)"
            >
              <AButton type="link" size="small" class="table-action-link--success" @click.stop="() => {}">
                标记已读
              </AButton>
            </APopconfirm>
            <span v-else>-</span>
          </template>
        </template>
      </ATable>
    </ACard>
  </div>
</template>
