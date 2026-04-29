<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { transformColorWithOpacity } from '@sa/utils';
import { tagColorTransferTransferred, workOrderMainStatusTagColor } from '@/constants/list-status-tag';
import { countWorkOrderStatus, listWorkOrder } from '@/service/api';
import type { WorkOrderQuery, WorkOrderStatusCountQuery } from '@/service/api';
import { useThemeStore } from '@/store/modules/theme';
import { useTableScroll } from '@/hooks/common/table';
import { useAuth } from '@/hooks/business/auth';
import WorkOrderCreateModals from './components/WorkOrderCreateModals.vue';
import WorkOrderDetailDrawer from './components/WorkOrderDetailDrawer.vue';
import { getRowPrimaryActions } from './list-actions';

type RowData = Record<string, any>;

const { tableWrapperRef, scrollConfig } = useTableScroll(1500);
const { hasAuth } = useAuth();

type MainStatusTab = 'ALL' | 'PENDING_ASSIGN' | 'PENDING_TECH_ACCEPT' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';

const loading = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  viewScope: 'CURRENT' as WorkOrderQuery['viewScope'],
  orderNo: '',
  customerName: '',
  customerMobile: '',
  barcode: '',
  mainStatus: '' as string,
  hasTransfer: undefined as undefined | 0 | 1
});

const statusCountMap = ref<Record<string, number>>({
  ALL: 0,
  PENDING_ASSIGN: 0,
  PENDING_TECH_ACCEPT: 0,
  IN_PROGRESS: 0,
  COMPLETED: 0,
  CLOSED: 0
});

const hasTransferOptions = [
  { label: '是', value: 1 },
  { label: '否', value: 0 }
];

const createModalsRef = ref<InstanceType<typeof WorkOrderCreateModals> | null>(null);
const route = useRoute();
const router = useRouter();
const themeStore = useThemeStore();
const VIEW_SCOPE_SET = new Set(['CURRENT', 'HISTORY', 'ALL']);
const MAIN_STATUS_SET = new Set(['PENDING_ASSIGN', 'PENDING_TECH_ACCEPT', 'IN_PROGRESS', 'COMPLETED', 'CLOSED']);

/** 与顶部 PageTab `chrome-tab_active` / `chrome-tab_active_dark` 下 `.chrome-tab__bg` 的 fill 色一致（createTabCssVars：primaryColor1 / primaryColor2） */
const workOrderStatusSegmentedThumbBg = computed(() => {
  const primary = themeStore.themeColors.primary;
  return themeStore.darkMode
    ? transformColorWithOpacity(primary, 0.3, '#000000')
    : transformColorWithOpacity(primary, 0.1, '#ffffff');
});

const detailOpen = ref(false);
const detailWorkOrderId = ref<number | null>(null);
const detailDrawerRef = ref<InstanceType<typeof WorkOrderDetailDrawer> | null>(null);

const activeMainStatus = computed({
  get: () => (query.mainStatus ? query.mainStatus : 'ALL') as MainStatusTab,
  set: (v: MainStatusTab) => {
    query.mainStatus = v === 'ALL' ? '' : v;
  }
});

const statusTabOptions = computed(() => [
  { value: 'ALL' as const, label: '全部', count: statusCountMap.value.ALL || 0 },
  { value: 'PENDING_ASSIGN' as const, label: '待派单', count: statusCountMap.value.PENDING_ASSIGN || 0 },
  { value: 'PENDING_TECH_ACCEPT' as const, label: '待接单', count: statusCountMap.value.PENDING_TECH_ACCEPT || 0 },
  { value: 'IN_PROGRESS' as const, label: '维修中', count: statusCountMap.value.IN_PROGRESS || 0 },
  { value: 'COMPLETED' as const, label: '已完成', count: statusCountMap.value.COMPLETED || 0 },
  { value: 'CLOSED' as const, label: '已关闭', count: statusCountMap.value.CLOSED || 0 }
]);

const statusSegmentOptions = computed(() =>
  statusTabOptions.value.map(item => ({
    label: `${item.label}（${item.count}）`,
    value: item.value
  }))
);

/** 当前列表页至少一条存在可操作按钮时，才展示「操作」列 */
const hasAnyRowActionButtons = computed(() => rows.value.some(row => getRowPrimaryActions(row).length > 0));

const columns = computed(() => {
  const baseColumns: any[] = [
    { title: '工单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
    { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
    { title: '客户手机号', dataIndex: 'customerMobile', key: 'customerMobile', width: 120 },
    { title: '条码', dataIndex: 'barcode', key: 'barcode', width: 140, ellipsis: true },
    { title: '机型', dataIndex: 'productModel', key: 'productModel', width: 140, ellipsis: true },
    { title: '状态', dataIndex: 'mainStatusLabel', key: 'mainStatusLabel', width: 120 },
    {
      title: '当前受理公司',
      dataIndex: 'currentAcceptCompanyName',
      key: 'currentAcceptCompanyName',
      width: 180,
      ellipsis: true
    },
    { title: '网点电话', dataIndex: 'currentAcceptCompanyPhone', key: 'currentAcceptCompanyPhone', width: 140 },
    { title: '当前维修员', dataIndex: 'assignedUserName', key: 'assignedUserName', width: 140 },
    { title: '转单', dataIndex: 'hasTransfer', key: 'hasTransfer', width: 90 },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 }
  ];
  if (hasAnyRowActionButtons.value) {
    baseColumns.push({
      title: '操作',
      dataIndex: 'actions',
      key: 'actions',
      width: 200,
      fixed: 'right' as const
    });
  }
  return baseColumns;
});

function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

function pickTotal(data: any, fallback: number) {
  const value = data?.total ?? data?.count ?? fallback;
  return Number(value) || fallback;
}

/** 与 jasic-ui `buildStatusCountParams` 一致：统计接口不带 mainStatus、不带分页 */
function buildStatusCountParams(): WorkOrderStatusCountQuery {
  return {
    viewScope: query.viewScope,
    orderNo: query.orderNo || undefined,
    customerName: query.customerName || undefined,
    customerMobile: query.customerMobile || undefined,
    barcode: query.barcode || undefined,
    hasTransfer: query.hasTransfer
  };
}

function buildListParams(): WorkOrderQuery {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    viewScope: query.viewScope,
    orderNo: query.orderNo || undefined,
    customerName: query.customerName || undefined,
    customerMobile: query.customerMobile || undefined,
    barcode: query.barcode || undefined,
    mainStatus: (query.mainStatus || undefined) as WorkOrderQuery['mainStatus'],
    hasTransfer: query.hasTransfer
  };
}

function syncStatusCountMap(list: unknown) {
  const next: Record<string, number> = {
    ALL: 0,
    PENDING_ASSIGN: 0,
    PENDING_TECH_ACCEPT: 0,
    IN_PROGRESS: 0,
    COMPLETED: 0,
    CLOSED: 0
  };
  if (!Array.isArray(list)) {
    statusCountMap.value = next;
    return;
  }
  for (const item of list as any[]) {
    if (item?.mainStatus) {
      next[item.mainStatus] = Number(item.countNum || 0);
    }
  }
  statusCountMap.value = next;
}

async function loadData() {
  loading.value = true;
  try {
    const [listRes, countRes] = await Promise.all([
      listWorkOrder(buildListParams()),
      countWorkOrderStatus(buildStatusCountParams())
    ]);
    const page = listRes.data as any;
    rows.value = pickRows(page);
    total.value = pickTotal(page, rows.value.length);
    syncStatusCountMap(countRes.data);
  } finally {
    loading.value = false;
  }
}

function trimSearchFields() {
  query.orderNo = query.orderNo.trim();
  query.customerName = query.customerName.trim();
  query.customerMobile = query.customerMobile.trim();
  query.barcode = query.barcode.trim();
}

function handleSearch() {
  trimSearchFields();
  query.pageNum = 1;
  loadData();
}

function handleScopeChange() {
  query.pageNum = 1;
  loadData();
}

function handleMainStatusChange() {
  query.pageNum = 1;
  loadData();
}

function resetQuery() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.viewScope = 'CURRENT';
  query.orderNo = '';
  query.customerName = '';
  query.customerMobile = '';
  query.barcode = '';
  query.mainStatus = '';
  query.hasTransfer = undefined;
  loadData();
}

/** 与 ant-design-vue Pagination 行为一致：改每页条数时回到第 1 页 */
function handleTableChange(page: number, pageSize?: number) {
  if (pageSize !== undefined && pageSize !== query.pageSize) {
    query.pageSize = pageSize;
    query.pageNum = 1;
  } else {
    query.pageNum = page;
    if (pageSize !== undefined) query.pageSize = pageSize;
  }
  loadData();
}

function openDetail(row: RowData) {
  const wid = Number(row.id);
  if (!Number.isFinite(wid)) return;
  detailWorkOrderId.value = wid;
  detailOpen.value = true;
}

async function handleListAction(row: RowData, action: string) {
  const wid = Number(row.id);
  if (!Number.isFinite(wid) || !action) return;
  detailWorkOrderId.value = wid;
  detailOpen.value = true;
  await nextTick();
  await detailDrawerRef.value?.openActionFromList(action);
}

function openDetailByRouteQuery() {
  const detailId = Number(route.query.detailId);
  if (!Number.isFinite(detailId) || detailId <= 0) return;

  detailWorkOrderId.value = detailId;
  detailOpen.value = true;

  const nextQuery = { ...route.query };
  delete nextQuery.detailId;
  delete nextQuery.fromNotify;
  router.replace({ query: nextQuery });
}

function applyFiltersFromRouteQuery() {
  const routeViewScope = String(route.query.viewScope || '').toUpperCase();
  const routeMainStatus = String(route.query.mainStatus || '').toUpperCase();

  query.viewScope = (VIEW_SCOPE_SET.has(routeViewScope) ? routeViewScope : 'CURRENT') as WorkOrderQuery['viewScope'];
  query.mainStatus = MAIN_STATUS_SET.has(routeMainStatus) ? routeMainStatus : '';
  query.pageNum = 1;
}

onMounted(() => {
  applyFiltersFromRouteQuery();
  loadData();
  openDetailByRouteQuery();
});

watch(
  () => [route.query.viewScope, route.query.mainStatus],
  () => {
    applyFiltersFromRouteQuery();
    loadData();
  }
);
</script>

<template>
  <div class="work-order-page min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" size="small" class="work-order-search-card card-wrapper">
      <AForm :model="query" :label-col="{ span: 5, md: 7 }" class="work-order-search-form">
        <div class="work-order-dual-filters">
          <div class="work-order-scope-section work-order-filter-strip">
            <ATabs
              v-model:active-key="query.viewScope"
              size="small"
              class="work-order-l1-tabs"
              @change="handleScopeChange"
            >
              <ATabPane key="CURRENT" tab="当前处理" />
              <ATabPane key="HISTORY" tab="历史转出/只读" />
            </ATabs>
          </div>
          <div
            class="work-order-status-toolbar work-order-filter-strip"
            :style="{ '--work-order-segmented-thumb-bg': workOrderStatusSegmentedThumbBg }"
          >
            <span class="work-order-section-label">工单状态</span>
            <div class="work-order-status-segmented-wrap">
              <ASegmented
                v-model:value="activeMainStatus"
                size="small"
                :options="statusSegmentOptions"
                class="work-order-status-segmented"
                @change="handleMainStatusChange"
              />
            </div>
          </div>
        </div>
        <ADivider class="work-order-search-divider !my-14px" />
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="工单号" class="m-0">
                  <AInput v-model:value="query.orderNo" allow-clear placeholder="请输入工单号" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="客户姓名" class="m-0">
                  <AInput v-model:value="query.customerName" allow-clear placeholder="请输入客户姓名" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="客户手机号" class="m-0">
                  <AInput v-model:value="query.customerMobile" allow-clear placeholder="请输入客户手机号" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="条码" class="m-0">
                  <AInput v-model:value="query.barcode" allow-clear placeholder="请输入条码" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="是否转单" class="m-0">
                  <ASelect
                    v-model:value="query.hasTransfer"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="hasTransferOptions"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleSearch">查询</AButton>
            <AButton :loading="loading" @click="resetQuery">重置</AButton>
          </div>
        </div>
      </AForm>
    </ACard>
    <ACard
      :bordered="false"
      size="small"
      :body-style="{ flex: 1, overflow: 'hidden', paddingTop: '12px' }"
      class="work-order-table-card flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #title>
        <div class="flex items-center gap-8px">
          <span class="work-order-card-title-bar" />
          <span>工单管理</span>
        </div>
      </template>
      <template #extra>
        <AButton v-if="hasAuth('workorder:add')" type="primary" size="small" @click="createModalsRef?.open()">
          建维修订单
        </AButton>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        bordered
        class="work-order-table h-full"
        :scroll="scrollConfig"
        :pagination="{
          current: query.pageNum,
          pageSize: query.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handleTableChange
        }"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openDetail(record)"
              @keydown.enter.prevent="openDetail(record)"
            >
              {{ record.orderNo || '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'mainStatusLabel'">
            <ATag :color="workOrderMainStatusTagColor(String(record.mainStatus || ''))">
              {{ record.mainStatusLabel || record.mainStatus || '-' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'hasTransfer'">
            <ATag :color="tagColorTransferTransferred(Number(record.hasTransfer) === 1)">
              {{ Number(record.hasTransfer) === 1 ? '是' : '否' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="work-order-actions-cell">
              <div class="work-order-actions-cell__row work-order-actions-cell__row--primary">
                <AButton
                  v-for="item in getRowPrimaryActions(record)"
                  :key="`${record.id}-${item.action}`"
                  type="link"
                  size="small"
                  class="work-order-action-link table-action-link"
                  :danger="item.type === 'danger'"
                  :class="{
                    'table-action-link--warning': item.type === 'warning',
                    'table-action-link--primary': item.type === 'primary'
                  }"
                  @click="handleListAction(record, item.action)"
                >
                  {{ item.label }}
                </AButton>
              </div>
            </div>
          </template>
        </template>
      </ATable>
    </ACard>

    <WorkOrderCreateModals ref="createModalsRef" @created="loadData" />
    <WorkOrderDetailDrawer
      ref="detailDrawerRef"
      :open="detailOpen"
      :work-order-id="detailWorkOrderId"
      @update:open="v => (detailOpen = v)"
      @success="loadData"
    />
  </div>
</template>

<style scoped>
.work-order-card-title-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--ant-color-primary);
  flex-shrink: 0;
}

.work-order-filter-strip {
  padding: 6px 0 2px;
  background: transparent;
  border: none;
}

.work-order-dual-filters {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.work-order-scope-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.work-order-section-label {
  font-size: 12px;
  color: var(--ant-color-text-secondary);
  font-weight: 500;
  line-height: 1.25;
  letter-spacing: 0.02em;
}

/** L1：线型 Tabs（主色字 + 下划线） */
.work-order-l1-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.work-order-l1-tabs :deep(.ant-tabs-nav::before) {
  border-bottom-color: transparent;
}

.work-order-l1-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.work-order-l1-tabs :deep(.ant-tabs-tab) {
  padding: 6px 0;
  margin: 0 14px 0 0;
}

.work-order-l1-tabs :deep(.ant-tabs-tab:last-child) {
  margin-right: 0;
}

.work-order-l1-tabs :deep(.ant-tabs-tab-btn) {
  font-size: 14px;
  transition: color 0.2s ease;
}

.work-order-l1-tabs :deep(.ant-tabs-tab.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: rgb(var(--primary-color));
  font-weight: 600;
}

.work-order-l1-tabs :deep(.ant-tabs-tab:hover .ant-tabs-tab-btn) {
  color: rgb(var(--primary-600-color));
}

.work-order-l1-tabs :deep(.ant-tabs-ink-bar) {
  background: rgb(var(--primary-color));
  height: 2px;
  border-radius: 1px;
}

/** L2：工单状态 Segmented（搜索卡片内） */
.work-order-status-toolbar {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  min-width: 0;
  margin-bottom: 0;
  flex-shrink: 0;
}

.work-order-status-toolbar .work-order-section-label {
  flex-shrink: 0;
}

.work-order-status-segmented-wrap {
  flex: 1 1 auto;
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 2px;
  -webkit-overflow-scrolling: touch;
}

.work-order-status-segmented {
  width: max-content;
  max-width: 100%;
}

/** class 与 ant-segmented 在同一 DOM 根节点上 */
:deep(.work-order-status-segmented.ant-segmented) {
  border-radius: 8px;
  padding: 4px;
  background: transparent !important;
  box-shadow: none !important;
}

/**
 * Segmented 静止态用 `.ant-segmented-item-selected` 铺底（thumb 多在切换动画时出现），故选中底必须在 item-selected 上体现。
 * --work-order-segmented-thumb-bg 与 PageTab chrome-tab_active 同色（transformColorWithOpacity）。
 */
:deep(.work-order-status-segmented .ant-segmented-thumb) {
  background: var(--work-order-segmented-thumb-bg, rgb(var(--primary-100-color))) !important;
  border-radius: 6px;
  box-shadow: none !important;
}

:deep(.work-order-status-segmented .ant-segmented-item:hover:not(.ant-segmented-item-selected)) {
  background: transparent !important;
}

:deep(.work-order-status-segmented .ant-segmented-item-selected) {
  border-radius: 6px;
  overflow: hidden;
  background-color: var(--work-order-segmented-thumb-bg, rgb(var(--primary-100-color))) !important;
  box-shadow: none !important;
  color: rgb(var(--primary-color));
}

:deep(.work-order-status-segmented .ant-segmented-item-selected::after) {
  background-color: transparent !important;
}

:deep(.work-order-status-segmented .ant-segmented-item-selected .ant-segmented-item-label) {
  color: rgb(var(--primary-color)) !important;
  font-weight: 500;
}

:deep(.work-order-status-segmented .ant-segmented-item:not(.ant-segmented-item-selected) .ant-segmented-item-label) {
  color: var(--ant-color-text-secondary);
}

:deep(.work-order-status-segmented .ant-segmented-item) {
  margin: 0 2px;
}

:deep(.work-order-status-segmented .ant-segmented-item-label) {
  padding: 0 10px;
  line-height: 28px;
}

@media (max-width: 767px) {
  .work-order-status-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .work-order-status-segmented-wrap {
    width: 100%;
  }
}

.work-order-search-divider {
  border-color: rgba(5, 5, 5, 0.06);
}

.work-order-search-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.work-order-table :deep(.ant-table-cell) {
  font-size: 13px;
}

.work-order-table-card :deep(.ant-card-head) {
  min-height: 48px;
}

.work-order-search-card :deep(.ant-card-head) {
  min-height: 44px;
}

.work-order-actions-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.work-order-actions-cell__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 2px;
  line-height: 1.4;
}

.work-order-actions-cell__row--primary {
  margin-bottom: 0;
}

.work-order-action-link {
  height: auto !important;
  padding: 0 4px !important;
  font-size: 13px;
}

.work-order-actions-cell__reason {
  flex-basis: 100%;
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
  line-height: 1.4;
  max-width: 260px;
}
</style>
