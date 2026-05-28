<script setup lang="ts">
/**
 * 系统管理 - 投诉与建议受理列表页：按受理状态分 Tab，支持筛选、分页和行内受理/修改动作。
 */
import { computed, onMounted, reactive, ref } from 'vue';
import dayjs, { type Dayjs } from 'dayjs';
import type { FormInstance } from 'ant-design-vue';
import type { Key } from 'ant-design-vue/es/_util/type';
import { acceptFeedback, listFeedbackManage, updateAcceptFeedback } from '@/service/api';
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useAuth } from '@/hooks/business/auth';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableActionColumn, withAntTableActionColumn } from '@/utils/table-action-width';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { applyDateTimeColumnRender } from '@/utils/datetime';

type FeedbackStatus = 'UNACCEPTED' | 'ACCEPTED';

type FeedbackRow = {
  id: number;
  submitter: string;
  phone: string;
  submitSource: string;
  submitContent: string;
  submitTime: string;
  acceptContent: string;
  acceptor: string;
  acceptTime: string;
  status: FeedbackStatus;
};

const pageMenuTitle = useRouteMenuTitle();
const { hasAuth } = useAuth();
const loading = ref(false);
const rows = ref<FeedbackRow[]>([]);
const total = ref(0);
const activeTab = ref<'UNACCEPTED' | 'ACCEPTED' | 'ALL'>('UNACCEPTED');

const { tableWrapperRef, scrollConfig } = useTableScroll(1460);
const { listFetchErrorMsg, listEmptyBackendMsg, clearListMsgs, consumeFlatError, refreshEmptySuccessMsg, setMsgFromCatch } =
  useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  phone: '',
  submitSource: undefined as string | undefined,
  submitTimeRange: undefined as [Dayjs, Dayjs] | undefined,
  acceptTimeRange: undefined as [Dayjs, Dayjs] | undefined
});
const acceptModalOpen = ref(false);
const acceptSubmitting = ref(false);
const acceptFormRef = ref<FormInstance | null>(null);
const acceptForm = reactive({
  id: 0,
  acceptReply: ''
});
const acceptFormRules = {
  acceptReply: [{ required: true, message: '请输入受理回复', trigger: 'blur' }]
};
const currentAction = ref<'accept' | 'update'>('accept');
const acceptReplyLength = computed(() => acceptForm.acceptReply.length);

const showFeedbackActionColumn = computed(() => hasAuth(['feedback:accept', 'feedback:updateAccept']));
const columns = computed(() =>
  withAntTableActionColumn(
    applyDateTimeColumnRender([
      { title: '提交人', dataIndex: 'submitter', key: 'submitter', width: 120 },
      { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 140 },
      { title: '提交来源', dataIndex: 'submitSource', key: 'submitSource', width: 160 },
      { title: '提交内容', dataIndex: 'submitContent', key: 'submitContent', minWidth: 260, ellipsis: true },
      { title: '提交时间', dataIndex: 'submitTime', key: 'submitTime', width: 180 },
      { title: '受理内容', dataIndex: 'acceptContent', key: 'acceptContent', minWidth: 260, ellipsis: true },
      { title: '受理人', dataIndex: 'acceptor', key: 'acceptor', width: 120 },
      { title: '受理时间', dataIndex: 'acceptTime', key: 'acceptTime', width: 180 }
    ]),
    showFeedbackActionColumn.value,
    createAntTableActionColumn({ width: 100 })
  )
);

/**
 * 作用：加载当前 Tab + 条件下的列表数据（模拟数据实现）。
 */
async function loadRows() {
  clearListMsgs();
  loading.value = true;
  try {
    const flat = await listFeedbackManage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      viewType: activeTab.value,
      contactPhone: query.phone.trim() || undefined,
      submitSourceName: query.submitSource || undefined,
      beginCreateTime: query.submitTimeRange?.[0]?.startOf('day').format('YYYY-MM-DD HH:mm:ss'),
      endCreateTime: query.submitTimeRange?.[1]?.endOf('day').format('YYYY-MM-DD HH:mm:ss'),
      beginAcceptTime: query.acceptTimeRange?.[0]?.startOf('day').format('YYYY-MM-DD HH:mm:ss'),
      endAcceptTime: query.acceptTimeRange?.[1]?.endOf('day').format('YYYY-MM-DD HH:mm:ss')
    });
    if (consumeFlatError(flat)) {
      rows.value = [];
      total.value = 0;
      return;
    }
    const page = ((flat as { data?: unknown })?.data || {}) as { records?: Array<Record<string, unknown>>; total?: number };
    const list = Array.isArray(page.records) ? page.records : [];
    rows.value = list.map(item => ({
      id: Number(item.id || 0),
      submitter: String(item.submitterName || ''),
      phone: String(item.contactPhone || ''),
      submitSource: String(item.submitSourceName || ''),
      submitContent: String(item.content || ''),
      submitTime: String(item.createTime || ''),
      acceptContent: String(item.acceptReply || ''),
      acceptor: String(item.acceptUserName || ''),
      acceptTime: String(item.acceptTime || ''),
      status: String(item.status || 'UNACCEPTED') as FeedbackStatus
    }));
    total.value = Number(page.total || 0);
    refreshEmptySuccessMsg(flat, rows.value.length);
  } catch (error: unknown) {
    rows.value = [];
    total.value = 0;
    setMsgFromCatch(error);
  } finally {
    loading.value = false;
  }
}

function handleTabChange(tab: Key) {
  if (tab !== 'UNACCEPTED' && tab !== 'ACCEPTED' && tab !== 'ALL') return;
  activeTab.value = tab;
  query.pageNum = 1;
  loadRows();
}

function handleSearch() {
  query.pageNum = 1;
  loadRows();
}

function resetQuery() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.phone = '';
  query.submitSource = undefined;
  query.submitTimeRange = undefined;
  query.acceptTimeRange = undefined;
  loadRows();
}

function handlePaginationChange(page: number, pageSize?: number) {
  if (pageSize !== undefined && pageSize !== query.pageSize) {
    query.pageSize = pageSize;
    query.pageNum = 1;
  } else {
    query.pageNum = page;
    if (pageSize !== undefined) query.pageSize = pageSize;
  }
  loadRows();
}

/**
 * 行内操作：按 Tab 与记录状态动态返回按钮文案。
 * - 未受理 Tab：固定显示“受理”
 * - 已受理 Tab：固定显示“修改”
 * - 全部 Tab：按行状态区分
 */
function getActionText(record: FeedbackRow) {
  if (activeTab.value === 'UNACCEPTED') return '受理';
  if (activeTab.value === 'ACCEPTED') return '修改';
  return record.status === 'ACCEPTED' ? '修改' : '受理';
}

function handleRowAction(record: FeedbackRow) {
  currentAction.value = getActionText(record) === '修改' ? 'update' : 'accept';
  if (currentAction.value === 'accept' && !hasAuth('feedback:accept')) return;
  if (currentAction.value === 'update' && !hasAuth('feedback:updateAccept')) return;
  acceptForm.id = record.id;
  acceptForm.acceptReply = currentAction.value === 'update' ? record.acceptContent || '' : '';
  acceptModalOpen.value = true;
}

async function submitAcceptForm() {
  try {
    await acceptFormRef.value?.validate();
  } catch {
    return;
  }
  acceptSubmitting.value = true;
  try {
    const payload = {
      id: acceptForm.id,
      acceptReply: acceptForm.acceptReply.trim()
    };
    if (currentAction.value === 'accept') {
      const res = await acceptFeedback(payload);
      if (!notifyOnceSuccessFromFlatResult(res, '受理成功')) return;
    } else {
      const res = await updateAcceptFeedback(payload);
      if (!notifyOnceSuccessFromFlatResult(res, '修改成功')) return;
    }
    acceptModalOpen.value = false;
    await loadRows();
  } finally {
    acceptSubmitting.value = false;
  }
}

onMounted(() => {
  loadRows();
});
</script>

<template>
  <div class="feedback-page min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" size="small" class="feedback-search-card card-wrapper">
      <ATabs :active-key="activeTab" @change="handleTabChange">
        <ATabPane key="UNACCEPTED" tab="未受理" />
        <ATabPane key="ACCEPTED" tab="已受理" />
        <ATabPane key="ALL" tab="全部" />
      </ATabs>

      <AForm :model="query" :label-col="{ span: 5, md: 7 }" class="feedback-search-form">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="联系电话" class="m-0">
                  <AInput v-model:value="query.phone" allow-clear placeholder="请输入联系电话" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="提交来源" class="m-0">
                  <AInput v-model:value="query.submitSource" allow-clear placeholder="请输入提交来源" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="提交时间" class="m-0">
                  <ARangePicker v-model:value="query.submitTimeRange" class="w-full" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="受理时间" class="m-0">
                  <ARangePicker v-model:value="query.acceptTimeRange" class="w-full" />
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
      :title="pageMenuTitle"
      :bordered="false"
      size="small"
      :body-style="{ flex: 1, overflow: 'hidden', paddingTop: '12px' }"
      class="feedback-table-card flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
        :pagination="{
          current: query.pageNum,
          pageSize: query.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handlePaginationChange
        }"
        row-key="id"
        size="small"
        class="h-full"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'acceptContent'">
            {{ record.acceptContent || '-' }}
          </template>
          <template v-else-if="column.key === 'acceptor'">
            {{ record.acceptor || '-' }}
          </template>
          <template v-else-if="column.key === 'acceptTime'">
            {{ record.acceptTime || '-' }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <AButton
              v-if="
                (getActionText(record as FeedbackRow) === '受理' && hasAuth('feedback:accept')) ||
                (getActionText(record as FeedbackRow) === '修改' && hasAuth('feedback:updateAccept'))
              "
              type="link"
              size="small"
              class="table-action-link--primary"
              @click="handleRowAction(record as FeedbackRow)"
            >
              {{ getActionText(record as FeedbackRow) }}
            </AButton>
            <span v-else>-</span>
          </template>
        </template>
      </ATable>
    </ACard>

    <AModal
      v-model:open="acceptModalOpen"
      :title="currentAction === 'accept' ? '受理投诉与建议' : '修改受理回复'"
      :confirm-loading="acceptSubmitting"
      @ok="submitAcceptForm"
    >
      <AForm ref="acceptFormRef" layout="vertical" :model="acceptForm" :rules="acceptFormRules as any">
        <AFormItem label="受理回复" name="acceptReply" required>
          <ATextarea
            v-model:value="acceptForm.acceptReply"
            :maxlength="200"
            :rows="4"
            show-count
            placeholder="请输入受理回复内容"
          />
        </AFormItem>
      </AForm>
    </AModal>
  </div>
</template>

<style scoped>
.feedback-search-card :deep(.ant-card-head) {
  min-height: 44px;
}

.feedback-table-card :deep(.ant-card-head) {
  min-height: 48px;
}

.feedback-search-form :deep(.ant-form-item) {
  margin-bottom: 0;
}
</style>
