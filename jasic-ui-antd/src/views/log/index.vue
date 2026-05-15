<script setup lang="ts">
/**
 * 操作日志：分页查询、条件筛选与清理/删除；详情以右侧抽屉展示（对接 log 域接口）。
 */
import { onMounted, reactive, ref } from 'vue';
import { tagColorEnabled } from '@/constants/list-status-tag';
import { type OperLogQuery, cleanOperLog, deleteOperLog, listOperLog } from '@/service/api';
import PageSearchExpandButton from '@/components/custom/page-search-expand-button.vue';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { usePageSearchFilterCollapse } from '@/hooks/common/page-search-filter-collapse';
import { useTableScroll } from '@/hooks/common/table';
import {
  createAntTableListLocale,
  useListRequestTableMsgs
} from '@/utils/list-table-empty-state';

type RowData = Record<string, any>;

const { tableWrapperRef, scrollConfig } = useTableScroll(1200);
const pageMenuTitle = useRouteMenuTitle();

const logSearchFilter = usePageSearchFilterCollapse(5);

// 列表请求加载态
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

// 表格多选主键
const selectedRowKeys = ref<(string | number)[]>([]);

const queryParams = reactive<OperLogQuery>({
  pageNum: 1,
  pageSize: 10,
  title: '',
  operUserName: '',
  operType: undefined,
  status: undefined,
  beginTime: '',
  endTime: ''
});

// 操作日期区间（映射到 beginTime/endTime）
const dateRange = ref<[string, string] | undefined>(undefined);

const operTypeMap: Record<number, string> = {
  1: '新增',
  2: '修改',
  3: '删除',
  10: '登录',
  11: '登出',
  12: '强退',
  99: '其他'
};

// 表格列定义
const columns = [
  { title: '日志ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '操作模块', dataIndex: 'title', key: 'title', width: 150 },
  { title: '操作类型', dataIndex: 'operType', key: 'operType', width: 100 },
  { title: '请求方式', dataIndex: 'requestMethod', key: 'requestMethod', width: 90 },
  { title: '操作人', dataIndex: 'operUserName', key: 'operUserName', width: 110 },
  { title: 'IP地址', dataIndex: 'ip', key: 'ip', width: 130 },
  { title: '请求URL', dataIndex: 'requestUrl', key: 'requestUrl', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '耗时(ms)', dataIndex: 'costTime', key: 'costTime', width: 100 },
  { title: '操作时间', dataIndex: 'operTime', key: 'operTime', width: 170 }
];

// 详情抽屉开关与当前行数据
const detailOpen = ref(false);
const detail = ref<RowData>({});

/**
 * 作用：兼容分页或数组响应，取出日志表格行数组。
 * @param data - 日志列表接口返回的数据主体
 * @returns 可渲染表格的行数组
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 作用：组装列表查询参数，并将日期范围写入 begin/end。
 * @param 无
 * @returns 可直接用于日志列表接口的查询对象
 */
function buildListParams(): OperLogQuery {
  const p = { ...queryParams };
  if (dateRange.value?.length === 2) {
    p.beginTime = dateRange.value[0];
    p.endTime = dateRange.value[1];
  } else {
    p.beginTime = undefined;
    p.endTime = undefined;
  }
  return p;
}

/**
 * 作用：请求操作日志分页并刷新表格与总数。
 * @param 无
 * @returns Promise，列表加载完成后结束
 */
async function loadList() {
  clearListMsgs();
  loading.value = true;
  try {
    const flat = await listOperLog(buildListParams());
    if (consumeFlatError(flat)) {
      rows.value = [];
      total.value = 0;
      return;
    }
    const data = (flat as { data?: unknown }).data;
    rows.value = pickRows(data);
    total.value = Number((data as { total?: unknown })?.total) || rows.value.length;
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
 * 作用：查询：重置到第一页并拉取日志。
 * @param 无
 * @returns {void} 无
 */
function handleQuery() {
  queryParams.pageNum = 1;
  loadList();
}

/**
 * 作用：重置筛选条件与分页为第一页默认值并刷新。
 * @param 无
 * @returns {void} 无
 */
function resetQuery() {
  dateRange.value = undefined;
  queryParams.pageNum = 1;
  queryParams.pageSize = 10;
  queryParams.title = '';
  queryParams.operUserName = '';
  queryParams.operType = undefined;
  queryParams.status = undefined;
  queryParams.beginTime = '';
  queryParams.endTime = '';
  loadList();
}

/**
 * 作用：分页或每页条数变化时刷新列表。
 * @param page - 目标页码
 * @param pageSize - 每页条数，可选
 * @returns {void} 无
 */
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

/**
 * 作用：打开右侧详情抽屉并展示当前行数据。
 * @param row - 日志表格行
 * @returns {void} 无
 */
function openDetail(row: RowData) {
  detail.value = row;
  detailOpen.value = true;
}

/**
 * 作用：批量删除勾选的日志并清空选择。
 * @param 无
 * @returns Promise，删除完成后结束
 */
async function batchDelete() {
  if (!selectedRowKeys.value.length) return;
  await deleteOperLog(selectedRowKeys.value);
  selectedRowKeys.value = [];
  loadList();
}

/**
 * 作用：清空全部操作日志后刷新并与勾选清零。
 * @param 无
 * @returns Promise，清空完成后结束
 */
async function cleanAll() {
  await cleanOperLog();
  selectedRowKeys.value = [];
  loadList();
}

/**
 * 作用：挂载后首次加载日志列表。
 * @param 无
 * @returns {void} 无
 */
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
                :class="{ 'page-search-toolbar__filter-col--collapsed': logSearchFilter.isSearchFilterHidden(0) }"
              >
                <AFormItem label="操作模块" class="m-0">
                  <AInput v-model:value="queryParams.title" allow-clear placeholder="请输入操作模块" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{ 'page-search-toolbar__filter-col--collapsed': logSearchFilter.isSearchFilterHidden(1) }"
              >
                <AFormItem label="操作人" class="m-0">
                  <AInput v-model:value="queryParams.operUserName" allow-clear placeholder="请输入操作人" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{ 'page-search-toolbar__filter-col--collapsed': logSearchFilter.isSearchFilterHidden(2) }"
              >
                <AFormItem label="操作类型" class="m-0">
                  <ASelect
                    v-model:value="queryParams.operType"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="[
                      { label: '新增', value: 1 },
                      { label: '修改', value: 2 },
                      { label: '删除', value: 3 },
                      { label: '登录', value: 10 },
                      { label: '登出', value: 11 }
                    ]"
                  />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{ 'page-search-toolbar__filter-col--collapsed': logSearchFilter.isSearchFilterHidden(3) }"
              >
                <AFormItem label="操作状态" class="m-0">
                  <ASelect
                    v-model:value="queryParams.status"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="[
                      { label: '成功', value: 1 },
                      { label: '失败', value: 0 }
                    ]"
                  />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="12"
                :class="{ 'page-search-toolbar__filter-col--collapsed': logSearchFilter.isSearchFilterHidden(4) }"
              >
                <AFormItem label="操作日期" class="m-0">
                  <ARangePicker
                    v-model:value="dateRange"
                    value-format="YYYY-MM-DD"
                    class="w-full"
                    :placeholder="['开始日期', '结束日期']"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleQuery">搜索</AButton>
            <AButton @click="resetQuery">重置</AButton>
            <PageSearchExpandButton
              v-if="logSearchFilter.showSearchFilterExpandToggle"
              :expanded="logSearchFilter.searchFilterExpanded"
              @click="logSearchFilter.toggleSearchFilterExpand"
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
        <ASpace>
          <APopconfirm title="确认删除所选日志？" :disabled="!selectedRowKeys.length" @confirm="batchDelete">
            <AButton danger size="small" :disabled="!selectedRowKeys.length">批量删除</AButton>
          </APopconfirm>
          <APopconfirm title="确认清空全部操作日志？此操作不可恢复。" @confirm="cleanAll">
            <AButton danger size="small" ghost>清空日志</AButton>
          </APopconfirm>
        </ASpace>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        row-key="id"
        size="small"
        class="h-full"
        :row-selection="{
          selectedRowKeys,
          onChange: (keys: (string | number)[]) => {
            selectedRowKeys = keys;
          }
        }"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: onPageChange
        }"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openDetail(record)"
              @keydown.enter.prevent="openDetail(record)"
            >
              {{ record.id || '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'title'">
            {{ record.title || '-' }}
          </template>
          <template v-else-if="column.key === 'operType'">
            {{ operTypeMap[Number(record.operType)] ?? record.operType }}
          </template>
          <template v-else-if="column.key === 'status'">
            <ATag :color="tagColorEnabled(record.status === 1)">
              {{ record.status === 1 ? '成功' : '失败' }}
            </ATag>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer
      v-model:open="detailOpen"
      title="操作日志详情"
      placement="right"
      :width="800"
      destroy-on-close
    >
      <ADescriptions bordered size="small" :column="2" :label-style="{ whiteSpace: 'nowrap' }">
        <ADescriptionsItem label="日志ID">{{ detail.id }}</ADescriptionsItem>
        <ADescriptionsItem label="操作模块">{{ detail.title }}</ADescriptionsItem>
        <ADescriptionsItem label="操作类型">
          {{ operTypeMap[Number(detail.operType)] ?? detail.operType }}
        </ADescriptionsItem>
        <ADescriptionsItem label="请求方式">{{ detail.requestMethod }}</ADescriptionsItem>
        <ADescriptionsItem label="操作人">{{ detail.operUserName }}</ADescriptionsItem>
        <ADescriptionsItem label="IP地址">{{ detail.ip }}</ADescriptionsItem>
        <ADescriptionsItem label="请求URL">{{ detail.requestUrl }}</ADescriptionsItem>
        <ADescriptionsItem label="调用方法">{{ detail.method }}</ADescriptionsItem>
        <ADescriptionsItem label="请求参数">
          <div class="max-h-120px overflow-y-auto break-all">{{ detail.requestParam }}</div>
        </ADescriptionsItem>
        <ADescriptionsItem label="返回结果">
          <div class="max-h-120px overflow-y-auto break-all">{{ detail.responseResult }}</div>
        </ADescriptionsItem>
        <ADescriptionsItem label="状态">{{ detail.status === 1 ? '成功' : '失败' }}</ADescriptionsItem>
        <ADescriptionsItem label="耗时">{{ detail.costTime }} ms</ADescriptionsItem>
        <ADescriptionsItem label="操作时间">{{ detail.operTime }}</ADescriptionsItem>
        <ADescriptionsItem v-if="detail.errorMsg" label="错误信息">
          <span class="text-red-500">{{ detail.errorMsg }}</span>
        </ADescriptionsItem>
      </ADescriptions>
      <template #footer>
        <ASpace>
          <AButton type="primary" @click="detailOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>
