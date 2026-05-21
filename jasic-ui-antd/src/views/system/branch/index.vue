<script setup lang="ts">
/**
 * 总部系统管理 — 网点管理：按承修方汇总展示工单数量（`GET /system/work-order/hq-site-summary`）。
 * 菜单 component 约定为 `system/branch/index`，由动态路由映射为 `system_branch` 视图。
 */
import { computed, onMounted, reactive, ref } from 'vue';
import { listHqSiteSummary } from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import BranchSiteOrdersDrawer from './branch-site-orders-drawer.vue';

type RowData = Record<string, any>;

const authStore = useAuthStore();
const pageMenuTitle = useRouteMenuTitle();

/** 当前登录总部公司 ID（服务端按此上下文注入 `hq-site-summary` 数据范围） */
const currentHqCompanyId = computed(() => {
  const id = Number(authStore.userInfo.currentCompanyId);
  return Number.isFinite(id) && id > 0 ? id : undefined;
});

const loading = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);
/** 接口一次返回的全量网点汇总，前端分页切片展示 */
const siteSummaryAllRows = ref<RowData[]>([]);

const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch
} = useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

/** 筛选参数与 `WorkOrderHqSiteSummaryQuery` 一致，仅支持 `siteName` 模糊查询 */
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  siteName: ''
});

const tableScrollMinX = computed(() => 880);
const { tableWrapperRef, scrollConfig } = useTableScroll(tableScrollMinX);

/** 承修方网点工单列表抽屉（点击承修网点名称打开） */
const siteOrdersDrawerRef = ref<InstanceType<typeof BranchSiteOrdersDrawer> | null>(null);

const columns = [
  {
    title: '承修网点',
    dataIndex: 'siteCompanyName',
    key: 'siteCompanyName',
    width: 220,
    ellipsis: true,
    align: 'left' as const
  },
  { title: '工单总量', dataIndex: 'totalCount', key: 'totalCount', width: 100, align: 'left' as const },
  { title: '待接单', dataIndex: 'waitAcceptCount', key: 'waitAcceptCount', width: 100, align: 'left' as const },
  { title: '维修中', dataIndex: 'inProgressCount', key: 'inProgressCount', width: 100, align: 'left' as const },
  { title: '已完成', dataIndex: 'completedCount', key: 'completedCount', width: 100, align: 'left' as const }
];

function pickRows(data: unknown) {
  if (Array.isArray(data)) return data;
  if (data && typeof data === 'object' && Array.isArray((data as { records?: unknown }).records)) {
    return (data as { records: RowData[] }).records;
  }
  return [];
}

/**
 * 作用：将全量汇总结果按当前页码、每页条数切片到表格数据源。
 */
function applyPagination() {
  const start = (query.pageNum - 1) * query.pageSize;
  rows.value = siteSummaryAllRows.value.slice(start, start + query.pageSize);
  total.value = siteSummaryAllRows.value.length;
}

/**
 * 作用：请求总部网点工单汇总；未选择总部公司时不发起请求。
 */
async function loadList() {
  clearListMsgs();
  if (!currentHqCompanyId.value) {
    rows.value = [];
    total.value = 0;
    siteSummaryAllRows.value = [];
    window.$message?.warning?.('请先选择总部公司后再查看网点');
    return;
  }

  loading.value = true;
  try {
    const flat = await listHqSiteSummary({
      siteName: query.siteName.trim() || undefined
    });
    if (consumeFlatError(flat)) {
      siteSummaryAllRows.value = [];
      rows.value = [];
      total.value = 0;
      return;
    }
    const data = (flat as { data?: unknown }).data;
    const list = Array.isArray(data) ? data : pickRows(data);
    siteSummaryAllRows.value = list.map((item: RowData) => ({
      siteCompanyId: item.siteCompanyId,
      siteCompanyName: item.siteCompanyName,
      totalCount: Number(item.totalCount ?? 0),
      waitAcceptCount: Number(item.waitAcceptCount ?? 0),
      inProgressCount: Number(item.inProgressCount ?? 0),
      completedCount: Number(item.completedCount ?? 0)
    }));
    applyPagination();
    refreshEmptySuccessMsg(flat, siteSummaryAllRows.value.length);
  } catch (e: unknown) {
    siteSummaryAllRows.value = [];
    rows.value = [];
    total.value = 0;
    setMsgFromCatch(e);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadList();
}

function handleReset() {
  query.siteName = '';
  handleSearch();
}

function tableRowKey(record: RowData) {
  return record.siteCompanyId ?? record.siteCompanyName ?? 'summary-row';
}

/**
 * 作用：打开指定承修网点的只读工单列表抽屉。
 */
function openSiteOrders(record: RowData) {
  siteOrdersDrawerRef.value?.open(record);
}

function tablePagination() {
  return {
    current: query.pageNum,
    pageSize: query.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
    onChange: (page: number, pageSize?: number) => {
      query.pageNum = page;
      if (pageSize) query.pageSize = pageSize;
      applyPagination();
    }
  };
}

onMounted(() => {
  loadList();
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <AForm :model="query" :label-col="{ span: 5, md: 7 }" class="mb-0">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="网点名称" class="m-0">
                  <AInput
                    v-model:value="query.siteName"
                    allow-clear
                    placeholder="承修方公司名称"
                    @press-enter="handleSearch"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleSearch">查询</AButton>
            <AButton :loading="loading" @click="handleReset">重置</AButton>
          </div>
        </div>
      </AForm>
    </ACard>

    <ACard
      :title="pageMenuTitle"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        class="h-full"
        :pagination="tablePagination()"
        :row-key="tableRowKey"
        size="small"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'siteCompanyName'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openSiteOrders(record)"
              @keydown.enter.prevent="openSiteOrders(record)"
            >
              {{ record.siteCompanyName || '-' }}
            </span>
          </template>
        </template>
      </ATable>
    </ACard>

    <BranchSiteOrdersDrawer ref="siteOrdersDrawerRef" />
  </div>
</template>
