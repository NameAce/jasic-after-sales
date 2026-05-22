<script setup lang="ts">
/**
 * 站内通知：待办与历史分页列表、角标数量与标记已读（对接 notify 接口）。
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
 * 作用：返回分页初始状态对象。
 * @param 无
 * @returns 含 pageNum、pageSize 的对象
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
 * 作用：从分页响应中取出消息行数组。
 * @param data - 接口数据
 * @returns 行数组
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 作用：请求待办数量并更新 todoCount。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
async function refreshTodoCount() {
  const { data } = await getNotifyTodoCount();
  todoCount.value = Number(data?.count || 0);
}

/**
 * 作用：按当前 Tab 与分页拉取消息列表。
 * @param 无
 * @returns 返回 Promise，列表加载结束后结束
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
 * 作用：并行刷新待办数与当前列表。
 * @param 无
 * @returns 返回 Promise，全部完成后结束
 */
async function loadPage() {
  await Promise.all([refreshTodoCount(), loadList()]);
}

/**
 * 作用：将待办状态码转为中文展示文案。
 * @param status - 状态字符串
 * @returns 展示文案或原值
 */
function statusLabel(status: string | undefined) {
  if (!status) return '-';
  return STATUS_META[status]?.label ?? status;
}

/**
 * 作用：将待办状态码映射为 Tag 颜色。
 * @param status - 状态字符串
 * @returns Ant Design Tag 颜色名
 */
function statusColor(status: string | undefined) {
  if (!status) return 'default';
  return STATUS_META[status]?.color ?? 'default';
}

/**
 * 作用：切换待处理/历史 Tab 并刷新列表与角标。
 * @param key - Tab key
 * @returns {void} 无
 */
function handleTabChange(key: string | number) {
  activeTab.value = String(key) as TabKey;
  loadList();
  refreshTodoCount();
}

/**
 * 作用：分页变更时更新当前 Tab 的 pageNum/pageSize（改 pageSize 时回到第 1 页）。
 * @param page - 当前页码
 * @param pageSize - 每页条数，可选
 * @returns {void} 无
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
 * 作用：将单条消息标为已读并刷新列表。
 * @param row - 表格行
 * @returns 返回 Promise，刷新完成后结束
 */
async function markRead(row: RowData) {
  const id = row.messageId ?? row.id;
  if (!id) return;
  await markNotifyMessageRead(id);
  await loadPage();
}

/**
 * 作用：从消息行解析可跳转的工单 ID（优先 routeValue，其次 bizId）。
 * @param row - 表格行
 * @returns 有效工单 ID 或 null
 */
function resolveWorkOrderId(row: RowData) {
  const routeValueId = Number(row?.routeValue);
  if (Number.isFinite(routeValueId) && routeValueId > 0) return routeValueId;

  const bizId = Number(row?.bizId);
  if (Number.isFinite(bizId) && bizId > 0) return bizId;

  return null;
}

/**
 * 作用：点击消息跳转工单详情；待处理时先标记已读。
 * @param row - 表格行
 * @returns 返回 Promise，跳转与刷新完成后结束
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
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :title="pageMenuTitle" :bordered="false" class="flex-col-stretch card-wrapper sm:flex-1-hidden">
      <div class="mb-12px">待办通知数：{{ todoCount }}</div>
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
