<script setup lang="ts">
/**
 * 系统管理 — 角色：分页列表、数据范围、分配菜单与角色模板维护（对接后端角色接口）。
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'ant-design-vue';
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
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { ADAPTIVE_MODAL_FORM_WIDE_MIN_COUNT, adaptiveModalWidth } from '@/hooks/common/modal-form-layout';
import { usePageSearchFilterCollapse } from '@/hooks/common/page-search-filter-collapse';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableActionColumn } from '@/utils/table-action-width';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { computeExpandedKeysForCheckedMenuTree } from '@/utils/tree-expand-keys';
import PageSearchExpandButton from '@/components/custom/page-search-expand-button.vue';

type RowData = Record<string, any>;
type ScopeOption = {
  value: string;
  label: string;
  defaultOption?: boolean;
  disabled?: boolean;
};

const authStore = useAuthStore();
const { hasAuth } = useAuth();
const pageMenuTitle = useRouteMenuTitle();

const ROLE_ACTION_COL_WIDTH = 200;
const roleListTableScrollMinX = computed(() => 890 + ROLE_ACTION_COL_WIDTH);
const { tableWrapperRef, scrollConfig } = useTableScroll(roleListTableScrollMinX);
const loading = ref(false);
const rows = ref<RowData[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
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

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];

const systemRoleSearchFilter = usePageSearchFilterCollapse(2);

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
const roleFormRef = ref<FormInstance | null>(null);
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
/** 分配菜单抽屉打开时，展开所有「已勾选节点」的祖先路径，便于直接看到选中项 */
const menuExpandedKeys = ref<Array<string | number>>([]);
const currentRoleId = ref<number | null>(null);

/** 新增/编辑抽屉内 AFormItem 数量（与模板一致；≤6 单列窄抽屉，≥7 才与全局规则一致加宽并两列） */
const ROLE_EDIT_FORM_FIELD_COUNT = 6;
const roleFormDrawerWidth = computed(() => adaptiveModalWidth(560, ROLE_EDIT_FORM_FIELD_COUNT));
const roleFormOperateColSpan = computed(() =>
  ROLE_EDIT_FORM_FIELD_COUNT >= ADAPTIVE_MODAL_FORM_WIDE_MIN_COUNT ? 12 : 24
);

// 角色列表表格列
const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 160 },
  { title: '角色标识', dataIndex: 'roleKey', key: 'roleKey', width: 160 },
  {
    title: '数据范围',
    dataIndex: 'dataScope',
    key: 'dataScope',
    minWidth: 140
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '系统角色', dataIndex: 'isSystem', key: 'isSystem', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  createAntTableActionColumn({ width: ROLE_ACTION_COL_WIDTH })
]);

/**
 * 作用：从分页或数组结构中解析表格行数据。
 * @param data - 接口返回体
 * @returns 行数组
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 作用：将数据范围选项列表转为 value→label 映射表。
 * @param options - 数据范围选项
 * @returns 映射对象
 */
function buildDataScopeMap(options: ScopeOption[]) {
  return (options || []).reduce<Record<string, string>>((map, option) => {
    map[option.value] = option.label;
    return map;
  }, {});
}

/**
 * 作用：合并历史遗留的数据范围值到选项列表（禁用展示）。
 * @param options - 当前可选列表
 * @param currentValue - 当前表单中的旧值
 * @returns 合并后的选项数组
 */
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

const roleFormRules = computed(() => ({
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
  dataScope: [
    { required: true, message: '请选择数据范围', trigger: 'change' },
    {
      validator: async () => {
        const v = String(formModel.dataScope || '').trim();
        if (!v) {
          return Promise.reject(new Error('请选择数据范围'));
        }
        if (!isValidDataScope(v)) {
          return Promise.reject(new Error('请选择当前公司允许的数据范围'));
        }
        return Promise.resolve();
      },
      trigger: 'change'
    }
  ]
}));

/**
 * 作用：根据主数据范围选项同步表单内下拉可选项（含历史值兜底）。
 * @param currentValue - 当前 dataScope 字符串
 * @returns {void} 无
 */
function syncFormDataScopeOptions(currentValue = String(formModel.dataScope || '')) {
  formDataScopeOptions.value = mergeLegacyOption(dataScopeOptions.value, currentValue);
}

/**
 * 作用：读取后端标记的默认数据范围编码。
 * @param 无
 * @returns 默认 value，无则 SELF
 */
function getDefaultDataScope() {
  const defaultOption = dataScopeOptions.value.find(item => item.defaultOption);
  return defaultOption?.value || 'SELF';
}

/**
 * 作用：校验 dataScope 是否在当前公司允许列表内。
 * @param value - 数据范围编码
 * @returns 是否合法
 */
function isValidDataScope(value: string) {
  return dataScopeOptions.value.some(item => item.value === value);
}

/**
 * 作用：加载数据范围字典并更新映射与表单选项。
 * @param 无
 * @returns 返回 Promise，请求结束后结束
 */
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

/**
 * 作用：分页拉取角色列表并更新表格。
 * @param page - 目标页码，默认可不传则用当前 pageNum
 * @returns 返回 Promise，加载结束后结束
 */
async function loadList(page = pageNum.value) {
  clearListMsgs();
  loading.value = true;
  pageNum.value = page;
  try {
    const flat = await listRole({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      roleName: queryParams.roleName || undefined,
      status: queryParams.status
    });

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
 * 作用：搜索时回到第一页并刷新列表。
 * @param 无
 * @returns {void} 无
 */
function handleSearch() {
  pageNum.value = 1;
  loadList(1);
}

/**
 * 作用：重置筛选条件并重新查询。
 * @param 无
 * @returns {void} 无
 */
function resetSearch() {
  queryParams.roleName = '';
  queryParams.status = undefined;
  handleSearch();
}

/**
 * 作用：表格分页变化时重新拉取（每页条数变化时回到第一页）。
 * @param page - 页码
 * @param pageSizeArg - 每页条数，可选
 * @returns {void} 无
 */
function handleTablePageChange(page: number, pageSizeArg?: number) {
  if (pageSizeArg !== undefined && pageSizeArg !== pageSize.value) {
    pageSize.value = pageSizeArg;
    loadList(1);
    return;
  }
  loadList(page);
}

/**
 * 作用：打开新增角色抽屉并初始化表单默认值。
 * @param 无
 * @returns {void} 无
 */
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

/**
 * 作用：打开编辑抽屉并回填指定角色详情。
 * @param record - 表格行数据
 * @returns 返回 Promise，详情加载并回填后结束
 */
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

/**
 * 作用：校验并提交新增/编辑角色表单。
 * @param 无
 * @returns 返回 Promise，提交结束后结束
 */
async function submitForm() {
  try {
    await roleFormRef.value?.validate();
  } catch {
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
      const res = await updateRole(payload);
      if (!notifyOnceSuccessFromFlatResult(res, '操作成功')) return;
    } else {
      const res = await addRole(payload);
      if (!notifyOnceSuccessFromFlatResult(res, '操作成功')) return;
    }
    formOpen.value = false;
    await loadList(pageNum.value);
  } finally {
    formSubmitting.value = false;
  }
}

/**
 * 作用：删除角色并刷新当前页列表。
 * @param record - 表格行
 * @returns 返回 Promise，删除完成后结束
 */
async function removeRole(record: RowData) {
  const res = await deleteRole(record.id);
  if (!notifyOnceSuccessFromFlatResult(res, '删除成功')) return;
  await loadList(pageNum.value);
}

/**
 * 作用：打开分配菜单抽屉，加载当前公司类型下的菜单树；已勾选菜单以 GET /system/role/{id} 返回的 SysRoleVO.menuIds 为准（与后端 sys_role_menu 一致）。
 * @param record - 角色行
 * @returns 返回 Promise，数据就绪后结束
 */
async function openAssignMenu(record: RowData) {
  const typeCode = String(authStore.userInfo.currentTypeCode || '');
  if (!typeCode) {
    window.$message?.warning('当前账号缺少公司类型编码，无法分配菜单');
    return;
  }

  currentRoleId.value = Number(record.id);
  menuCheckedKeys.value = [];
  menuExpandedKeys.value = [];

  const [treeRes, roleRes] = await Promise.all([typeCodeMenuTree(typeCode), getRole(record.id)]);
  menuTreeData.value = pickRows(treeRes.data);
  const role = roleRes.data as SysRoleVO | undefined;
  const ids = role?.menuIds;
  menuCheckedKeys.value = Array.isArray(ids)
    ? ids.map((id: unknown) => Number(id)).filter((id: number) => !Number.isNaN(id))
    : [];
  menuExpandedKeys.value = computeExpandedKeysForCheckedMenuTree(menuTreeData.value, menuCheckedKeys.value);
  await nextTick();
  menuOpen.value = true;
}

/**
 * 作用：提交角色菜单权限勾选结果。
 * @param 无
 * @returns 返回 Promise，提交完成后结束
 */
async function submitAssignMenu() {
  if (!currentRoleId.value) return;

  menuSubmitting.value = true;
  try {
    const res = await assignRoleMenus(currentRoleId.value, menuCheckedKeys.value);
    if (!notifyOnceSuccessFromFlatResult(res, '分配成功')) return;
    menuOpen.value = false;
  } finally {
    menuSubmitting.value = false;
  }
}

/**
 * 作用：挂载时先加载数据范围字典再加载列表。
 * @param 无
 * @returns 返回 Promise，初始化完成后结束
 */
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
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': systemRoleSearchFilter.isSearchFilterHidden(0)
                }"
              >
                <AFormItem label="角色名称" class="m-0">
                  <AInput v-model:value="queryParams.roleName" allow-clear placeholder="请输入角色名称" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': systemRoleSearchFilter.isSearchFilterHidden(1)
                }"
              >
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
            <PageSearchExpandButton
              v-if="systemRoleSearchFilter.showSearchFilterExpandToggle"
              :expanded="systemRoleSearchFilter.searchFilterExpanded"
              @click="systemRoleSearchFilter.toggleSearchFilterExpand"
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
        <AButton v-if="hasAuth('system:role:add')" type="primary" @click="openAdd">新增</AButton>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :locale="tableListLocale"
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
            <ASpace :size="2" :wrap="false">
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

    <ADrawer v-model:open="formOpen" :title="formTitle" :width="roleFormDrawerWidth">
      <AForm ref="roleFormRef" layout="vertical" :model="formModel" :rules="roleFormRules as any">
        <ARow :gutter="16">
          <ACol :span="roleFormOperateColSpan">
            <AFormItem label="角色名称" name="roleName" required>
              <AInput v-model:value="formModel.roleName" />
            </AFormItem>
          </ACol>
          <ACol :span="roleFormOperateColSpan">
            <AFormItem label="角色标识" name="roleKey" required>
              <AInput v-model:value="formModel.roleKey" />
            </AFormItem>
          </ACol>
          <ACol :span="roleFormOperateColSpan">
            <AFormItem label="数据范围" name="dataScope" required>
              <ASelect v-model:value="formModel.dataScope" :options="formDataScopeOptions" />
            </AFormItem>
          </ACol>
          <ACol :span="roleFormOperateColSpan">
            <AFormItem label="排序">
              <AInputNumber v-model:value="formModel.orderNum" :min="0" class="w-full" />
            </AFormItem>
          </ACol>
          <ACol :span="roleFormOperateColSpan">
            <AFormItem label="状态">
              <ARadioGroup v-model:value="formModel.status">
                <ARadio v-for="item in statusOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="roleFormOperateColSpan">
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
        :key="`role-menu-${currentRoleId ?? ''}`"
        v-model:checked-keys="menuCheckedKeys"
        v-model:expanded-keys="menuExpandedKeys"
        checkable
        :tree-data="menuTreeData"
        :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
        class="overflow-auto"
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
