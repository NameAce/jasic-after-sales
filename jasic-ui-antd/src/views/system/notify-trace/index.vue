<script setup lang="ts">
/**
 * 通知记录排障：按事件维度分页查询、查看事件/分发详情，支持人工重试与死信标记。
 */
import { onMounted, reactive, ref } from 'vue';
import {
  type NotifyTraceQuery,
  deadNotifyTraceDispatch,
  deadNotifyTraceEvent,
  getNotifyTraceDispatch,
  getNotifyTraceEvent,
  getNotifyTracePage,
  retryNotifyTraceDispatch,
  retryNotifyTraceEvent
} from '@/service/api';
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useAuth } from '@/hooks/business/auth';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { usePageSearchFilterCollapse } from '@/hooks/common/page-search-filter-collapse';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableActionColumn } from '@/utils/table-action-width';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { applyDateTimeColumnRender, formatDateTime } from '@/utils/datetime';
import PageSearchExpandButton from '@/components/custom/page-search-expand-button.vue';

type RowData = Record<string, any>;
type TargetSummary = {
  targetType?: string;
  targetTypeDesc?: string;
  highlightStatus?: string;
  highlightStatusDesc?: string;
  summaryText?: string;
};

/** 允许人工重试的状态 */
const RETRY_STATUS = ['FAILED', 'DEAD'];
/** 允许人工标记死信的事件状态 */
const EVENT_DEAD_STATUS = ['NEW', 'PROCESSING', 'FAILED'];
/** 允许人工标记死信的分发状态 */
const DISPATCH_DEAD_STATUS = ['PENDING', 'PROCESSING', 'FAILED'];

const pageMenuTitle = useRouteMenuTitle();
const { hasAuth } = useAuth();
const traceSearchFilter = usePageSearchFilterCollapse(5);

const loading = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);
const dateRange = ref<[string, string] | undefined>(undefined);

const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch
} = useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

const queryParams = reactive<NotifyTraceQuery>({
  pageNum: 1,
  pageSize: 10,
  bizNo: '',
  sceneCode: '',
  targetType: undefined,
  eventStatus: undefined,
  dispatchStatus: undefined
});

const targetTypeOptions = [
  { label: '站内消息', value: 'IN_APP_MESSAGE' },
  { label: '站内待办', value: 'IN_APP_TODO' },
  { label: '小程序订阅消息(B端)', value: 'MP_SUBSCRIBE_B' },
  { label: '小程序订阅消息(C端)', value: 'MP_SUBSCRIBE_C' }
];

const eventStatusOptions = [
  { label: '新建 / NEW', value: 'NEW' },
  { label: '处理中 / PROCESSING', value: 'PROCESSING' },
  { label: '成功 / SUCCESS', value: 'SUCCESS' },
  { label: '失败 / FAILED', value: 'FAILED' },
  { label: '死信 / DEAD', value: 'DEAD' }
];

const dispatchStatusOptions = [
  { label: '待发送 / PENDING', value: 'PENDING' },
  { label: '处理中 / PROCESSING', value: 'PROCESSING' },
  { label: '成功 / SUCCESS', value: 'SUCCESS' },
  { label: '失败 / FAILED', value: 'FAILED' },
  { label: '跳过 / SKIPPED', value: 'SKIPPED' },
  { label: '死信 / DEAD', value: 'DEAD' }
];

/** 操作列仅保留「重试事件」「标记死信」，不再单独放「事件详情」入口 */
const NOTIFY_TRACE_ACTION_WIDTH = 160;
/** 列表列宽合计，尽量在常见屏宽下一屏横向看全 */
const NOTIFY_TRACE_TABLE_SCROLL_X = 1358 + NOTIFY_TRACE_ACTION_WIDTH;
const { tableWrapperRef, scrollConfig } = useTableScroll(NOTIFY_TRACE_TABLE_SCROLL_X);

const columns = applyDateTimeColumnRender([
  {
    title: '业务编号',
    dataIndex: 'bizNo',
    key: 'bizNo',
    width: 140,
    ellipsis: true
  },
  {
    title: '通知场景',
    dataIndex: 'sceneCode',
    key: 'scene',
    width: 190,
    ellipsis: true
  },
  {
    title: '事件状态',
    dataIndex: 'eventStatus',
    key: 'eventStatus',
    width: 90
  },
  {
    title: '站内产物',
    dataIndex: 'messageCount',
    key: 'messageSummary',
    width: 240,
    ellipsis: true
  },
  {
    title: '外部分发',
    dataIndex: 'dispatchCount',
    key: 'dispatchSummary',
    width: 240,
    ellipsis: true
  },
  {
    title: '最近错误',
    dataIndex: 'eventErrorMessage',
    key: 'eventErrorMessage',
    width: 160,
    ellipsis: true
  },
  {
    title: '重试次数',
    dataIndex: 'eventRetryCount',
    key: 'eventRetryCount',
    width: 80
  },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 154 },
  createAntTableActionColumn({
    width: NOTIFY_TRACE_ACTION_WIDTH,
    fixed: 'right'
  })
]);

const eventDrawerOpen = ref(false);
const eventDrawerLoading = ref(false);
const eventDetail = ref<RowData | null>(null);

const dispatchDrawerOpen = ref(false);
const dispatchDrawerLoading = ref(false);
const dispatchDetail = ref<RowData | null>(null);

/** 死信确认抽屉 */
const deadDrawerOpen = ref(false);
const deadDrawerSubmitting = ref(false);
const deadDrawerReason = ref('');
const deadDrawerTarget = ref<{ type: 'event' | 'dispatch'; id: number } | null>(null);

/**
 * 作用：从分页响应中解析表格行数组。
 */
function pickRows(data: unknown) {
  if (Array.isArray(data)) return data;
  if (Array.isArray((data as { records?: unknown })?.records)) return (data as { records: RowData[] }).records;
  return [];
}

/**
 * 作用：组装列表查询参数，并将时间范围映射为 beginTime/endTime。
 */
function buildQueryParams(): NotifyTraceQuery {
  const params: NotifyTraceQuery = { ...queryParams };
  if (dateRange.value?.length === 2) {
    params.beginTime = dateRange.value[0];
    params.endTime = dateRange.value[1];
  } else {
    params.beginTime = undefined;
    params.endTime = undefined;
  }
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
  ) as NotifyTraceQuery;
}

/**
 * 作用：拉取通知记录分页列表。
 */
async function loadList() {
  clearListMsgs();
  loading.value = true;
  try {
    const flat = await getNotifyTracePage(buildQueryParams());
    if (consumeFlatError(flat)) {
      rows.value = [];
      total.value = 0;
      return;
    }
    const data = (flat as { data?: unknown }).data;
    rows.value = pickRows(data);
    total.value = Number((data as { total?: unknown })?.total) || 0;
    refreshEmptySuccessMsg(flat, rows.value.length);
  } catch (e: unknown) {
    rows.value = [];
    total.value = 0;
    setMsgFromCatch(e);
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  loadList();
}

function resetQuery() {
  dateRange.value = undefined;
  queryParams.pageNum = 1;
  queryParams.pageSize = 10;
  queryParams.bizNo = '';
  queryParams.sceneCode = '';
  queryParams.targetType = undefined;
  queryParams.eventStatus = undefined;
  queryParams.dispatchStatus = undefined;
  loadList();
}

function onPageChange(page: number, pageSize?: number) {
  if (pageSize !== undefined && pageSize !== queryParams.pageSize) {
    queryParams.pageSize = pageSize;
    queryParams.pageNum = 1;
  } else {
    queryParams.pageNum = page;
    if (pageSize !== undefined) queryParams.pageSize = pageSize;
  }
  loadList();
}

async function openEventDetail(eventId?: number) {
  if (!eventId) {
    window.$message?.warning('当前记录没有事件ID');
    return;
  }
  eventDrawerOpen.value = true;
  await loadEventDetail(eventId);
}

/**
 * 作用：列表行点击业务编号时打开事件详情抽屉（与故障配置页「归属总部」入口交互一致）。
 */
function openEventDetailFromRow(row: RowData) {
  if (!hasAuth('system:notifyTrace:view')) return;
  openEventDetail(row.eventId);
}

/** 业务编号是否展示为可点击的详情入口 */
function canOpenEventDetailFromBizNo(row: RowData) {
  return hasAuth('system:notifyTrace:view') && Boolean(row.eventId);
}

async function loadEventDetail(eventId: number) {
  eventDrawerLoading.value = true;
  try {
    const flat = await getNotifyTraceEvent(eventId);
    if (consumeFlatError(flat)) {
      eventDetail.value = null;
      return;
    }
    eventDetail.value = ((flat as { data?: RowData }).data || null) as RowData | null;
  } catch {
    eventDetail.value = null;
  } finally {
    eventDrawerLoading.value = false;
  }
}

async function openDispatchDetail(dispatchId?: number) {
  if (!dispatchId) {
    window.$message?.warning('当前记录没有分发ID');
    return;
  }
  dispatchDrawerOpen.value = true;
  await loadDispatchDetail(dispatchId);
}

async function loadDispatchDetail(dispatchId: number) {
  dispatchDrawerLoading.value = true;
  try {
    const flat = await getNotifyTraceDispatch(dispatchId);
    if (consumeFlatError(flat)) {
      dispatchDetail.value = null;
      return;
    }
    dispatchDetail.value = ((flat as { data?: RowData }).data || null) as RowData | null;
  } catch {
    dispatchDetail.value = null;
  } finally {
    dispatchDrawerLoading.value = false;
  }
}

function openDeadDrawer(type: 'event' | 'dispatch', id?: number) {
  if (!id) {
    window.$message?.warning('未获取到可操作记录ID');
    return;
  }
  deadDrawerTarget.value = { type, id };
  deadDrawerReason.value = '';
  deadDrawerOpen.value = true;
}

async function submitDeadDrawer() {
  const reason = deadDrawerReason.value.trim();
  if (!reason) {
    window.$message?.warning('处理原因不能为空');
    return;
  }
  const target = deadDrawerTarget.value;
  if (!target) return;

  deadDrawerSubmitting.value = true;
  try {
    const flat =
      target.type === 'dispatch'
        ? await deadNotifyTraceDispatch(target.id, { reason })
        : await deadNotifyTraceEvent(target.id, { reason });
    if (!notifyOnceSuccessFromFlatResult(flat, '已标记死信')) return;
    deadDrawerOpen.value = false;
    await refreshAfterAction(target.type, target.id);
  } finally {
    deadDrawerSubmitting.value = false;
  }
}

async function retryTarget(type: 'event' | 'dispatch', id?: number) {
  if (!id) {
    window.$message?.warning('未获取到可操作记录ID');
    return;
  }
  const flat = type === 'dispatch' ? await retryNotifyTraceDispatch(id) : await retryNotifyTraceEvent(id);
  if (!notifyOnceSuccessFromFlatResult(flat, '重试已提交')) return;
  await refreshAfterAction(type, id);
}

async function refreshAfterAction(type: 'event' | 'dispatch', id: number) {
  await loadList();
  if (type === 'event' && eventDrawerOpen.value && eventDetail.value?.id === id) {
    await loadEventDetail(id);
  }
  if (type === 'dispatch' && dispatchDrawerOpen.value && dispatchDetail.value?.id === id) {
    await loadDispatchDetail(id);
  }
  if (type === 'dispatch' && eventDrawerOpen.value && eventDetail.value?.id) {
    await loadEventDetail(Number(eventDetail.value.id));
  }
}

function canRetryEventStatus(status?: string) {
  return Boolean(status) && RETRY_STATUS.includes(status);
}

function canDeadEventStatus(status?: string) {
  return Boolean(status) && EVENT_DEAD_STATUS.includes(status);
}

function canRetryDispatchStatus(status?: string) {
  return Boolean(status) && RETRY_STATUS.includes(status);
}

function canDeadDispatchStatus(status?: string) {
  return Boolean(status) && DISPATCH_DEAD_STATUS.includes(status);
}

function sceneLabel(sceneCode?: string, sceneName?: string) {
  return sceneName || sceneCode || '-';
}

function targetTypeLabel(code?: string, desc?: string) {
  if (desc) return desc;
  const map: Record<string, string> = {
    IN_APP_MESSAGE: '站内消息',
    IN_APP_TODO: '站内待办',
    MP_SUBSCRIBE_B: '小程序订阅消息(B端)',
    MP_SUBSCRIBE_C: '小程序订阅消息(C端)',
    SMS: '短信',
    EMAIL: '邮件'
  };
  return (code && map[code]) || code || '-';
}

function eventStatusLabel(status?: string) {
  const map: Record<string, string> = {
    NEW: '新建',
    PROCESSING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
    DEAD: '死信'
  };
  return (status && map[status]) || status || '-';
}

function dispatchStatusLabel(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待发送',
    PROCESSING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
    SKIPPED: '已跳过',
    DEAD: '死信'
  };
  return (status && map[status]) || status || '-';
}

function inAppStatusLabel(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    READ: '已读',
    DONE: '已处理',
    INVALID: '已失效'
  };
  return (status && map[status]) || status || '-';
}

function statusTagColor(status?: string) {
  const map: Record<string, string> = {
    SUCCESS: 'success',
    DONE: 'success',
    READ: 'processing',
    FAILED: 'error',
    DEAD: 'error',
    INVALID: 'default',
    SKIPPED: 'warning',
    PROCESSING: 'warning',
    NEW: 'default',
    PENDING: 'default'
  };
  return (status && map[status]) || 'default';
}

function prettyJson(value: unknown) {
  if (!value) return '-';
  if (typeof value === 'object') return JSON.stringify(value, null, 2);
  try {
    return JSON.stringify(JSON.parse(String(value)), null, 2);
  } catch {
    return String(value);
  }
}

function messageSummaries(row: RowData): TargetSummary[] {
  return Array.isArray(row.messageTargetSummaries) ? row.messageTargetSummaries : [];
}

function dispatchSummaries(row: RowData): TargetSummary[] {
  return Array.isArray(row.dispatchTargetSummaries) ? row.dispatchTargetSummaries : [];
}

/** 事件详情 / 分发详情抽屉宽度 */
const EVENT_DRAWER_WIDTH = 1280;
const DISPATCH_DRAWER_WIDTH = 1100;
/** 事件详情抽屉内表格横向滚动宽度（略小于抽屉内容区，避免多余滚动条） */
const EVENT_DRAWER_TABLE_SCROLL_X = 1180;

const eventMessageColumns = applyDateTimeColumnRender([
  { title: '消息ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '目标', key: 'target', width: 120 },
  { title: '接收人', key: 'receiver', width: 160, ellipsis: true },
  {
    title: '标题',
    dataIndex: 'title',
    key: 'title',
    width: 200,
    ellipsis: true
  },
  { title: '状态', dataIndex: 'todoStatus', key: 'todoStatus', width: 96 },
  {
    title: '失效原因',
    dataIndex: 'invalidReason',
    key: 'invalidReason',
    width: 160,
    ellipsis: true
  },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
]);

const eventDispatchColumns = [
  { title: '分发ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '目标', key: 'target', width: 120 },
  { title: '渠道', key: 'channel', width: 110 },
  {
    title: '接收地址',
    dataIndex: 'receiverAddress',
    key: 'receiverAddress',
    width: 180,
    ellipsis: true
  },
  {
    title: '状态',
    dataIndex: 'dispatchStatus',
    key: 'dispatchStatus',
    width: 96
  },
  {
    title: '结果编码',
    dataIndex: 'resultCode',
    key: 'resultCode',
    width: 150,
    ellipsis: true
  },
  {
    title: '结果说明',
    dataIndex: 'resultMessage',
    key: 'resultMessage',
    width: 200,
    ellipsis: true
  },
  createAntTableActionColumn({ width: 180 })
];

onMounted(loadList);
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <AForm :model="queryParams" :label-col="{ span: 5, md: 7 }">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': traceSearchFilter.isSearchFilterHidden(0)
                }"
              >
                <AFormItem label="工单号" class="m-0">
                  <AInput v-model:value="queryParams.bizNo" allow-clear placeholder="请输入工单号" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': traceSearchFilter.isSearchFilterHidden(1)
                }"
              >
                <AFormItem label="通知场景" class="m-0">
                  <AInput v-model:value="queryParams.sceneCode" allow-clear placeholder="请输入场景编码" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': traceSearchFilter.isSearchFilterHidden(2)
                }"
              >
                <AFormItem label="通知目标" class="m-0">
                  <ASelect
                    v-model:value="queryParams.targetType"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="targetTypeOptions"
                  />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': traceSearchFilter.isSearchFilterHidden(3)
                }"
              >
                <AFormItem label="事件状态" class="m-0">
                  <ASelect
                    v-model:value="queryParams.eventStatus"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="eventStatusOptions"
                  />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': traceSearchFilter.isSearchFilterHidden(4)
                }"
              >
                <AFormItem label="分发状态" class="m-0">
                  <ASelect
                    v-model:value="queryParams.dispatchStatus"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="dispatchStatusOptions"
                  />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': traceSearchFilter.isSearchFilterHidden(5)
                }"
              >
                <AFormItem label="时间范围" class="m-0">
                  <ARangePicker
                    v-model:value="dateRange"
                    show-time
                    value-format="YYYY-MM-DD HH:mm:ss"
                    class="w-full"
                    :placeholder="['开始时间', '结束时间']"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleQuery">搜索</AButton>
            <AButton @click="resetQuery">重置</AButton>
            <PageSearchExpandButton
              v-if="traceSearchFilter.showSearchFilterExpandToggle"
              :expanded="traceSearchFilter.searchFilterExpanded"
              @click="traceSearchFilter.toggleSearchFilterExpand"
            />
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
      <template #extra>
        <AButton size="small" @click="loadList">刷新</AButton>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        :scroll="scrollConfig"
        row-key="eventId"
        size="small"
        class="h-full"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: onPageChange
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'bizNo'">
            <span
              v-if="canOpenEventDetailFromBizNo(record)"
              class="table-list-entry-link notify-trace-biz-no"
              role="button"
              tabindex="0"
              :title="record.bizNo || ''"
              @click="openEventDetailFromRow(record)"
              @keydown.enter.prevent="openEventDetailFromRow(record)"
            >
              {{ record.bizNo || '-' }}
            </span>
            <span v-else class="notify-trace-biz-no" :title="record.bizNo || ''">{{ record.bizNo || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'scene'">
            <div>{{ sceneLabel(record.sceneCode, record.sceneName) }}</div>
            <div class="muted-code">{{ record.sceneCode || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'eventStatus'">
            <ATag :color="statusTagColor(record.eventStatus)">{{ eventStatusLabel(record.eventStatus) }}</ATag>
          </template>
          <template v-else-if="column.key === 'messageSummary'">
            <div v-if="record.messageCount" class="summary-block">
              <div
                v-for="summary in messageSummaries(record)"
                :key="`message-${summary.targetType}`"
                class="summary-item"
              >
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <ATag :color="statusTagColor(summary.highlightStatus)" size="small">
                    {{ summary.highlightStatusDesc || '-' }}
                  </ATag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
              </div>
            </div>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'dispatchSummary'">
            <div v-if="record.dispatchCount" class="summary-block">
              <div
                v-for="summary in dispatchSummaries(record)"
                :key="`dispatch-${summary.targetType}`"
                class="summary-item"
              >
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <ATag :color="statusTagColor(summary.highlightStatus)" size="small">
                    {{ summary.highlightStatusDesc || '-' }}
                  </ATag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
              </div>
            </div>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <APopconfirm
              v-if="hasAuth('system:notifyTrace:retry') && canRetryEventStatus(record.eventStatus)"
              title="确认将该通知事件重新放回待处理队列吗？"
              @confirm="retryTarget('event', record.eventId)"
            >
              <AButton type="link" size="small">重试事件</AButton>
            </APopconfirm>
            <AButton
              v-if="hasAuth('system:notifyTrace:dead') && canDeadEventStatus(record.eventStatus)"
              type="link"
              size="small"
              danger
              @click="openDeadDrawer('event', record.eventId)"
            >
              标记死信
            </AButton>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer
      v-model:open="eventDrawerOpen"
      title="事件详情"
      placement="right"
      :width="EVENT_DRAWER_WIDTH"
      destroy-on-close
    >
      <ASpin :spinning="eventDrawerLoading">
        <div v-if="eventDetail?.id" class="drawer-toolbar">
          <ASpace>
            <APopconfirm
              v-if="hasAuth('system:notifyTrace:retry') && canRetryEventStatus(eventDetail.status)"
              title="确认重试该通知事件？"
              @confirm="retryTarget('event', eventDetail.id)"
            >
              <AButton type="primary" ghost size="small">重试事件</AButton>
            </APopconfirm>
            <AButton
              v-if="hasAuth('system:notifyTrace:dead') && canDeadEventStatus(eventDetail.status)"
              danger
              ghost
              size="small"
              @click="openDeadDrawer('event', eventDetail.id)"
            >
              标记死信
            </AButton>
          </ASpace>
        </div>

        <ADescriptions
          v-if="eventDetail"
          bordered
          size="small"
          :column="3"
          :label-style="{ whiteSpace: 'nowrap' }"
          class="event-detail-descriptions mb-16px"
        >
          <ADescriptionsItem label="事件ID">{{ eventDetail.id }}</ADescriptionsItem>
          <ADescriptionsItem label="事件状态">
            <ATag :color="statusTagColor(eventDetail.status)">{{ eventStatusLabel(eventDetail.status) }}</ATag>
          </ADescriptionsItem>
          <ADescriptionsItem label="通知场景">
            {{ sceneLabel(eventDetail.sceneCode, eventDetail.sceneName) }}
          </ADescriptionsItem>
          <ADescriptionsItem label="场景编码">{{ eventDetail.sceneCode || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="事件类型">{{ eventDetail.eventType || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="幂等键">{{ eventDetail.eventKey || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="业务类型">{{ eventDetail.bizType || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="业务编号">{{ eventDetail.bizNo || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="业务ID">{{ eventDetail.bizId || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="接收对象ID">{{ eventDetail.receiverId || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="操作人ID">{{ eventDetail.operatorId || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="重试次数">{{ eventDetail.retryCount || 0 }}</ADescriptionsItem>
          <ADescriptionsItem label="处理时间">{{ formatDateTime(eventDetail.processingTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="下次重试">{{ formatDateTime(eventDetail.nextRetryTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="创建时间">{{ formatDateTime(eventDetail.createTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="更新时间">{{ formatDateTime(eventDetail.updateTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="最近错误" :span="3">
            <span class="error-text">{{ eventDetail.errorMessage || '-' }}</span>
          </ADescriptionsItem>
        </ADescriptions>

        <div class="section-title">目标产物概览</div>
        <div class="overview-grid mb-16px">
          <div class="overview-card">
            <div class="overview-card__title">站内消息 / 站内待办</div>
            <template v-if="eventDetail?.messageTargetSummaries?.length">
              <div
                v-for="summary in eventDetail.messageTargetSummaries"
                :key="`event-message-${summary.targetType}`"
                class="summary-item mb-8px"
              >
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <ATag :color="statusTagColor(summary.highlightStatus)" size="small">
                    {{ summary.highlightStatusDesc || '-' }}
                  </ATag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
              </div>
            </template>
            <div v-else class="empty-text">当前事件未生成站内产物</div>
          </div>
          <div class="overview-card">
            <div class="overview-card__title">外部分发任务</div>
            <template v-if="eventDetail?.dispatchTargetSummaries?.length">
              <div
                v-for="summary in eventDetail.dispatchTargetSummaries"
                :key="`event-dispatch-${summary.targetType}`"
                class="summary-item mb-8px"
              >
                <div class="summary-item__header">
                  <span>{{ targetTypeLabel(summary.targetType, summary.targetTypeDesc) }}</span>
                  <ATag :color="statusTagColor(summary.highlightStatus)" size="small">
                    {{ summary.highlightStatusDesc || '-' }}
                  </ATag>
                </div>
                <div class="summary-item__text">{{ summary.summaryText }}</div>
              </div>
            </template>
            <div v-else class="empty-text">当前事件未生成外部分发任务</div>
          </div>
        </div>

        <div class="section-title">事件载荷 payload_json</div>
        <pre class="json-view mb-16px">{{ prettyJson(eventDetail?.payloadJson) }}</pre>

        <div class="section-title">关联站内产物</div>
        <ATable
          :columns="eventMessageColumns"
          :data-source="eventDetail?.messages || []"
          :pagination="false"
          :scroll="{ x: EVENT_DRAWER_TABLE_SCROLL_X }"
          size="small"
          bordered
          class="event-detail-table mb-16px"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'target'">
              <div>
                {{ targetTypeLabel(record.targetType, record.targetTypeDesc) }}
              </div>
              <div class="muted-code">{{ record.targetType || '-' }}</div>
            </template>
            <template v-else-if="column.key === 'receiver'">
              {{ record.receiverName || '-' }} / {{ record.receiverId || '-' }}
            </template>
            <template v-else-if="column.key === 'todoStatus'">
              <ATag :color="statusTagColor(record.todoStatus)">{{ inAppStatusLabel(record.todoStatus) }}</ATag>
            </template>
          </template>
        </ATable>

        <div class="section-title">关联外部分发任务</div>
        <ATable
          :columns="eventDispatchColumns"
          :data-source="eventDetail?.dispatches || []"
          :pagination="false"
          :scroll="{ x: EVENT_DRAWER_TABLE_SCROLL_X }"
          size="small"
          bordered
          class="event-detail-table"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'target'">
              <div>
                {{ targetTypeLabel(record.targetType, record.targetTypeDesc) }}
              </div>
              <div class="muted-code">{{ record.targetType || '-' }}</div>
            </template>
            <template v-else-if="column.key === 'channel'">
              {{ targetTypeLabel(record.channelType) }}
            </template>
            <template v-else-if="column.key === 'dispatchStatus'">
              <ATag :color="statusTagColor(record.dispatchStatus)">
                {{ dispatchStatusLabel(record.dispatchStatus) }}
              </ATag>
            </template>
            <template v-else-if="column.key === 'actions'">
              <AButton type="link" size="small" @click="openDispatchDetail(record.id)">查看</AButton>
              <APopconfirm
                v-if="hasAuth('system:notifyTrace:retry') && canRetryDispatchStatus(record.dispatchStatus)"
                title="确认重试该分发任务？"
                @confirm="retryTarget('dispatch', record.id)"
              >
                <AButton type="link" size="small">重试</AButton>
              </APopconfirm>
              <AButton
                v-if="hasAuth('system:notifyTrace:dead') && canDeadDispatchStatus(record.dispatchStatus)"
                type="link"
                size="small"
                danger
                @click="openDeadDrawer('dispatch', record.id)"
              >
                死信
              </AButton>
            </template>
          </template>
        </ATable>
      </ASpin>
    </ADrawer>

    <ADrawer
      v-model:open="dispatchDrawerOpen"
      title="分发详情"
      placement="right"
      :width="DISPATCH_DRAWER_WIDTH"
      destroy-on-close
    >
      <ASpin :spinning="dispatchDrawerLoading">
        <div v-if="dispatchDetail?.id" class="drawer-toolbar">
          <ASpace>
            <APopconfirm
              v-if="hasAuth('system:notifyTrace:retry') && canRetryDispatchStatus(dispatchDetail.dispatchStatus)"
              title="确认重试该分发任务？"
              @confirm="retryTarget('dispatch', dispatchDetail.id)"
            >
              <AButton type="primary" ghost size="small">重试分发</AButton>
            </APopconfirm>
            <AButton
              v-if="hasAuth('system:notifyTrace:dead') && canDeadDispatchStatus(dispatchDetail.dispatchStatus)"
              danger
              ghost
              size="small"
              @click="openDeadDrawer('dispatch', dispatchDetail.id)"
            >
              标记死信
            </AButton>
          </ASpace>
        </div>

        <ADescriptions v-if="dispatchDetail" bordered size="small" :column="2">
          <ADescriptionsItem label="分发ID">{{ dispatchDetail.id }}</ADescriptionsItem>
          <ADescriptionsItem label="来源事件ID">{{ dispatchDetail.eventId || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="通知场景">
            {{ sceneLabel(dispatchDetail.sceneCode, dispatchDetail.sceneName) }}
          </ADescriptionsItem>
          <ADescriptionsItem label="场景编码">{{ dispatchDetail.sceneCode || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="通知目标">
            {{ targetTypeLabel(dispatchDetail.targetType, dispatchDetail.targetTypeDesc) }}
          </ADescriptionsItem>
          <ADescriptionsItem label="目标编码">{{ dispatchDetail.targetType || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="渠道">{{ targetTypeLabel(dispatchDetail.channelType) }}</ADescriptionsItem>
          <ADescriptionsItem label="接收对象">{{ dispatchDetail.receiverId || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="接收地址">{{ dispatchDetail.receiverAddress || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="业务编号">{{ dispatchDetail.bizNo || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="业务ID">{{ dispatchDetail.bizId || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="分发状态">
            <ATag :color="statusTagColor(dispatchDetail.dispatchStatus)">
              {{ dispatchStatusLabel(dispatchDetail.dispatchStatus) }}
            </ATag>
          </ADescriptionsItem>
          <ADescriptionsItem label="重试次数">{{ dispatchDetail.retryCount || 0 }}</ADescriptionsItem>
          <ADescriptionsItem label="结果编码">{{ dispatchDetail.resultCode || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="发送成功时间">{{ formatDateTime(dispatchDetail.sentTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="处理时间">{{ formatDateTime(dispatchDetail.processingTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="下次重试">{{ formatDateTime(dispatchDetail.nextRetryTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="创建时间">{{ formatDateTime(dispatchDetail.createTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="更新时间">{{ formatDateTime(dispatchDetail.updateTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="最近错误 / 结果说明" :span="2">
            <span class="error-text">{{ dispatchDetail.resultMessage || '-' }}</span>
          </ADescriptionsItem>
        </ADescriptions>

        <div class="section-title">分发载荷 payload_json</div>
        <pre class="json-view mb-16px">{{ prettyJson(dispatchDetail?.payloadJson) }}</pre>

        <div class="section-title">渠道响应 channel_response_json</div>
        <pre class="json-view">{{ prettyJson(dispatchDetail?.channelResponseJson) }}</pre>
      </ASpin>
    </ADrawer>

    <ADrawer v-model:open="deadDrawerOpen" title="死信确认" placement="right" :width="480" destroy-on-close>
      <AFormItem label="处理原因" required>
        <ATextarea v-model:value="deadDrawerReason" :rows="4" placeholder="请填写标记死信的原因" />
      </AFormItem>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="deadDrawerOpen = false">取消</AButton>
          <AButton type="primary" danger :loading="deadDrawerSubmitting" @click="submitDeadDrawer">确认标记</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>

<style scoped>
.muted-code {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

/* 业务编号单行展示，过长时省略号 + title 悬停看全文 */
.notify-trace-biz-no {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.drawer-toolbar {
  margin-bottom: 12px;
  text-align: right;
}
.section-title {
  margin: 18px 0 8px;
  font-weight: 600;
  color: #303133;
}
.summary-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-item {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafbfc;
}
.summary-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  color: #303133;
  font-weight: 600;
}
.summary-item__text {
  color: #606266;
  font-size: 12px;
  line-height: 18px;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.overview-card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
}
.overview-card__title {
  margin-bottom: 10px;
  color: #303133;
  font-weight: 600;
}
.empty-text {
  color: #909399;
  font-size: 12px;
}
.json-view {
  min-height: 90px;
  max-height: 320px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #f7f8fa;
  color: #303133;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-word;
}
.error-text {
  color: #ff4d4f;
}
.event-detail-descriptions :deep(.ant-descriptions-item-label) {
  width: 96px;
}
.event-detail-table :deep(.ant-table) {
  font-size: 12px;
}
.event-detail-table :deep(.ant-table-thead > tr > th),
.event-detail-table :deep(.ant-table-tbody > tr > td) {
  padding: 6px 8px;
}
</style>
