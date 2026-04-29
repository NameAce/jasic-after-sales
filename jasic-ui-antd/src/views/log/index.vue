<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { tagColorEnabled } from '@/constants/list-status-tag';
import { type OperLogQuery, cleanOperLog, deleteOperLog, listOperLog } from '@/service/api';
import { useTableScroll } from '@/hooks/common/table';

type RowData = Record<string, any>;

const { tableWrapperRef, scrollConfig } = useTableScroll(1200);

const loading = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);
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

const detailOpen = ref(false);
const detail = ref<RowData>({});

/**
 * 兼容不同返回结构，提取日志列表数组数据。
 * @param data 日志列表接口返回的数据主体。
 * @returns 返回可渲染表格的日志数组。
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 组装日志列表查询参数，按日期控件同步开始和结束时间。
 * @param 无
 * @returns 返回可直接用于日志列表接口的查询参数对象。
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
 * 加载操作日志列表并更新表格数据、总条数与加载状态。
 * @param 无
 * @returns 返回 Promise，在列表加载完成后结束。
 */
async function loadList() {
  loading.value = true;
  try {
    const { data } = await listOperLog(buildListParams());
    rows.value = pickRows(data);
    total.value = Number(data?.total) || rows.value.length;
  } finally {
    loading.value = false;
  }
}

/**
 * 执行查询操作，重置到第一页并重新拉取日志列表。
 * @param 无
 * @returns 无返回值。
 */
function handleQuery() {
  queryParams.pageNum = 1;
  loadList();
}

/**
 * 重置查询表单和分页参数后重新加载日志列表。
 * @param 无
 * @returns 无返回值。
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
 * 处理分页变化，兼容页码切换与每页条数变更。
 * @param page 当前页码。
 * @param pageSize 当前每页条数，可选。
 * @returns 无返回值。
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
 * 打开日志详情弹窗并写入当前行详情数据。
 * @param row 当前选中的日志行数据。
 * @returns 无返回值。
 */
function openDetail(row: RowData) {
  detail.value = row;
  detailOpen.value = true;
}

/**
 * 批量删除选中的日志记录并清空勾选状态。
 * @param 无
 * @returns 返回 Promise，在批量删除流程完成后结束。
 */
async function batchDelete() {
  if (!selectedRowKeys.value.length) return;
  await deleteOperLog(selectedRowKeys.value);
  selectedRowKeys.value = [];
  loadList();
}

/**
 * 清空全部操作日志并重置勾选状态后刷新列表。
 * @param 无
 * @returns 返回 Promise，在清空流程完成后结束。
 */
async function cleanAll() {
  await cleanOperLog();
  selectedRowKeys.value = [];
  loadList();
}

onMounted(loadList);
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <AForm :model="queryParams" :label-col="{ span: 5, md: 7 }">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="操作模块" class="m-0">
                  <AInput v-model:value="queryParams.title" allow-clear placeholder="请输入操作模块" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="操作人" class="m-0">
                  <AInput v-model:value="queryParams.operUserName" allow-clear placeholder="请输入操作人" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
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
              <ACol :span="24" :md="12" :lg="6">
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
              <ACol :span="24" :md="12" :lg="12">
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
          </div>
        </div>
      </AForm>
    </ACard>
    <ACard
      title="操作日志"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <ASpace>
          <AButton danger size="small" :disabled="!selectedRowKeys.length" @click="batchDelete">批量删除</AButton>
          <AButton danger size="small" ghost @click="cleanAll">清空日志</AButton>
        </ASpace>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
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

    <AModal v-model:open="detailOpen" title="操作日志详情" width="860px" :footer="null">
      <ADescriptions bordered size="small" :column="2" :label-style="{ whiteSpace: 'nowrap' }">
        <ADescriptionsItem label="日志ID">{{ detail.id }}</ADescriptionsItem>
        <ADescriptionsItem label="操作模块">{{ detail.title }}</ADescriptionsItem>
        <ADescriptionsItem label="操作类型">
          {{ operTypeMap[Number(detail.operType)] ?? detail.operType }}
        </ADescriptionsItem>
        <ADescriptionsItem label="请求方式">{{ detail.requestMethod }}</ADescriptionsItem>
        <ADescriptionsItem label="操作人">{{ detail.operUserName }}</ADescriptionsItem>
        <ADescriptionsItem label="IP地址">{{ detail.ip }}</ADescriptionsItem>
        <ADescriptionsItem label="请求URL" :span="2">{{ detail.requestUrl }}</ADescriptionsItem>
        <ADescriptionsItem label="调用方法" :span="2">{{ detail.method }}</ADescriptionsItem>
        <ADescriptionsItem label="请求参数" :span="2">
          <div class="max-h-120px overflow-y-auto break-all">{{ detail.requestParam }}</div>
        </ADescriptionsItem>
        <ADescriptionsItem label="返回结果" :span="2">
          <div class="max-h-120px overflow-y-auto break-all">{{ detail.responseResult }}</div>
        </ADescriptionsItem>
        <ADescriptionsItem label="状态">{{ detail.status === 1 ? '成功' : '失败' }}</ADescriptionsItem>
        <ADescriptionsItem label="耗时">{{ detail.costTime }} ms</ADescriptionsItem>
        <ADescriptionsItem label="操作时间" :span="2">{{ detail.operTime }}</ADescriptionsItem>
        <ADescriptionsItem v-if="detail.errorMsg" label="错误信息" :span="2">
          <span class="text-red-500">{{ detail.errorMsg }}</span>
        </ADescriptionsItem>
      </ADescriptions>
    </AModal>
  </div>
</template>
