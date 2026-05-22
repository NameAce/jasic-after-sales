<script setup lang="ts">
/**
 * 网点管理 — 承修方网点工单只读列表抽屉（`GET /system/work-order/hq-site-orders`）。
 * 由网点汇总表点击承修网点名称打开，支持状态筛选与工单号/客户/条码关键词检索。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, reactive, ref, watch } from 'vue';
import { workOrderMainStatusTagColor } from '@/constants/list-status-tag';
import type { WorkOrderHqSiteOrdersDisplayStatus, WorkOrderHqSiteOrdersQuery } from '@/service/api';
import { listHqSiteOrders } from '@/service/api';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { applyDateTimeColumnRender } from '@/utils/datetime';
import WorkOrderDetailDrawer from '@/views/work-order/components/WorkOrderDetailDrawer.vue';

type RowData = Record<string, any>;

const drawerOpen = ref(false);
const siteCompanyId = ref<number | undefined>();
const siteCompanyName = ref('');

const loading = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);

const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch
} = useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

const displayStatus = ref<WorkOrderHqSiteOrdersDisplayStatus>('ALL');
const searchKeyword = ref('');

const orderQuery = reactive({
  pageNum: 1,
  pageSize: 10
});

const statusSegmentOptions = [
  { label: '全部', value: 'ALL' as const },
  { label: '待接单', value: 'WAIT_ACCEPT' as const },
  { label: '维修中', value: 'IN_PROGRESS' as const },
  { label: '已完成', value: 'COMPLETED' as const },
  { label: '已关闭', value: 'CLOSED' as const }
];

const drawerTitle = computed(() => {
  const name = siteCompanyName.value || '承修网点';
  return `${name} — 工单列表`;
});

const tableScrollMinX = computed(() => 980);
const { scrollConfig } = useTableScroll(tableScrollMinX);

const rawColumns = [
  { title: '工单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 120, ellipsis: true },
  { title: '客户手机号', dataIndex: 'customerMobile', key: 'customerMobile', width: 120 },
  { title: '条码', dataIndex: 'barcode', key: 'barcode', width: 140, ellipsis: true },
  { title: '机型', dataIndex: 'productModel', key: 'productModel', width: 120, ellipsis: true },
  { title: '状态', dataIndex: 'mainStatusLabel', key: 'mainStatusLabel', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
];
const columns = applyDateTimeColumnRender(rawColumns);

const detailOpen = ref(false);
const detailWorkOrderId = ref<number | null>(null);

/**
 * 作用：从分页接口响应解析列表数组。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickRows(data: unknown) {
  if (Array.isArray(data)) return data;
  if (data && typeof data === 'object' && Array.isArray((data as { records?: unknown }).records)) {
    return (data as { records: RowData[] }).records;
  }
  return [];
}

/**
 * 作用：从分页接口响应解析总条数。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickTotal(data: unknown) {
  if (data && typeof data === 'object' && 'total' in data) {
    return Number((data as { total?: unknown }).total) || 0;
  }
  return 0;
}

/**
 * 作用：应用配置或路由参数：applySearchKeywordToParams。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applySearchKeywordToParams(params: WorkOrderHqSiteOrdersQuery) {
  const q = searchKeyword.value.trim();
  if (!q) return;
  if (/[\u4E00-\u9FFF]/.test(q)) {
    params.customerName = q;
    return;
  }
  if (/^\d{8,}$/.test(q)) {
    params.barcode = q;
    return;
  }
  params.orderNo = q;
}

/**
 * 作用：加载数据：loadOrderList。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadOrderList() {
  const siteId = siteCompanyId.value;
  if (!siteId) {
    rows.value = [];
    total.value = 0;
    return;
  }

  clearListMsgs();
  loading.value = true;
  try {
    const params: WorkOrderHqSiteOrdersQuery = {
      siteCompanyId: siteId,
      displayStatus: displayStatus.value,
      pageNum: orderQuery.pageNum,
      pageSize: orderQuery.pageSize
    };
    applySearchKeywordToParams(params);

    const flat = await listHqSiteOrders(params);
    if (consumeFlatError(flat)) {
      rows.value = [];
      total.value = 0;
      return;
    }
    const data = (flat as { data?: unknown }).data;
    rows.value = pickRows(data);
    total.value = pickTotal(data);
    refreshEmptySuccessMsg(flat, rows.value.length);
  } catch (e: unknown) {
    rows.value = [];
    total.value = 0;
    setMsgFromCatch(e);
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：执行查询（回到第一页）：handleSearch。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleSearch() {
  orderQuery.pageNum = 1;
  loadOrderList();
}

/**
 * 作用：执行查询（回到第一页）：handleResetSearch。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleResetSearch() {
  searchKeyword.value = '';
  handleSearch();
}

/**
 * 作用：处理交互事件：handleStatusChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleStatusChange(val: string | number) {
  displayStatus.value = String(val) as WorkOrderHqSiteOrdersDisplayStatus;
  orderQuery.pageNum = 1;
  loadOrderList();
}

/**
 * 作用：处理交互事件：handleTableChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleTableChange(page: number, pageSize?: number) {
  orderQuery.pageNum = page;
  if (pageSize) orderQuery.pageSize = pageSize;
  loadOrderList();
}

/**
 * 作用：页面内业务方法：openOrderDetail。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openOrderDetail(row: RowData) {
  const wid = Number(row.id);
  if (!Number.isFinite(wid) || wid <= 0) return;
  detailWorkOrderId.value = wid;
  detailOpen.value = true;
}

/**
 * 作用：页面内业务方法：open。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function open(record: RowData) {
  const siteId = Number(record.siteCompanyId);
  if (!Number.isFinite(siteId) || siteId <= 0) {
    window.$message?.warning?.('网点信息无效，无法查看工单');
    return;
  }
  siteCompanyId.value = siteId;
  siteCompanyName.value = String(record.siteCompanyName || '').trim();
  displayStatus.value = 'ALL';
  searchKeyword.value = '';
  orderQuery.pageNum = 1;
  drawerOpen.value = true;
}

watch(drawerOpen, openVal => {
  if (openVal) {
    loadOrderList();
  } else {
    rows.value = [];
    total.value = 0;
    detailOpen.value = false;
    detailWorkOrderId.value = null;
  }
});

defineExpose({ open });
</script>

<template>
  <ADrawer v-model:open="drawerOpen" :title="drawerTitle" :width="1100" destroy-on-close>
    <!-- 网点工单抽屉：按主状态 Segmented + 关键词筛选该网点下工单列表 -->
    <div class="flex-col-stretch gap-12px">
      <ASegmented :value="displayStatus" :options="statusSegmentOptions" @change="handleStatusChange" />
      <div class="page-search-toolbar">
        <div class="page-search-toolbar__filters">
          <AInput
            v-model:value="searchKeyword"
            allow-clear
            class="max-w-360px"
            placeholder="搜索工单号、客户姓名或条码"
            @press-enter="handleSearch"
          />
        </div>
        <div class="page-search-toolbar__actions">
          <AButton type="primary" :loading="loading" @click="handleSearch">查询</AButton>
          <AButton :loading="loading" @click="handleResetSearch">重置</AButton>
        </div>
      </div>
      <ATable
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        :pagination="{
          current: orderQuery.pageNum,
          pageSize: orderQuery.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handleTableChange
        }"
        row-key="id"
        size="small"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openOrderDetail(record)"
              @keydown.enter.prevent="openOrderDetail(record)"
            >
              {{ record.orderNo || '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'mainStatusLabel'">
            <ATag :color="workOrderMainStatusTagColor(String(record.mainStatus || ''))">
              {{ record.mainStatusLabel || record.mainStatus || '-' }}
            </ATag>
          </template>
        </template>
      </ATable>
    </div>
    <WorkOrderDetailDrawer :open="detailOpen" :work-order-id="detailWorkOrderId" @update:open="v => (detailOpen = v)" />
  </ADrawer>
</template>
