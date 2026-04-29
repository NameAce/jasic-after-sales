<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { tagColorEnabled, tagColorPositiveNeutral } from '@/constants/list-status-tag';
import {
  addMenu,
  copyMenus,
  deleteMenu,
  getMenu,
  menuPublishOptions,
  menuTree,
  publishMenu,
  updateMenu
} from '@/service/api';
import type { SysMenuDTO } from '@/service/api';
import { getResponseMsg } from '@/service/request/shared';
import { useAuth } from '@/hooks/business/auth';
import { useTableScroll } from '@/hooks/common/table';

type RowData = Record<string, any>;
const { hasAuth } = useAuth();
const { tableWrapperRef, scrollConfig } = useTableScroll(1200);

const loading = ref(false);
const menuTreeRows = ref<RowData[]>([]);
const subjectType = ref<'PLATFORM' | 'HQ' | 'SERVICE'>('PLATFORM');
const isExpandAll = ref(true);
const expandedRowKeys = ref<Array<string | number>>([]);

const subjectTypeOptions = [
  { label: '平台', value: 'PLATFORM' },
  { label: '总部', value: 'HQ' },
  { label: '服务网点', value: 'SERVICE' }
];
const subjectTypeLabelMap: Record<'PLATFORM' | 'HQ' | 'SERVICE', string> = {
  PLATFORM: '平台',
  HQ: '总部',
  SERVICE: '服务网点'
};

const formOpen = ref(false);
const formSubmitting = ref(false);
const publishPrepLoading = ref(false);
const formTitle = ref('新增菜单');
const menuOptions = ref<RowData[]>([]);
const formModel = reactive<RowData>({
  id: undefined,
  parentId: 0,
  menuType: 'M',
  subjectType: 'PLATFORM',
  menuName: '',
  path: '',
  component: '',
  perms: '',
  icon: '',
  orderNum: 0,
  isVisible: 1,
  status: 1
});
const copyOpen = ref(false);
const copySubmitting = ref(false);
const copyTreeLoading = ref(false);
const copySourceTree = ref<RowData[]>([]);
const copyCheckedKeys = ref<Array<string | number>>([]);
const copyForm = reactive({
  sourceSubjectType: 'PLATFORM' as 'PLATFORM' | 'HQ' | 'SERVICE',
  targetSubjectType: 'HQ' as 'PLATFORM' | 'HQ' | 'SERVICE'
});

const publishOpen = ref(false);
const publishLoading = ref(false);
const publishOptionsLoading = ref(false);
const publishReturnToForm = ref(false);
const publishDialogTitle = ref('菜单发布');
const publishOptions = ref<{ typeOptions: RowData[]; templateOptions: RowData[] }>({
  typeOptions: [],
  templateOptions: []
});
const publishForm = reactive({
  menu: null as RowData | null,
  targetTypeCodes: [] as string[],
  targetTemplateIds: [] as Array<string | number>,
  syncExistingCompanies: true
});

const columns = computed(() => [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName', width: 220 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 120 },
  { title: '排序', dataIndex: 'orderNum', key: 'orderNum', width: 80 },
  { title: '权限标识', dataIndex: 'perms', key: 'perms', width: 200 },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 220, ellipsis: true },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 90 },
  { title: '可见', dataIndex: 'isVisible', key: 'isVisible', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' as const }
]);

const groupedPublishTemplates = computed(() => {
  const selectedTypeCodes = new Set(publishForm.targetTypeCodes);
  const typeLabelMap = publishOptions.value.typeOptions.reduce<Record<string, string>>((map, item) => {
    map[String(item.typeCode)] = String(item.typeName || item.typeCode || '');
    return map;
  }, {});
  const groupMap = publishOptions.value.templateOptions
    .filter(item => selectedTypeCodes.has(String(item.typeCode)))
    .reduce<Record<string, { typeCode: string; typeName: string; templates: RowData[] }>>((map, item) => {
      const typeCode = String(item.typeCode);
      if (!map[typeCode]) {
        map[typeCode] = {
          typeCode,
          typeName: typeLabelMap[typeCode] || typeCode,
          templates: []
        };
      }
      map[typeCode].templates.push(item);
      return map;
    }, {});
  return Object.values(groupMap);
});

function normalizeMenuTree(data: unknown) {
  if (Array.isArray(data)) return data as RowData[];
  if (data && typeof data === 'object') {
    const obj = data as RowData;
    if (Array.isArray(obj.records)) return obj.records;
  }
  return [];
}

function collectRowKeys(rows: RowData[]): Array<string | number> {
  const keys: Array<string | number> = [];
  const stack = [...rows];
  while (stack.length) {
    const row = stack.pop();
    if (row) {
      if (row.id !== undefined && row.id !== null) keys.push(row.id);
      if (Array.isArray(row.children) && row.children.length) {
        stack.push(...row.children);
      }
    }
  }
  return keys;
}

async function loadMenuOptions() {
  const { data } = await menuTree(String(formModel.subjectType || subjectType.value));
  menuOptions.value = [{ id: 0, menuName: '顶级菜单', children: normalizeMenuTree(data) }];
}

function openAdd(parent?: RowData) {
  formTitle.value = '新增菜单';
  Object.assign(formModel, {
    id: undefined,
    parentId: parent?.id ?? 0,
    menuType: 'M',
    subjectType: parent?.subjectType || subjectType.value,
    menuName: '',
    path: '',
    component: '',
    perms: '',
    icon: '',
    orderNum: 0,
    isVisible: 1,
    status: 1
  });
  loadMenuOptions();
  formOpen.value = true;
}

async function openCopyDialog() {
  copyForm.sourceSubjectType = 'PLATFORM';
  copyForm.targetSubjectType = 'HQ';
  copyCheckedKeys.value = [];
  copySourceTree.value = [];
  copyOpen.value = true;
  await loadCopySourceTree();
}

async function loadCopySourceTree() {
  copyTreeLoading.value = true;
  try {
    const { data } = await menuTree(copyForm.sourceSubjectType);
    copySourceTree.value = normalizeMenuTree(data);
    copyCheckedKeys.value = [];
  } finally {
    copyTreeLoading.value = false;
  }
}

async function submitCopy() {
  if (copyForm.sourceSubjectType === copyForm.targetSubjectType) {
    window.$message?.warning('源主体与目标主体不能相同');
    return;
  }

  copySubmitting.value = true;
  try {
    const { response } = await copyMenus({
      sourceSubjectType: copyForm.sourceSubjectType,
      targetSubjectType: copyForm.targetSubjectType,
      menuIds: copyCheckedKeys.value.length ? copyCheckedKeys.value : null
    });
    copyOpen.value = false;
    subjectType.value = copyForm.targetSubjectType;
    window.$message?.success(getResponseMsg(response, `拷贝成功，共 ${copyCheckedKeys.value.length || 0} 个菜单`));
    await loadList();
  } finally {
    copySubmitting.value = false;
  }
}

async function openEdit(record: RowData) {
  formTitle.value = '编辑菜单';
  const { data } = await getMenu(record.id);
  const row = (data as RowData) || record;
  Object.assign(formModel, {
    id: row.id,
    parentId: row.parentId ?? 0,
    menuType: row.menuType ?? 'M',
    subjectType: row.subjectType || subjectType.value,
    menuName: row.menuName ?? '',
    path: row.path ?? row.routePath ?? '',
    component: row.component ?? '',
    perms: row.perms ?? '',
    icon: row.icon ?? '',
    orderNum: row.orderNum ?? 0,
    isVisible: row.isVisible ?? 1,
    status: row.status ?? 1
  });
  await loadMenuOptions();
  formOpen.value = true;
}

async function openPublish(record: RowData) {
  const { data } = await getMenu(record.id);
  const menu = (data as RowData) || record;
  await openPublishDialog(menu, false);
}

async function openPublishDialog(menu: RowData, returnToForm: boolean) {
  publishDialogTitle.value = `${menu?.id ? '发布菜单' : '保存并发布'} - ${menu?.menuName || ''}`;
  publishReturnToForm.value = returnToForm;

  publishForm.menu = menu;
  publishForm.targetTypeCodes = [];
  publishForm.targetTemplateIds = [];
  publishForm.syncExistingCompanies = true;
  publishOptions.value = { typeOptions: [], templateOptions: [] };
  publishOpen.value = true;

  publishOpen.value = true;
  publishOptionsLoading.value = true;
  try {
    const { data: optionsData } = await menuPublishOptions(menu.subjectType);
    const obj = (optionsData || {}) as RowData;
    const typeOptions = Array.isArray(obj.typeOptions) ? obj.typeOptions : [];
    const templateOptions = Array.isArray(obj.templateOptions) ? obj.templateOptions : [];
    publishOptions.value = { typeOptions, templateOptions };
    publishForm.targetTypeCodes = typeOptions.map(item => String(item.typeCode));
    publishForm.targetTemplateIds = templateOptions.map(item => item.id);
  } finally {
    publishOptionsLoading.value = false;
  }
}

function closePublishDialog() {
  const shouldReturn = publishReturnToForm.value;
  publishOpen.value = false;
  publishReturnToForm.value = false;
  if (shouldReturn) {
    formOpen.value = true;
  }
}

function createMenuPayload(): SysMenuDTO {
  return {
    id: formModel.id,
    parentId: formModel.parentId ?? 0,
    menuType: formModel.menuType as 'M' | 'C' | 'F',
    subjectType: String(formModel.subjectType),
    menuName: String(formModel.menuName).trim(),
    path: String(formModel.path || '').trim(),
    component: String(formModel.component || '').trim(),
    perms: String(formModel.perms || '').trim(),
    icon: String(formModel.icon || '').trim(),
    orderNum: Number(formModel.orderNum ?? 0),
    isVisible: Number(formModel.isVisible ?? 1),
    status: Number(formModel.status ?? 1)
  };
}

function onPublishTypeCodesChange() {
  const selected = new Set(publishForm.targetTypeCodes);
  publishForm.targetTemplateIds = publishOptions.value.templateOptions
    .filter(item => selected.has(String(item.typeCode)))
    .map(item => item.id);
}

async function submitPublish() {
  if (!publishForm.menu) {
    window.$message?.warning('菜单信息为空');
    return;
  }
  if (!publishForm.targetTypeCodes.length) {
    window.$message?.warning('请选择至少一个目标公司类型');
    return;
  }
  const filteredTemplates = publishOptions.value.templateOptions.filter(item =>
    publishForm.targetTypeCodes.includes(String(item.typeCode))
  );
  if (filteredTemplates.length > 0 && !publishForm.targetTemplateIds.length) {
    window.$message?.warning('请选择至少一个目标角色模板');
    return;
  }

  publishLoading.value = true;
  try {
    const { response } = await publishMenu({
      menu: { ...publishForm.menu },
      targetTypeCodes: [...publishForm.targetTypeCodes],
      targetTemplateIds: [...publishForm.targetTemplateIds],
      syncExistingCompanies: publishForm.syncExistingCompanies
    });
    publishOpen.value = false;
    publishReturnToForm.value = false;
    window.$message?.success(getResponseMsg(response, '菜单发布成功'));
    await loadList();
  } finally {
    publishLoading.value = false;
  }
}

async function handleSaveAndPublish() {
  if (!String(formModel.menuName || '').trim()) {
    window.$message?.warning('请输入菜单名称');
    return;
  }
  if (!String(formModel.subjectType || '').trim()) {
    window.$message?.warning('请选择主体类型');
    return;
  }
  if (!String(formModel.menuType || '').trim()) {
    window.$message?.warning('请选择类型');
    return;
  }

  publishPrepLoading.value = true;
  try {
    const payload = createMenuPayload();
    formOpen.value = false;
    await openPublishDialog(payload as RowData, true);
  } finally {
    publishPrepLoading.value = false;
  }
}

async function submitForm() {
  if (!String(formModel.menuName || '').trim()) {
    window.$message?.warning('请输入菜单名称');
    return;
  }
  if (!String(formModel.subjectType || '').trim()) {
    window.$message?.warning('请选择主体类型');
    return;
  }
  if (!String(formModel.menuType || '').trim()) {
    window.$message?.warning('请选择类型');
    return;
  }

  formSubmitting.value = true;
  try {
    const payload = createMenuPayload();
    if (payload.id) {
      const { response } = await updateMenu(payload);
      window.$message?.success(getResponseMsg(response, '操作成功'));
    } else {
      const { response } = await addMenu(payload);
      window.$message?.success(getResponseMsg(response, '操作成功'));
    }

    formOpen.value = false;
    await loadList();
  } finally {
    formSubmitting.value = false;
  }
}

async function removeMenu(record: RowData) {
  const { response } = await deleteMenu(record.id);
  window.$message?.success(getResponseMsg(response, '删除成功'));
  await loadList();
}

function toggleExpand() {
  isExpandAll.value = !isExpandAll.value;
  expandedRowKeys.value = isExpandAll.value ? collectRowKeys(menuTreeRows.value) : [];
}

watch(subjectType, () => {
  loadList();
});

async function loadList() {
  loading.value = true;
  try {
    const { data } = await menuTree(subjectType.value);
    menuTreeRows.value = normalizeMenuTree(data);
    expandedRowKeys.value = isExpandAll.value ? collectRowKeys(menuTreeRows.value) : [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadList();
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <AForm :label-col="{ span: 5, md: 7 }">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="主体类型" class="m-0">
                  <ASelect v-model:value="subjectType" class="w-full" :options="subjectTypeOptions" />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
        </div>
      </AForm>
    </ACard>
    <ACard
      title="系统管理-菜单"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <ASpace>
          <AButton @click="toggleExpand">{{ isExpandAll ? '全部折叠' : '全部展开' }}</AButton>
          <AButton v-if="hasAuth('system:menu:add')" type="primary" @click="openAdd()">新增</AButton>
          <AButton v-if="hasAuth('system:menu:add')" @click="openCopyDialog">菜单拷贝</AButton>
        </ASpace>
      </template>
      <ATable
        ref="tableWrapperRef"
        v-model:expanded-row-keys="expandedRowKeys"
        :columns="columns"
        :data-source="menuTreeRows"
        :loading="loading"
        row-key="id"
        size="small"
        class="h-full"
        :pagination="false"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <span>{{ record.icon || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'menuType'">
            <ATag v-if="record.menuType === 'M'">目录</ATag>
            <ATag v-else-if="record.menuType === 'C'" color="success">菜单</ATag>
            <ATag v-else-if="record.menuType === 'F'" color="processing">按钮</ATag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'isVisible'">
            <ATag :color="tagColorPositiveNeutral(record.isVisible === 1)">
              {{ record.isVisible === 1 ? '是' : '否' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'status'">
            <ATag :color="tagColorEnabled(record.status === 1)">{{ record.status === 1 ? '启用' : '停用' }}</ATag>
          </template>
          <template v-else-if="column.key === 'perms'">
            <span>{{ record.perms || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'component'">
            <span>{{ record.component || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <ASpace :size="2" wrap>
              <AButton
                v-if="hasAuth('system:menu:update')"
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openEdit(record)"
              >
                编辑
              </AButton>
              <AButton
                v-if="hasAuth('system:menu:add')"
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openAdd(record)"
              >
                新增子级
              </AButton>
              <AButton
                v-if="hasAuth('system:menu:publish')"
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openPublish(record)"
              >
                发布
              </AButton>
              <APopconfirm
                v-if="hasAuth('system:menu:remove')"
                :title="`确认删除菜单“${record.menuName || '-'}”？`"
                @confirm="removeMenu(record)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer v-model:open="formOpen" :title="formTitle" :width="960">
      <AForm layout="vertical">
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="上级菜单">
              <ATreeSelect
                v-model:value="formModel.parentId"
                :tree-data="menuOptions"
                :field-names="{ value: 'id', label: 'menuName', children: 'children' }"
                tree-default-expand-all
                allow-clear
                placeholder="无上级（顶级菜单）"
                class="w-full"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="菜单类型" required>
              <ARadioGroup v-model:value="formModel.menuType">
                <ARadio value="M">目录</ARadio>
                <ARadio value="C">菜单</ARadio>
                <ARadio value="F">按钮</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="主体类型" required>
              <ASelect v-model:value="formModel.subjectType" :options="subjectTypeOptions" @change="loadMenuOptions" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="菜单名称" required>
              <AInput v-model:value="formModel.menuName" />
            </AFormItem>
          </ACol>
          <ACol v-if="formModel.menuType !== 'F'" :span="12">
            <AFormItem label="路由地址">
              <AInput v-model:value="formModel.path" placeholder="如 user、role" />
            </AFormItem>
          </ACol>
          <ACol v-if="formModel.menuType === 'C'" :span="12">
            <AFormItem label="组件路径">
              <AInput v-model:value="formModel.component" placeholder="如 system/user/index" />
            </AFormItem>
          </ACol>
          <ACol v-if="formModel.menuType !== 'M'" :span="12">
            <AFormItem label="权限标识">
              <AInput v-model:value="formModel.perms" placeholder="如 system:user:list" />
            </AFormItem>
          </ACol>
          <ACol v-if="formModel.menuType !== 'F'" :span="12">
            <AFormItem label="图标">
              <AInput v-model:value="formModel.icon" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="排序">
              <AInputNumber v-model:value="formModel.orderNum" :min="0" class="w-full" />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="是否可见">
              <ARadioGroup v-model:value="formModel.isVisible">
                <ARadio :value="1">是</ARadio>
                <ARadio :value="0">否</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="状态">
              <ARadioGroup v-model:value="formModel.status">
                <ARadio :value="1">启用</ARadio>
                <ARadio :value="0">停用</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
        </ARow>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="formOpen = false">取消</AButton>
          <AButton v-if="hasAuth('system:menu:publish')" :loading="publishPrepLoading" @click="handleSaveAndPublish">
            保存并发布
          </AButton>
          <AButton type="primary" :loading="formSubmitting" @click="submitForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="copyOpen" title="菜单拷贝" :width="360">
      <AForm layout="vertical">
        <AFormItem label="源主体类型" required>
          <ASelect
            v-model:value="copyForm.sourceSubjectType"
            :options="subjectTypeOptions"
            @change="loadCopySourceTree"
          />
        </AFormItem>
        <AFormItem label="目标主体类型" required>
          <ASelect v-model:value="copyForm.targetSubjectType" :options="subjectTypeOptions" />
        </AFormItem>
        <AFormItem label="选择菜单（不选则拷贝全部）">
          <ASpin :spinning="copyTreeLoading">
            <ATree
              v-model:checked-keys="copyCheckedKeys"
              checkable
              :tree-data="copySourceTree as any"
              :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
              class="max-h-320px overflow-auto"
            />
          </ASpin>
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="copyOpen = false">取消</AButton>
          <AButton type="primary" :loading="copySubmitting" @click="submitCopy">开始拷贝</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="publishOpen" :title="publishDialogTitle" :width="420">
      <ASpin :spinning="publishOptionsLoading">
        <AAlert
          type="info"
          show-icon
          message="发布只做追加：会补充公司类型菜单上限、模板菜单，并可把本次菜单追加到已有公司的系统角色，不会回收公司已有额外菜单。"
          class="publish-alert"
        />
        <AForm layout="vertical">
          <AFormItem label="菜单名称">
            <span>{{ publishForm.menu?.menuName || '-' }}</span>
          </AFormItem>
          <AFormItem label="主体类型">
            <span>
              {{ subjectTypeLabelMap[publishForm.menu?.subjectType as 'PLATFORM' | 'HQ' | 'SERVICE'] || '-' }}
            </span>
          </AFormItem>
          <AFormItem label="目标公司类型" required>
            <ACheckboxGroup v-model:value="publishForm.targetTypeCodes" @change="onPublishTypeCodesChange">
              <ASpace direction="vertical">
                <ACheckbox
                  v-for="item in publishOptions.typeOptions"
                  :key="item.typeCode"
                  :value="String(item.typeCode)"
                >
                  {{ item.typeName }}（{{ item.typeCode }}）
                </ACheckbox>
              </ASpace>
            </ACheckboxGroup>
          </AFormItem>
          <AFormItem label="目标角色模板" :required="groupedPublishTemplates.length > 0">
            <div v-if="groupedPublishTemplates.length === 0" class="publish-empty">
              当前所选主体下暂无角色模板，本次发布仅处理公司类型菜单上限。
            </div>
            <div v-else class="publish-template-list">
              <div v-for="group in groupedPublishTemplates" :key="group.typeCode" class="publish-template-group">
                <div class="publish-template-title">{{ group.typeName }}（{{ group.typeCode }}）</div>
                <ACheckboxGroup v-model:value="publishForm.targetTemplateIds">
                  <ASpace direction="vertical">
                    <ACheckbox v-for="item in group.templates" :key="item.id" :value="item.id">
                      {{ item.roleName }}（{{ item.roleKey }}）
                    </ACheckbox>
                  </ASpace>
                </ACheckboxGroup>
              </div>
            </div>
          </AFormItem>
          <AFormItem label="同步已有公司">
            <ASwitch v-model:checked="publishForm.syncExistingCompanies" />
            <span class="publish-tip">仅向匹配模板 `roleKey` 的系统角色追加当前菜单。</span>
          </AFormItem>
        </AForm>
      </ASpin>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="closePublishDialog">取消</AButton>
          <AButton type="primary" :loading="publishLoading" @click="submitPublish">确认发布</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>

<style scoped>
.publish-alert {
  margin-bottom: 16px;
}

.publish-template-list {
  max-height: 300px;
  overflow-y: auto;
}

.publish-template-group + .publish-template-group {
  margin-top: 12px;
}

.publish-template-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.publish-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.publish-empty {
  color: #909399;
  font-size: 12px;
}
</style>
