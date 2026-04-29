<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { tagColorEnabled } from '@/constants/list-status-tag';
import {
  type SysRoleVO,
  addRole,
  assignRoleMenus,
  deleteRole,
  getRole,
  listRole,
  roleDataScopeOptions,
  typeCodeMenuTree,
  updateRole
} from '@/service/api';
import { getResponseMsg } from '@/service/request/shared';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { useTableScroll } from '@/hooks/common/table';

type RowData = Record<string, any>;
type ScopeOption = { value: string; label: string; defaultOption?: boolean; disabled?: boolean };

const authStore = useAuthStore();
const { hasAuth } = useAuth();
const { tableWrapperRef, scrollConfig } = useTableScroll(900);
const loading = ref(false);
const rows = ref<RowData[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];

const queryParams = reactive({
  roleName: '',
  status: undefined as number | undefined
});

const dataScopeOptions = ref<ScopeOption[]>([]);
const dataScopeMap = ref<Record<string, string>>({});
const formDataScopeOptions = ref<ScopeOption[]>([]);

const formOpen = ref(false);
const formSubmitting = ref(false);
const formTitle = ref('新增角色');
const formModel = reactive<RowData>({
  id: undefined,
  roleName: '',
  roleKey: '',
  dataScope: '',
  orderNum: 0,
  status: 1,
  remark: ''
});

const menuOpen = ref(false);
const menuSubmitting = ref(false);
const menuTreeData = ref<any[]>([]);
const menuCheckedKeys = ref<Array<string | number>>([]);
const currentRoleId = ref<number | null>(null);

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 160 },
  { title: '角色标识', dataIndex: 'roleKey', key: 'roleKey', width: 160 },
  { title: '数据范围', dataIndex: 'dataScope', key: 'dataScope', minWidth: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '系统角色', dataIndex: 'isSystem', key: 'isSystem', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'actions', width: 260, fixed: 'right' as const }
]);

function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

function buildDataScopeMap(options: ScopeOption[]) {
  return (options || []).reduce<Record<string, string>>((map, option) => {
    map[option.value] = option.label;
    return map;
  }, {});
}

function mergeLegacyOption(options: ScopeOption[], currentValue?: string) {
  const result = [...(options || [])];
  if (currentValue && !result.some(item => item.value === currentValue)) {
    result.push({
      value: currentValue,
      label: `${currentValue}（历史值）`,
      disabled: true
    });
  }
  return result;
}

function syncFormDataScopeOptions(currentValue = String(formModel.dataScope || '')) {
  formDataScopeOptions.value = mergeLegacyOption(dataScopeOptions.value, currentValue);
}

function getDefaultDataScope() {
  const defaultOption = dataScopeOptions.value.find(item => item.defaultOption);
  return defaultOption?.value || 'SELF';
}

function isValidDataScope(value: string) {
  return dataScopeOptions.value.some(item => item.value === value);
}

async function loadDataScopeMap() {
  try {
    const { data } = await roleDataScopeOptions();
    const list = (Array.isArray(data) ? data : []) as ScopeOption[];
    dataScopeOptions.value = list;
    dataScopeMap.value = buildDataScopeMap(list);
    syncFormDataScopeOptions();
  } catch {
    dataScopeOptions.value = [];
    dataScopeMap.value = {};
    formDataScopeOptions.value = [];
  }
}

async function loadList(page = pageNum.value) {
  loading.value = true;
  pageNum.value = page;
  try {
    const { data } = await listRole({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      roleName: queryParams.roleName || undefined,
      status: queryParams.status
    });

    rows.value = pickRows(data);
    total.value = Number(data?.total) || rows.value.length;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  loadList(1);
}

function resetSearch() {
  queryParams.roleName = '';
  queryParams.status = undefined;
  handleSearch();
}

function handleTablePageChange(page: number, pageSizeArg?: number) {
  if (pageSizeArg !== undefined && pageSizeArg !== pageSize.value) {
    pageSize.value = pageSizeArg;
    loadList(1);
    return;
  }
  loadList(page);
}

function openAdd() {
  formTitle.value = '新增角色';
  Object.assign(formModel, {
    id: undefined,
    roleName: '',
    roleKey: '',
    dataScope: getDefaultDataScope(),
    orderNum: 0,
    status: 1,
    remark: ''
  });
  syncFormDataScopeOptions(String(formModel.dataScope));
  formOpen.value = true;
}

async function openEdit(record: RowData) {
  formTitle.value = '编辑角色';
  const { data } = await getRole(record.id);
  const role = (data as RowData) || record;
  Object.assign(formModel, {
    id: role.id,
    roleName: role.roleName ?? '',
    roleKey: role.roleKey ?? '',
    dataScope: role.dataScope ?? '',
    orderNum: role.orderNum ?? 0,
    status: role.status ?? 1,
    remark: role.remark ?? ''
  });
  syncFormDataScopeOptions(String(formModel.dataScope));
  formOpen.value = true;
}

async function submitForm() {
  if (!String(formModel.roleName || '').trim()) {
    window.$message?.warning('请输入角色名称');
    return;
  }
  if (!String(formModel.roleKey || '').trim()) {
    window.$message?.warning('请输入角色标识');
    return;
  }
  if (!String(formModel.dataScope || '').trim()) {
    window.$message?.warning('请选择数据范围');
    return;
  }
  if (!isValidDataScope(String(formModel.dataScope))) {
    window.$message?.warning('请选择当前公司允许的数据范围');
    return;
  }

  formSubmitting.value = true;
  try {
    const payload: SysRoleVO = {
      id: Number(formModel.id) || 0,
      roleName: String(formModel.roleName).trim(),
      roleKey: String(formModel.roleKey).trim(),
      dataScope: String(formModel.dataScope).trim(),
      orderNum: Number(formModel.orderNum) || 0,
      status: Number(formModel.status) || 0,
      remark: String(formModel.remark || '')
    };
    if (payload.id) {
      const { response } = await updateRole(payload);
      window.$message?.success(getResponseMsg(response, '操作成功'));
    } else {
      const { response } = await addRole(payload);
      window.$message?.success(getResponseMsg(response, '操作成功'));
    }
    formOpen.value = false;
    await loadList(pageNum.value);
  } finally {
    formSubmitting.value = false;
  }
}

async function removeRole(record: RowData) {
  const { response } = await deleteRole(record.id);
  window.$message?.success(getResponseMsg(response, '删除成功'));
  await loadList(pageNum.value);
}

async function openAssignMenu(record: RowData) {
  const typeCode = String(authStore.userInfo.currentTypeCode || '');
  if (!typeCode) {
    window.$message?.warning('当前账号缺少公司类型编码，无法分配菜单');
    return;
  }

  currentRoleId.value = Number(record.id);
  menuCheckedKeys.value = [];

  const [treeRes, roleRes] = await Promise.all([typeCodeMenuTree(typeCode), getRole(record.id)]);
  menuTreeData.value = pickRows(treeRes.data);
  menuCheckedKeys.value = Array.isArray(roleRes.data?.menuIds) ? roleRes.data.menuIds : [];
  menuOpen.value = true;
}

async function submitAssignMenu() {
  if (!currentRoleId.value) return;

  menuSubmitting.value = true;
  try {
    const { response } = await assignRoleMenus(currentRoleId.value, menuCheckedKeys.value);
    menuOpen.value = false;
    window.$message?.success(getResponseMsg(response, '分配成功'));
  } finally {
    menuSubmitting.value = false;
  }
}

onMounted(async () => {
  await loadDataScopeMap();
  await loadList(1);
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <AForm :model="queryParams" :label-col="{ span: 5, md: 7 }">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="角色名称" class="m-0">
                  <AInput v-model:value="queryParams.roleName" allow-clear placeholder="请输入角色名称" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="状态" class="m-0">
                  <ASelect
                    v-model:value="queryParams.status"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="[
                      { label: '启用', value: 1 },
                      { label: '停用', value: 0 }
                    ]"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleSearch">搜索</AButton>
            <AButton @click="resetSearch">重置</AButton>
          </div>
        </div>
      </AForm>
    </ACard>
    <ACard
      title="系统管理-角色"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <AButton v-if="hasAuth('system:role:add')" type="primary" @click="openAdd">新增</AButton>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="{
          current: pageNum,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handleTablePageChange
        }"
        row-key="id"
        size="small"
        class="h-full"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dataScope'">
            {{ dataScopeMap[record.dataScope] || record.dataScope || '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <ATag :color="tagColorEnabled(record.status === 1)">
              {{ record.status === 1 ? '启用' : '停用' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'isSystem'">
            <ATag v-if="record.isSystem === 1" color="warning">是</ATag>
            <span v-else>否</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <ASpace :size="2" wrap>
              <AButton
                v-if="hasAuth('system:role:update')"
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openEdit(record)"
              >
                编辑
              </AButton>
              <AButton
                v-if="hasAuth('system:role:update')"
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openAssignMenu(record)"
              >
                分配菜单
              </AButton>
              <APopconfirm
                v-if="hasAuth('system:role:remove')"
                :title="`确认删除角色“${record.roleName || '-'}”吗？`"
                :disabled="record.isSystem === 1"
                @confirm="removeRole(record)"
              >
                <AButton type="link" size="small" danger :disabled="record.isSystem === 1">删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer v-model:open="formOpen" :title="formTitle" :width="720">
      <AForm layout="vertical">
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="角色名称" required>
              <AInput v-model:value="formModel.roleName" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="角色标识" required>
              <AInput v-model:value="formModel.roleKey" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="数据范围" required>
              <ASelect v-model:value="formModel.dataScope" :options="formDataScopeOptions" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="排序">
              <AInputNumber v-model:value="formModel.orderNum" :min="0" class="w-full" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="状态">
              <ARadioGroup v-model:value="formModel.status">
                <ARadio v-for="item in statusOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="备注">
              <ATextarea v-model:value="formModel.remark" :rows="2" />
            </AFormItem>
          </ACol>
        </ARow>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="formOpen = false">取消</AButton>
          <AButton type="primary" :loading="formSubmitting" @click="submitForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="menuOpen" title="分配菜单权限" :width="640">
      <ATree
        v-model:checked-keys="menuCheckedKeys"
        checkable
        :tree-data="menuTreeData"
        :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
        class="max-h-420px overflow-auto"
      />
      <template #footer>
        <ASpace :size="16">
          <AButton @click="menuOpen = false">取消</AButton>
          <AButton type="primary" :loading="menuSubmitting" @click="submitAssignMenu">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>
