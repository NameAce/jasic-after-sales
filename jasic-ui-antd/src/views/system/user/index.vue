<script setup lang="ts">
/**
 * 系统管理 — 用户：分页列表、增删改、分配角色/区域、强退等（对接后端 system 域接口）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { FormInstance } from 'ant-design-vue';
import { tagColorEnabled } from '@/constants/list-status-tag';
import {
  addUser,
  assignUserRegions,
  assignUserRoles,
  deleteUser,
  getUser,
  getUserRegions,
  kickoutUser,
  listRegion,
  listUser,
  resetPwd,
  roleOptions,
  updateUser
} from '@/service/api';
import type { SysUserQuery } from '@/service/api';
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { usePageSearchFilterCollapse } from '@/hooks/common/page-search-filter-collapse';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableActionColumn, withAntTableActionColumn } from '@/utils/table-action-width';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { applyDateTimeColumnRender } from '@/utils/datetime';
import { readRouteQueryNumber, useRouteQueryFilterSync } from '@/utils/route-query-filter-sync';
import PageSearchExpandButton from '@/components/custom/page-search-expand-button.vue';

type RowData = Record<string, any>;

/** 操作列宽：本页 6 个链接按钮一排展示 */
const USER_ACTION_COL_WIDTH = 520;
/** 行内操作相关按钮权限（任一即可展示操作列） */
const USER_ROW_ACTION_AUTH_CODES = [
  'system:user:update',
  'system:user:resetPwd',
  'system:user:kickout',
  'system:user:remove'
];

// 会话与 RBAC（列表按钮显隐）
const authStore = useAuthStore();
const { hasAuth } = useAuth();

/** 当前角色是否具备任一用户行内操作权限（含总部绑定大区） */
const showUserTableActionColumn = computed(
  () =>
    hasAuth(USER_ROW_ACTION_AUTH_CODES) ||
    (authStore.userInfo.currentSubjectType === 'HQ' && hasAuth(['system:region:list', 'system:region:assign']))
);

const userListTableScrollMinX = computed(() => 1040 + (showUserTableActionColumn.value ? USER_ACTION_COL_WIDTH : 0));
// 表格区域滚动 Hook
const { tableWrapperRef, scrollConfig } = useTableScroll(userListTableScrollMinX);
const pageMenuTitle = useRouteMenuTitle();

// 列表分页、表单与分配弹窗等业务状态（含大区/角色勾选）

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

const roleOpts = ref<Array<{ label: string; value: number }>>([]);
const regionOpts = ref<Array<{ label: string; value: number }>>([]);

const editOpen = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref<FormInstance | null>(null);
const editForm = reactive<RowData>({
  id: undefined,
  username: '',
  password: '',
  realName: '',
  phone: '',
  status: 1,
  email: ''
});

const roleOpen = ref(false);
const roleSubmitting = ref(false);
const roleUserId = ref<number | undefined>(undefined);
const roleValues = ref<number[]>([]);

const resetOpen = ref(false);
const resetSubmitting = ref(false);
const resetPwdFormRef = ref<FormInstance | null>(null);
const resetForm = reactive({
  userId: undefined as number | undefined,
  password: ''
});

const regionOpen = ref(false);
const regionSubmitting = ref(false);
const regionUserId = ref<number | undefined>(undefined);
const regionValues = ref<number[]>([]);

const route = useRoute();
const router = useRouter();
const skipRouteFilterReload = ref(false);

const systemUserSearchFilter = usePageSearchFilterCollapse(4);

// Toolbar 分页与筛选字段
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  realName: '',
  phone: '',
  status: undefined as number | undefined
});

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];
// 中国大陆手机号正则（表单校验）
const mobileReg = /^1[3-9]\d{9}$/;

const editFormRules = computed(() => {
  const isAdd = !editForm.id;
  return {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: isAdd ? [{ required: true, message: '请输入密码', trigger: 'blur' }] : [],
    realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: mobileReg, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }]
  };
});

const resetPwdRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
};

// 用户管理表格列定义（无行内操作权限时不展示操作列）
const columns = computed(() =>
  withAntTableActionColumn(
    applyDateTimeColumnRender([
      { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
      { title: '用户名', dataIndex: 'username', key: 'username', width: 160 },
      { title: '姓名', dataIndex: 'realName', key: 'realName', width: 160 },
      { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email',
        width: 220,
        ellipsis: true
      },
      { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
      { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 }
    ]),
    showUserTableActionColumn.value,
    createAntTableActionColumn({ width: USER_ACTION_COL_WIDTH })
  )
);

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
 * 作用：构造数据或配置：buildListParams。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildListParams(): SysUserQuery {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    username: query.username.trim() || undefined,
    realName: query.realName.trim() || undefined,
    phone: query.phone.trim() || undefined,
    status: query.status,
    ...currentTargetCompanyParams()
  };
}

/**
 * 作用：加载数据：loadData。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadData() {
  clearListMsgs();
  loading.value = true;
  try {
    const flat = await listUser(buildListParams());
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
 * 作用：确保前置数据已加载：ensureAssignOptions。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function ensureAssignOptions() {
  if (!roleOpts.value.length) {
    const { data } = await roleOptions(currentTargetCompanyParams());
    const list = pickRows(data);
    roleOpts.value = list
      .map((r: RowData) => {
        const id = Number(r.id ?? r.roleId ?? r.value);
        return Number.isFinite(id)
          ? {
              value: id,
              label: String(r.roleName ?? r.label ?? r.roleKey ?? id)
            }
          : null;
      })
      .filter(Boolean) as Array<{ label: string; value: number }>;
  }
}

/**
 * 作用：页面内业务方法：currentCompanyId。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function currentCompanyId() {
  const cid = Number(authStore.userInfo.currentCompanyId);
  return Number.isFinite(cid) && cid > 0 ? cid : null;
}

/**
 * 作用：页面内业务方法：currentTargetCompanyParams。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function currentTargetCompanyParams() {
  const targetCompanyId = currentCompanyId();
  return targetCompanyId === null ? {} : { targetCompanyId };
}

/**
 * 作用：页面内业务方法：canBindRegion。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function canBindRegion() {
  return (
    authStore.userInfo.currentSubjectType === 'HQ' &&
    currentCompanyId() !== null &&
    hasAuth(['system:region:list', 'system:region:assign'])
  );
}

/**
 * 作用：加载数据：loadRegionOptions。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadRegionOptions() {
  const cid = currentCompanyId();
  if (cid === null) {
    regionOpts.value = [];
    return;
  }

  const { data } = await listRegion(cid);
  const list = pickRows(data);
  regionOpts.value = list
    .map((r: RowData) => {
      const id = Number(r.id ?? r.regionId ?? r.value);
      return Number.isFinite(id) ? { value: id, label: String(r.regionName ?? r.label ?? id) } : null;
    })
    .filter(Boolean) as Array<{ label: string; value: number }>;
}

/**
 * 作用：执行查询（回到第一页）：handleSearch。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleSearch() {
  query.pageNum = 1;
  loadData();
}

/**
 * 作用：重置查询条件并刷新列表：resetQuery。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetQuery() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.username = '';
  query.realName = '';
  query.phone = '';
  query.status = undefined;

  if ('status' in route.query) {
    skipRouteFilterReload.value = true;
    const nextQuery = Object.fromEntries(Object.entries(route.query).filter(([key]) => key !== 'status'));
    router.replace({ query: nextQuery });
  }

  loadData();
}

/** 首页账号治理卡片跳转：将路由 status 回显到筛选区 */
/**
 * 作用：应用配置或路由参数：applyFiltersFromRouteQuery。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyFiltersFromRouteQuery() {
  if (!('status' in route.query)) return;
  const statusNum = readRouteQueryNumber(route.query, 'status');
  query.status = statusNum === 0 || statusNum === 1 ? statusNum : undefined;
  query.pageNum = 1;
}

useRouteQueryFilterSync({
  apply: applyFiltersFromRouteQuery,
  reload: loadData,
  watchQueryKeys: ['status'],
  skipReloadRef: skipRouteFilterReload
});

/**
 * 作用：处理交互事件：handlePaginationChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handlePaginationChange(page: number, pageSize?: number) {
  if (pageSize !== undefined && pageSize !== query.pageSize) {
    query.pageSize = pageSize;
    query.pageNum = 1;
  } else {
    query.pageNum = page;
    if (pageSize !== undefined) query.pageSize = pageSize;
  }
  loadData();
}

/**
 * 作用：页面内业务方法：openAdd。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function openAdd() {
  Object.assign(editForm, {
    id: undefined,
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    status: 1
  });
  editOpen.value = true;
}

/**
 * 作用：页面内业务方法：openEdit。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function openEdit(record: RowData) {
  const { data } = await getUser(record.id, currentTargetCompanyParams());
  const row = (data as RowData) || record;
  Object.assign(editForm, {
    id: row.id,
    username: row.username ?? '',
    password: '',
    realName: row.realName ?? '',
    phone: row.phone ?? '',
    email: row.email ?? '',
    status: row.status ?? 1
  });
  editOpen.value = true;
}

/**
 * 作用：校验并提交：submitEdit。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitEdit() {
  try {
    await editFormRef.value?.validate();
  } catch {
    return;
  }
  editSubmitting.value = true;
  try {
    if (editForm.id) {
      const res = await updateUser({
        ...editForm,
        ...currentTargetCompanyParams(),
        username: editForm.username.trim(),
        realName: editForm.realName.trim(),
        phone: editForm.phone.trim(),
        email: editForm.email?.trim()
      });
      if (!notifyOnceSuccessFromFlatResult(res, '操作成功')) return;
    } else {
      const res = await addUser({
        ...editForm,
        ...currentTargetCompanyParams(),
        username: editForm.username.trim(),
        password: editForm.password.trim(),
        realName: editForm.realName.trim(),
        phone: editForm.phone.trim(),
        email: editForm.email?.trim()
      });
      if (!notifyOnceSuccessFromFlatResult(res, '操作成功')) return;
    }
    editOpen.value = false;
    await loadData();
  } finally {
    editSubmitting.value = false;
  }
}

/**
 * 作用：删除记录：removeRow。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function removeRow(record: RowData) {
  const res = await deleteUser(record.id, currentTargetCompanyParams());
  if (!notifyOnceSuccessFromFlatResult(res, '删除成功')) return;
  await loadData();
}

/**
 * 作用：页面内业务方法：openAssignRoles。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function openAssignRoles(record: RowData) {
  await ensureAssignOptions();
  roleUserId.value = Number(record.id);
  const { data } = await getUser(record.id, currentTargetCompanyParams());
  const user = (data as RowData) || {};
  let selected: unknown[] = [];
  if (Array.isArray(user.roles)) {
    selected = user.roles
      .map((role: RowData) => role.id ?? role.roleId)
      .filter((id: unknown) => id !== null && id !== undefined);
  } else if (Array.isArray(record.roleIds)) {
    selected = record.roleIds;
  }
  roleValues.value = selected.map((x: unknown) => Number(x)).filter((x: number) => Number.isFinite(x));
  roleOpen.value = true;
}

/**
 * 作用：校验并提交：submitAssignRoles。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitAssignRoles() {
  if (!roleUserId.value) return;
  roleSubmitting.value = true;
  try {
    const res = await assignUserRoles(roleUserId.value, roleValues.value, currentTargetCompanyParams());
    if (!notifyOnceSuccessFromFlatResult(res, '分配成功')) return;
    roleOpen.value = false;
    await loadData();
  } finally {
    roleSubmitting.value = false;
  }
}

/**
 * 作用：页面内业务方法：openResetPwd。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openResetPwd(record: RowData) {
  resetForm.userId = Number(record.id);
  resetForm.password = '';
  resetOpen.value = true;
}

/**
 * 作用：校验并提交：submitResetPwd。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitResetPwd() {
  try {
    await resetPwdFormRef.value?.validate();
  } catch {
    return;
  }
  if (!resetForm.userId) return;
  resetSubmitting.value = true;
  try {
    const res = await resetPwd({
      userId: resetForm.userId,
      newPassword: resetForm.password.trim(),
      ...currentTargetCompanyParams()
    });
    if (!notifyOnceSuccessFromFlatResult(res, '重置成功')) return;
    resetOpen.value = false;
  } finally {
    resetSubmitting.value = false;
  }
}

/**
 * 作用：页面内业务方法：forceKickout。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function forceKickout(record: RowData) {
  const res = await kickoutUser(record.id, currentTargetCompanyParams());
  notifyOnceSuccessFromFlatResult(res, '操作成功');
}

/**
 * 作用：页面内业务方法：openAssignRegions。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function openAssignRegions(record: RowData) {
  if (!canBindRegion()) {
    window.$message?.warning('当前公司为空，无法绑定大区');
    return;
  }

  await ensureAssignOptions();
  await loadRegionOptions();
  const userId = Number(record.id);
  regionUserId.value = userId;
  const { data } = await getUserRegions(userId, currentTargetCompanyParams());
  if (Array.isArray(data) && data.every(item => typeof item === 'number' || typeof item === 'string')) {
    regionValues.value = data.map(item => Number(item)).filter(x => Number.isFinite(x));
  } else {
    const list = pickRows(data);
    regionValues.value = list
      .map((r: RowData) => Number(r.id ?? r.regionId ?? r.value))
      .filter((x: number) => Number.isFinite(x));
  }
  regionOpen.value = true;
}

/**
 * 作用：校验并提交：submitAssignRegions。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitAssignRegions() {
  if (!regionUserId.value) return;
  regionSubmitting.value = true;
  try {
    const res = await assignUserRegions(regionUserId.value, regionValues.value, currentTargetCompanyParams());
    if (!notifyOnceSuccessFromFlatResult(res, '绑定成功')) return;
    regionOpen.value = false;
  } finally {
    regionSubmitting.value = false;
  }
}
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <!-- 系统用户：分页列表、筛选与新增/编辑/角色/大区/重置密码 -->
    <!-- 筛选区：用户名、姓名、手机号、状态等 -->
    <ACard :bordered="false" class="card-wrapper">
      <AForm :model="query" :label-col="{ span: 5, md: 7 }">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': systemUserSearchFilter.isSearchFilterHidden(0)
                }"
              >
                <AFormItem label="用户名" class="m-0">
                  <AInput v-model:value="query.username" allow-clear placeholder="请输入用户名" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': systemUserSearchFilter.isSearchFilterHidden(1)
                }"
              >
                <AFormItem label="姓名" class="m-0">
                  <AInput v-model:value="query.realName" allow-clear placeholder="请输入姓名" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': systemUserSearchFilter.isSearchFilterHidden(2)
                }"
              >
                <AFormItem label="手机号" class="m-0">
                  <AInput v-model:value="query.phone" allow-clear placeholder="请输入手机号" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': systemUserSearchFilter.isSearchFilterHidden(3)
                }"
              >
                <AFormItem label="状态" class="m-0">
                  <ASelect
                    v-model:value="query.status"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="statusOptions"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleSearch">查询</AButton>
            <AButton :loading="loading" @click="resetQuery">重置</AButton>
            <PageSearchExpandButton
              v-if="systemUserSearchFilter.showSearchFilterExpandToggle"
              :expanded="systemUserSearchFilter.searchFilterExpanded"
              @click="systemUserSearchFilter.toggleSearchFilterExpand"
            />
          </div>
        </div>
      </AForm>
    </ACard>
    <!-- 列表区：用户表格与行内编辑/角色/大区/重置密码/强制下线 -->
    <ACard
      :title="pageMenuTitle"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <AButton v-if="hasAuth('system:user:add')" type="primary" @click="openAdd">新增</AButton>
      </template>
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
          <template v-if="column.key === 'status'">
            <ATag :color="tagColorEnabled(record.status === 1)">
              {{ record.status === 1 ? '启用' : '停用' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <ASpace :size="2" :wrap="false">
              <AButton
                v-if="hasAuth('system:user:update')"
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openEdit(record)"
              >
                编辑
              </AButton>
              <AButton
                v-if="hasAuth('system:user:update')"
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openAssignRoles(record)"
              >
                分配角色
              </AButton>
              <AButton
                v-if="canBindRegion()"
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openAssignRegions(record)"
              >
                绑定大区
              </AButton>
              <AButton
                v-if="hasAuth('system:user:resetPwd')"
                type="link"
                size="small"
                class="table-action-link--warning"
                @click="openResetPwd(record)"
              >
                重置密码
              </AButton>
              <APopconfirm
                v-if="hasAuth('system:user:kickout')"
                :title="`确认强制下线用户“${record.username || '-'}”吗？`"
                @confirm="forceKickout(record)"
              >
                <AButton type="link" size="small" class="table-action-link--warning">强制下线</AButton>
              </APopconfirm>
              <APopconfirm
                v-if="hasAuth('system:user:remove')"
                :title="`确认删除用户“${record.username || '-'}”吗？`"
                @confirm="removeRow(record)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <!-- 抽屉：新增/编辑用户、分配角色、绑定大区、重置密码 -->
    <ADrawer v-model:open="editOpen" :title="editForm.id ? '编辑用户' : '新增用户'" :width="360">
      <AForm ref="editFormRef" layout="vertical" :model="editForm" :rules="editFormRules as any">
        <AFormItem label="用户名" name="username" required>
          <AInput v-model:value="editForm.username" :disabled="!!editForm.id" />
        </AFormItem>
        <AFormItem v-if="!editForm.id" label="初始密码" name="password" required>
          <AInputPassword v-model:value="editForm.password" />
        </AFormItem>
        <AFormItem label="姓名" name="realName" required>
          <AInput v-model:value="editForm.realName" />
        </AFormItem>
        <AFormItem label="手机号" name="phone" required>
          <AInput v-model:value="editForm.phone" />
        </AFormItem>
        <AFormItem label="状态" name="status" required>
          <ARadioGroup v-model:value="editForm.status">
            <ARadio v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </ARadio>
          </ARadioGroup>
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="editOpen = false">取消</AButton>
          <AButton type="primary" :loading="editSubmitting" @click="submitEdit">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="roleOpen" title="分配角色" :width="360">
      <AForm layout="vertical">
        <AFormItem label="角色">
          <ASelect
            v-model:value="roleValues"
            mode="multiple"
            max-tag-count="responsive"
            show-search
            option-filter-prop="label"
            :options="roleOpts"
            class="w-full"
            placeholder="请选择角色"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="roleOpen = false">取消</AButton>
          <AButton type="primary" :loading="roleSubmitting" @click="submitAssignRoles">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="resetOpen" title="重置密码" :width="360">
      <AForm ref="resetPwdFormRef" layout="vertical" :model="resetForm" :rules="resetPwdRules as any">
        <AFormItem label="新密码" name="password" required>
          <AInputPassword v-model:value="resetForm.password" placeholder="请输入新密码" />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="resetOpen = false">取消</AButton>
          <AButton type="primary" :loading="resetSubmitting" @click="submitResetPwd">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="regionOpen" title="绑定大区" :width="360">
      <AForm layout="vertical">
        <AFormItem label="大区">
          <ASelect
            v-model:value="regionValues"
            mode="multiple"
            max-tag-count="responsive"
            show-search
            option-filter-prop="label"
            :options="regionOpts"
            class="w-full"
            placeholder="请选择大区"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="regionOpen = false">取消</AButton>
          <AButton type="primary" :loading="regionSubmitting" @click="submitAssignRegions">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>
