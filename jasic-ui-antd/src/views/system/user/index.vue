<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
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
import { getResponseMsg } from '@/service/request/shared';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { useTableScroll } from '@/hooks/common/table';

type RowData = Record<string, any>;

const { tableWrapperRef, scrollConfig } = useTableScroll(1250);

const loading = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);
const roleOpts = ref<Array<{ label: string; value: number }>>([]);
const regionOpts = ref<Array<{ label: string; value: number }>>([]);

const editOpen = ref(false);
const editSubmitting = ref(false);
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
const resetForm = reactive({
  userId: undefined as number | undefined,
  password: ''
});

const regionOpen = ref(false);
const regionSubmitting = ref(false);
const regionUserId = ref<number | undefined>(undefined);
const regionValues = ref<number[]>([]);
const authStore = useAuthStore();
const { hasAuth } = useAuth();

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
const mobileReg = /^1[3-9]\d{9}$/;

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 160 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 160 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 220, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 360, fixed: 'right' as const }
]);

/**
 * 从接口响应中提取列表数组，兼容 records 包装结构。
 * @param data 接口返回数据。
 * @returns 可用于表格渲染的行数据数组。
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/** 与 jasic-ui `views/system/user/index.vue` 的 `listUser(this.queryParams)` 入参一致（pageNum/pageSize + username/realName/phone/status） */
/**
 * 构建用户列表查询参数。
 * @param 无
 * @returns 返回请求用户列表接口的查询对象。
 */
function buildListParams(): SysUserQuery {
  return {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    username: query.username.trim() || undefined,
    realName: query.realName.trim() || undefined,
    phone: query.phone.trim() || undefined,
    status: query.status
  };
}

/**
 * 加载用户列表数据并更新表格状态。
 * @param 无
 * @returns 返回 Promise，在列表加载完成后结束。
 */
async function loadData() {
  loading.value = true;
  try {
    const { data } = await listUser(buildListParams());
    rows.value = pickRows(data);
    total.value = Number(data?.total) || rows.value.length;
  } finally {
    loading.value = false;
  }
}

/**
 * 懒加载分配相关下拉选项（角色）。
 * @param 无
 * @returns 返回 Promise，在选项加载完成后结束。
 */
async function ensureAssignOptions() {
  if (!roleOpts.value.length) {
    const { data } = await roleOptions();
    const list = pickRows(data);
    roleOpts.value = list
      .map((r: RowData) => {
        const id = Number(r.id ?? r.roleId ?? r.value);
        return Number.isFinite(id) ? { value: id, label: String(r.roleName ?? r.label ?? r.roleKey ?? id) } : null;
      })
      .filter(Boolean) as Array<{ label: string; value: number }>;
  }
}

/**
 * 获取当前登录用户所属公司 ID。
 * @param 无
 * @returns 返回有效公司 ID，无效时返回 null。
 */
function currentCompanyId() {
  const cid = Number(authStore.userInfo.currentCompanyId);
  return Number.isFinite(cid) && cid > 0 ? cid : null;
}

/**
 * 判断当前用户是否具备绑定大区的前置条件和权限。
 * @param 无
 * @returns 满足绑定条件返回 true，否则返回 false。
 */
function canBindRegion() {
  return (
    authStore.userInfo.currentSubjectType === 'HQ' &&
    currentCompanyId() !== null &&
    hasAuth(['system:region:list', 'system:region:assign'])
  );
}

/**
 * 加载当前公司可绑定的大区选项。
 * @param 无
 * @returns 返回 Promise，在大区选项加载完成后结束。
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
 * 执行查询并重置到第一页。
 * @param 无
 * @returns 无返回值。
 */
function handleSearch() {
  query.pageNum = 1;
  loadData();
}

/**
 * 重置查询条件并重新加载用户列表。
 * @param 无
 * @returns 无返回值。
 */
function resetQuery() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.username = '';
  query.realName = '';
  query.phone = '';
  query.status = undefined;
  loadData();
}

/**
 * 处理分页变化并刷新列表。
 * @param page 当前页码。
 * @param pageSize 当前每页条数，可选。
 * @returns 无返回值。
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
 * 打开新增用户抽屉并初始化表单。
 * @param 无
 * @returns 返回 Promise，在弹窗状态更新后结束。
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
 * 打开编辑用户抽屉并回填用户信息。
 * @param record 当前选中的用户行数据。
 * @returns 返回 Promise，在用户详情加载并回填后结束。
 */
async function openEdit(record: RowData) {
  const { data } = await getUser(record.id);
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
 * 提交新增或编辑用户表单。
 * @param 无
 * @returns 返回 Promise，在提交流程结束后完成。
 */
async function submitEdit() {
  const isAdd = !editForm.id;
  if (!editForm.username?.trim()) {
    window.$message?.warning('请输入用户名');
    return;
  }
  if (isAdd && !editForm.password?.trim()) {
    window.$message?.warning('请输入密码');
    return;
  }
  if (!editForm.realName?.trim()) {
    window.$message?.warning('请输入姓名');
    return;
  }
  if (!editForm.phone?.trim()) {
    window.$message?.warning('请输入手机号');
    return;
  }
  if (!mobileReg.test(editForm.phone.trim())) {
    window.$message?.warning('请输入正确的手机号');
    return;
  }
  if (!Number.isFinite(Number(editForm.status))) {
    window.$message?.warning('请选择状态');
    return;
  }
  editSubmitting.value = true;
  try {
    if (editForm.id) {
      const { response } = await updateUser({
        ...editForm,
        username: editForm.username.trim(),
        realName: editForm.realName.trim(),
        phone: editForm.phone.trim(),
        email: editForm.email?.trim()
      });
      window.$message?.success(getResponseMsg(response, '操作成功'));
    } else {
      const { response } = await addUser({
        ...editForm,
        username: editForm.username.trim(),
        password: editForm.password.trim(),
        realName: editForm.realName.trim(),
        phone: editForm.phone.trim(),
        email: editForm.email?.trim()
      });
      window.$message?.success(getResponseMsg(response, '操作成功'));
    }
    editOpen.value = false;
    await loadData();
  } finally {
    editSubmitting.value = false;
  }
}

/**
 * 删除指定用户并刷新列表。
 * @param record 待删除用户行数据。
 * @returns 返回 Promise，在删除完成后结束。
 */
async function removeRow(record: RowData) {
  const { response } = await deleteUser(record.id);
  window.$message?.success(getResponseMsg(response, '删除成功'));
  await loadData();
}

/**
 * 打开分配角色抽屉并回填已选角色。
 * @param record 当前选中的用户行数据。
 * @returns 返回 Promise，在角色数据准备完成后结束。
 */
async function openAssignRoles(record: RowData) {
  await ensureAssignOptions();
  roleUserId.value = Number(record.id);
  const { data } = await getUser(record.id);
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
 * 提交用户角色分配结果。
 * @param 无
 * @returns 返回 Promise，在分配流程结束后完成。
 */
async function submitAssignRoles() {
  if (!roleUserId.value) return;
  roleSubmitting.value = true;
  try {
    const { response } = await assignUserRoles(roleUserId.value, roleValues.value);
    roleOpen.value = false;
    window.$message?.success(getResponseMsg(response, '分配成功'));
    await loadData();
  } finally {
    roleSubmitting.value = false;
  }
}

/**
 * 打开重置密码抽屉并初始化表单。
 * @param record 当前选中的用户行数据。
 * @returns 无返回值。
 */
function openResetPwd(record: RowData) {
  resetForm.userId = Number(record.id);
  resetForm.password = '';
  resetOpen.value = true;
}

/**
 * 提交重置密码请求。
 * @param 无
 * @returns 返回 Promise，在重置流程结束后完成。
 */
async function submitResetPwd() {
  if (!resetForm.userId || !resetForm.password.trim()) {
    window.$message?.warning('请输入新密码');
    return;
  }
  resetSubmitting.value = true;
  try {
    const { response } = await resetPwd({ userId: resetForm.userId, newPassword: resetForm.password.trim() });
    resetOpen.value = false;
    window.$message?.success(getResponseMsg(response, '重置成功'));
  } finally {
    resetSubmitting.value = false;
  }
}

/**
 * 强制指定用户下线。
 * @param record 当前选中的用户行数据。
 * @returns 返回 Promise，在下线操作完成后结束。
 */
async function forceKickout(record: RowData) {
  const { response } = await kickoutUser(record.id);
  window.$message?.success(getResponseMsg(response, '操作成功'));
}

/**
 * 打开绑定大区抽屉并加载用户已绑定大区。
 * @param record 当前选中的用户行数据。
 * @returns 返回 Promise，在大区数据准备完成后结束。
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
  const { data } = await getUserRegions(userId);
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
 * 提交用户绑定大区结果。
 * @param 无
 * @returns 返回 Promise，在绑定流程结束后完成。
 */
async function submitAssignRegions() {
  if (!regionUserId.value) return;
  regionSubmitting.value = true;
  try {
    const { response } = await assignUserRegions(regionUserId.value, regionValues.value);
    regionOpen.value = false;
    window.$message?.success(getResponseMsg(response, '绑定成功'));
  } finally {
    regionSubmitting.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <AForm :model="query" :label-col="{ span: 5, md: 7 }">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="用户名" class="m-0">
                  <AInput v-model:value="query.username" allow-clear placeholder="请输入用户名" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="姓名" class="m-0">
                  <AInput v-model:value="query.realName" allow-clear placeholder="请输入姓名" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="手机号" class="m-0">
                  <AInput v-model:value="query.phone" allow-clear placeholder="请输入手机号" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
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
          </div>
        </div>
      </AForm>
    </ACard>
    <ACard
      title="系统管理-用户"
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
            <ASpace :size="2" wrap>
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

    <ADrawer v-model:open="editOpen" :title="editForm.id ? '编辑用户' : '新增用户'" :width="360">
      <AForm layout="vertical">
        <AFormItem label="用户名" required>
          <AInput v-model:value="editForm.username" :disabled="!!editForm.id" />
        </AFormItem>
        <AFormItem v-if="!editForm.id" label="初始密码" required>
          <AInputPassword v-model:value="editForm.password" />
        </AFormItem>
        <AFormItem label="姓名" required>
          <AInput v-model:value="editForm.realName" />
        </AFormItem>
        <AFormItem label="手机号" required>
          <AInput v-model:value="editForm.phone" />
        </AFormItem>
        <AFormItem label="状态" required>
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
      <AForm layout="vertical">
        <AFormItem label="新密码" required>
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
