<script setup lang="ts">
/**
 * 通知场景配置：按场景维护多通知目标（站内/小程序等）的模板与渠道参数，支持预览渲染。 */
import { computed, onMounted, reactive, ref } from 'vue';
import {
  type NotifySceneConfigQuery,
  type NotifySceneConfigSaveDTO,
  type NotifyScenePreviewDTO,
  type NotifySceneTargetConfigDTO,
  getNotifyScene,
  getNotifySceneOptions,
  listNotifyScene,
  previewNotifyScene,
  updateNotifyScene
} from '@/service/api';
import { notifyOnceSuccessFromFlatResult } from '@/service/request/shared';
import { useAuth } from '@/hooks/business/auth';
import { useRouteMenuTitle } from '@/hooks/common/route-menu-title';
import { usePageSearchFilterCollapse } from '@/hooks/common/page-search-filter-collapse';
import { useTableScroll } from '@/hooks/common/table';
import { createAntTableActionColumn, withAntTableActionColumn } from '@/utils/table-action-width';
import { createAntTableListLocale, useListRequestTableMsgs } from '@/utils/list-table-empty-state';
import { applyDateTimeColumnRender } from '@/utils/datetime';
import PageSearchExpandButton from '@/components/custom/page-search-expand-button.vue';

type RowData = Record<string, any>;
type EnumOption = { code: string; desc: string };
type TargetMeta = RowData;
type DialogForm = RowData;

/**
 * 作用：构造数据或配置：buildDefaultQuery。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildDefaultQuery(): NotifySceneConfigQuery {
  return {
    pageNum: 1,
    pageSize: 10,
    sceneName: '',
    sceneCode: '',
    bizType: '',
    targetType: ''
  };
}

/**
 * 作用：构造数据或配置：buildFieldMappingRow。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildFieldMappingRow() {
  return { field: '', value: '' };
}

const pageMenuTitle = useRouteMenuTitle();
const { hasAuth } = useAuth();
const sceneSearchFilter = usePageSearchFilterCollapse(4);

const loading = ref(false);
const optionsLoading = ref(false);
const optionsLoaded = ref(false);
const rows = ref<RowData[]>([]);
const total = ref(0);
const queryParams = reactive<NotifySceneConfigQuery>(buildDefaultQuery());

const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch
} = useListRequestTableMsgs();
const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, rows);

/** 操作列仅保留「配置」，查看入口改由场景名称点击进入 */
const NOTIFY_SCENE_ACTION_WIDTH = 72;

/** 当前角色是否具备通知场景行内「配置」权限 */
const showNotifySceneTableActionColumn = computed(() => hasAuth('system:notifyScene:update'));

/** Tab 内表单项统一标签宽度（与标题模板等并排项一致），控件占满行内剩余宽度 */
const TARGET_CARD_FORM_LABEL_COL = { style: { width: '108px', flex: '0 0 108px' } };
const TARGET_CARD_FORM_WRAPPER_COL = { style: { flex: '1 1 0', minWidth: 0 } };
const notifySceneTableScrollX = computed(
  () => 1100 + (showNotifySceneTableActionColumn.value ? NOTIFY_SCENE_ACTION_WIDTH : 0)
);
const { tableWrapperRef, scrollConfig } = useTableScroll(notifySceneTableScrollX);

const options = reactive({
  sceneOptions: [] as RowData[],
  targetTypeOptions: [] as EnumOption[],
  channelSceneOptions: [] as EnumOption[],
  routeTypeOptions: [] as EnumOption[]
});

const dialogOpen = ref(false);
const dialogMode = ref<'view' | 'edit'>('view');
const dialogForm = ref<DialogForm | null>(null);
/** 配置弹窗内当前选中的通知目标 Tab（targetType） */
const activeTargetType = ref('');
/** 打开弹窗时各目标 enabled 快照，用于 Tab 区分「未启用」与「停用」 */
const initialTargetEnabledSnapshot = ref<Record<string, number>>({});
const submitLoading = ref(false);

const previewOpen = ref(false);
const previewLoading = ref(false);
const previewForm = reactive({
  sceneCode: '',
  sceneName: '',
  targetType: '',
  targetTypeDesc: '',
  variablesText: '{}',
  titleTemplate: '',
  contentTemplate: '',
  routeType: '',
  routeValueTemplate: '',
  templateId: '',
  channelScene: '',
  pagePathTemplate: '',
  fieldMapping: [] as Array<{ field: string; value: string }>,
  result: null as RowData | null
});

const dialogReadonly = computed(() => dialogMode.value !== 'edit');
const sceneOptions = computed(() => options.sceneOptions || []);
const targetTypeOptions = computed(() => options.targetTypeOptions || []);
const channelSceneOptions = computed(() => options.channelSceneOptions || []);
const routeTypeOptions = computed(() => options.routeTypeOptions || []);

const bizTypeOptions = computed(() => {
  const items = sceneOptions.value.map(item => item.bizType).filter(Boolean) as string[];
  return Array.from(new Set(items)).map(v => ({ label: v, value: v }));
});

/**
 * 作用：读取/解析：getSceneMeta。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getSceneMeta(sceneCode: string) {
  return sceneOptions.value.find(item => item.sceneCode === sceneCode) || null;
}

/**
 * 作用：当前场景在注册表中支持的通知目标元数据（与 jasic-ui `currentSceneTargetMetas` 一致）。
 */
const currentSceneTargetMetas = computed<TargetMeta[]>(() => {
  if (!dialogForm.value?.sceneCode) return [];
  const sceneMeta = getSceneMeta(dialogForm.value.sceneCode);
  return sceneMeta && Array.isArray(sceneMeta.targetMetas) ? sceneMeta.targetMetas : [];
});

/**
 * 作用：实际渲染的 Tab 列表。查看态仅展示已启用目标（对齐 jasic-ui 勾选后可见的配置区）；编辑态展示场景支持的全部目标。
 */
const visibleTargetTabMetas = computed<TargetMeta[]>(() => {
  const metas = currentSceneTargetMetas.value;
  if (!dialogForm.value || !metas.length) return [];
  if (dialogMode.value === 'edit') return metas;
  return metas.filter(meta => {
    const cfg = (dialogForm.value!.targetConfigs as RowData[]).find(item => item.targetType === meta.targetType);
    return Number(cfg?.enabled) === 1;
  });
});

const columns = computed(() =>
  withAntTableActionColumn<any>(
    applyDateTimeColumnRender([
      { title: '场景名称', dataIndex: 'sceneName', key: 'sceneName', width: 220 },
      {
        title: '场景编码',
        dataIndex: 'sceneCode',
        key: 'sceneCode',
        width: 220,
        ellipsis: true
      },
      {
        title: '业务类型',
        dataIndex: 'bizType',
        key: 'bizType',
        width: 140,
        ellipsis: true
      },
      {
        title: '已启用目标',
        dataIndex: 'enabledTargetTypeDescs',
        key: 'enabledTargets',
        width: 220
      },
      { title: '场景状态', dataIndex: 'status', key: 'status', width: 100 },
      { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 }
    ]),
    showNotifySceneTableActionColumn.value,
    createAntTableActionColumn({
      width: NOTIFY_SCENE_ACTION_WIDTH,
      fixed: 'right'
    })
  )
);

/** 可用变量转置表：行维度（原表格列）定义 */
const VARIABLE_TRANSPOSED_ROW_DEFS = [
  { rowKey: 'name', rowLabel: '变量名', field: 'name' },
  { rowKey: 'desc', rowLabel: '说明', field: 'desc' },
  { rowKey: 'example', rowLabel: '示例值', field: 'example' }
] as const;

/**
 * 作用：将可用变量由「一行一变量」转为「一列一变量」；不展示表头行，变量名由「变量名」数据行体现。
 */
const variableTransposedColumns = computed(() => {
  const variables = (dialogForm.value?.variables || []) as RowData[];
  const cols: Array<{
    title: string;
    dataIndex: string;
    key: string;
    width?: number;
    ellipsis?: boolean;
    fixed?: 'left';
  }> = [
    {
      title: '',
      dataIndex: 'rowLabel',
      key: 'rowLabel',
      width: 96,
      fixed: 'left'
    }
  ];
  variables.forEach((_item, index) => {
    cols.push({
      title: '',
      dataIndex: `var_${index}`,
      key: `var_${index}`,
      ellipsis: true
    });
  });
  return cols;
});

const variableTransposedRows = computed(() => {
  const variables = (dialogForm.value?.variables || []) as RowData[];
  return VARIABLE_TRANSPOSED_ROW_DEFS.map(rowDef => {
    const row: RowData = { rowKey: rowDef.rowKey, rowLabel: rowDef.rowLabel };
    variables.forEach((item, index) => {
      const value = item?.[rowDef.field];
      row[`var_${index}`] = value === null || value === undefined || value === '' ? '-' : String(value);
    });
    return row;
  });
});

const fieldMappingColumns = [
  { title: '模板字段', key: 'field', width: 180 },
  { title: '变量表达式', key: 'value', width: 240 },
  createAntTableActionColumn({ width: 90 })
];

const previewMappingColumns = [
  { title: '模板字段', dataIndex: 'field', key: 'field', width: 160 },
  {
    title: '值模板',
    dataIndex: 'valueTemplate',
    key: 'valueTemplate',
    width: 220
  },
  { title: '渲染结果', dataIndex: 'value', key: 'value', width: 220 }
];

/**
 * 作用：从分页接口响应解析列表数组。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickRows(data: unknown) {
  if (Array.isArray(data)) return data;
  if (Array.isArray((data as { records?: unknown })?.records)) return (data as { records: RowData[] }).records;
  return [];
}

/**
 * 作用：页面内业务方法：trimValue。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function trimValue(value: unknown) {
  if (value === null || value === undefined) return null;
  const text = String(value).trim();
  return text || null;
}

/**
 * 作用：判断是否满足条件：isMiniProgramTarget。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isMiniProgramTarget(targetMetaOrType?: TargetMeta | string) {
  if (!targetMetaOrType) return false;
  if (typeof targetMetaOrType === 'string') return targetMetaOrType.startsWith('MP_SUBSCRIBE_');
  if (targetMetaOrType.channelType === 'MP_SUBSCRIBE') return true;
  return isMiniProgramTarget(String(targetMetaOrType.targetType || ''));
}

/**
 * 作用：构造数据或配置：buildReceiverDesc。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildReceiverDesc(targetMeta: TargetMeta) {
  const parts = [targetMeta.receiverTypeDesc, targetMeta.receiverDesc].filter(Boolean);
  return parts.length ? parts.join(' / ') : '-';
}

/**
 * 作用：格式化展示：formatRouteType。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function formatRouteType(routeType?: string) {
  if (!routeType) return '-';
  const matched = routeTypeOptions.value.find(item => item.code === routeType);
  return matched ? matched.desc : routeType;
}

/**
 * 作用：加载数据：loadOptions。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadOptions() {
  optionsLoading.value = true;
  try {
    const flat = await getNotifySceneOptions();
    if (consumeFlatError(flat)) return;
    const data = ((flat as { data?: RowData }).data || {}) as RowData;
    options.sceneOptions = Array.isArray(data.sceneOptions) ? data.sceneOptions : [];
    options.targetTypeOptions = Array.isArray(data.targetTypeOptions) ? data.targetTypeOptions : [];
    options.channelSceneOptions = Array.isArray(data.channelSceneOptions) ? data.channelSceneOptions : [];
    options.routeTypeOptions = Array.isArray(data.routeTypeOptions) ? data.routeTypeOptions : [];
    optionsLoaded.value = true;
  } finally {
    optionsLoading.value = false;
  }
}

/**
 * 作用：确保前置数据已加载：ensureOptionsLoaded。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function ensureOptionsLoaded() {
  if (optionsLoaded.value) return;
  await loadOptions();
}

/**
 * 作用：构造数据或配置：buildQueryParams。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildQueryParams(): NotifySceneConfigQuery {
  const params: NotifySceneConfigQuery = { ...queryParams };
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
  ) as NotifySceneConfigQuery;
}

/**
 * 作用：加载当前 Tab/条件下表格数据。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadList() {
  clearListMsgs();
  loading.value = true;
  try {
    const flat = await listNotifyScene(buildQueryParams());
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

/**
 * 作用：处理交互事件：handleQuery。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleQuery() {
  queryParams.pageNum = 1;
  loadList();
}

/**
 * 作用：重置查询条件并刷新列表：resetQuery。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetQuery() {
  Object.assign(queryParams, buildDefaultQuery());
  loadList();
}

/**
 * 作用：组件回调：onPageChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
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
 * 作用：页面内业务方法：normalizeTargetConfigItem。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizeTargetConfigItem(item?: RowData | null): RowData {
  return {
    targetType: '',
    targetTypeDesc: '',
    receiverType: '',
    receiverTypeDesc: '',
    receiverDesc: '',
    channelType: '',
    channelTypeDesc: '',
    enabled: 0,
    titleTemplate: '',
    contentTemplate: '',
    routeType: '',
    routeValueTemplate: '',
    templateId: '',
    channelScene: '',
    channelSceneDesc: '',
    pagePathTemplate: '',
    fieldMapping: [buildFieldMappingRow()],
    remark: '',
    ...(item || {}),
    fieldMapping:
      Array.isArray(item?.fieldMapping) && item.fieldMapping.length
        ? item.fieldMapping.map((m: RowData) => ({
            field: m.field || '',
            value: m.value || ''
          }))
        : [buildFieldMappingRow()]
  };
}

/**
 * 作用：页面内业务方法：normalizeDetailForm。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function normalizeDetailForm(data: RowData): DialogForm {
  const form: DialogForm = {
    sceneCode: data.sceneCode || '',
    sceneName: data.sceneName || '',
    bizType: data.bizType || '',
    eventCode: data.eventCode || '',
    status: typeof data.status === 'number' ? data.status : 1,
    remark: data.remark || '',
    variables: Array.isArray(data.variables) ? data.variables : [],
    targetConfigs: []
  };
  form.targetConfigs = (Array.isArray(data.targetConfigs) ? data.targetConfigs : []).map((item: RowData) =>
    normalizeTargetConfigItem(item)
  );
  return form;
}

/**
 * 作用：页面内业务方法：mergeDialogTargetConfigs。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function mergeDialogTargetConfigs() {
  if (!dialogForm.value) return;
  const apiMap = new Map<string, RowData>();
  for (const item of (dialogForm.value.targetConfigs as RowData[]) || []) {
    const type = trimValue(item?.targetType);
    if (type) apiMap.set(String(type), item);
  }
  dialogForm.value.targetConfigs = currentSceneTargetMetas.value.map(meta =>
    normalizeTargetConfigItem({ ...buildTargetFormByMeta(meta), ...(apiMap.get(meta.targetType) || {}) })
  );
}

/**
 * 作用：读取/解析：getTargetForm。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getTargetForm(targetType: string): RowData {
  if (!dialogForm.value) return {};
  let targetForm = dialogForm.value.targetConfigs.find((item: RowData) => item.targetType === targetType);
  if (!targetForm) {
    const targetMeta = currentSceneTargetMetas.value.find(item => item.targetType === targetType);
    targetForm = buildTargetFormByMeta(targetMeta);
    dialogForm.value.targetConfigs.push(targetForm);
  }
  return targetForm;
}

/** 从目标元数据读取字符串字段，空值统一为 '' */
/**
 * 作用：页面内业务方法：metaText。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function metaText(value: unknown): string {
  if (value === null || value === undefined) return '';
  return String(value);
}

/** 从目标元数据构建字段映射行，无配置时返回一行空模板 */
/**
 * 作用：构造数据或配置：buildFieldMappingFromMeta。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildFieldMappingFromMeta(targetMeta?: TargetMeta) {
  const mapping = targetMeta?.fieldMapping;
  if (!Array.isArray(mapping) || !mapping.length) {
    return [buildFieldMappingRow()];
  }
  return mapping.map((item: RowData) => ({
    field: item.field || '',
    value: item.value || ''
  }));
}

/**
 * 作用：构造数据或配置：buildTargetFormByMeta。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildTargetFormByMeta(targetMeta?: TargetMeta) {
  const enabled = typeof targetMeta?.defaultEnabled === 'number' ? targetMeta.defaultEnabled : 0;
  return {
    targetType: metaText(targetMeta?.targetType),
    targetTypeDesc: metaText(targetMeta?.targetTypeDesc),
    receiverType: metaText(targetMeta?.receiverType),
    receiverTypeDesc: metaText(targetMeta?.receiverTypeDesc),
    receiverDesc: metaText(targetMeta?.receiverDesc),
    channelType: metaText(targetMeta?.channelType),
    channelTypeDesc: metaText(targetMeta?.channelTypeDesc),
    enabled,
    titleTemplate: metaText(targetMeta?.defaultTitleTemplate),
    contentTemplate: metaText(targetMeta?.defaultContentTemplate),
    routeType: metaText(targetMeta?.defaultRouteType),
    routeValueTemplate: metaText(targetMeta?.defaultRouteValueTemplate),
    templateId: metaText(targetMeta?.templateId),
    channelScene: metaText(targetMeta?.channelScene),
    channelSceneDesc: metaText(targetMeta?.channelSceneDesc),
    pagePathTemplate: metaText(targetMeta?.pagePathTemplate),
    fieldMapping: buildFieldMappingFromMeta(targetMeta),
    remark: ''
  };
}

/**
 * 作用：应用配置或路由参数：applyDefaultValuesIfBlank。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applyDefaultValuesIfBlank(targetMeta: TargetMeta, targetForm: RowData) {
  if (!targetForm.titleTemplate) targetForm.titleTemplate = targetMeta.defaultTitleTemplate || '';
  if (!targetForm.contentTemplate) targetForm.contentTemplate = targetMeta.defaultContentTemplate || '';
  if (!targetForm.routeType) targetForm.routeType = targetMeta.defaultRouteType || '';
  if (!targetForm.routeValueTemplate) targetForm.routeValueTemplate = targetMeta.defaultRouteValueTemplate || '';
  if (isMiniProgramTarget(targetMeta)) {
    if (!targetForm.templateId) targetForm.templateId = targetMeta.templateId || '';
    if (!targetForm.channelScene) targetForm.channelScene = targetMeta.channelScene || '';
    if (!targetForm.pagePathTemplate) targetForm.pagePathTemplate = targetMeta.pagePathTemplate || '';
    if ((!targetForm.fieldMapping || !targetForm.fieldMapping.length) && Array.isArray(targetMeta.fieldMapping)) {
      targetForm.fieldMapping = targetMeta.fieldMapping.map((item: RowData) => ({
        field: item.field || '',
        value: item.value || ''
      }));
    }
  }
}

/**
 * 作用：切换通知目标 Tab 时，确保目标表单已初始化并补齐默认模板。 */
/**
 * 作用：处理交互事件：handleTargetTabChange。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleTargetTabChange(targetType: string | number) {
  activeTargetType.value = String(targetType);
  const targetMeta = currentSceneTargetMetas.value.find(item => item.targetType === activeTargetType.value);
  if (!targetMeta) return;
  const targetForm = getTargetForm(targetMeta.targetType);
  if (targetForm.enabled !== 1 && targetForm.enabled !== 0) targetForm.enabled = 0;
  applyDefaultValuesIfBlank(targetMeta, targetForm);
}

/**
 * 作用：记录打开弹窗时各通知目标的启用状态，供 Tab 标签展示「未启用 / 停用」区分。 */
/**
 * 作用：页面内业务方法：snapshotInitialTargetEnabled。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function snapshotInitialTargetEnabled() {
  const snapshot: Record<string, number> = {};
  currentSceneTargetMetas.value.forEach(targetMeta => {
    const targetForm = getTargetForm(targetMeta.targetType);
    snapshot[targetMeta.targetType] = Number(targetForm.enabled === 1 ? 1 : 0);
  });
  initialTargetEnabledSnapshot.value = snapshot;
}

/**
 * 作用：读取/解析：getTargetTabTag。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getTargetTabTag(targetMeta: TargetMeta) {
  const targetForm = getTargetForm(targetMeta.targetType);
  if (targetForm.enabled === 1) {
    return { label: '已启用', color: 'success' as const };
  }
  const initial = initialTargetEnabledSnapshot.value[targetMeta.targetType] ?? 0;
  if (initial === 1) {
    return { label: '停用', color: 'default' as const };
  }
  return { label: '未启用', color: 'default' as const };
}

/**
 * 作用：页面内业务方法：resolveInitialActiveTargetType。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveInitialActiveTargetType(targetConfigs: RowData[]) {
  const ordered = targetConfigs.filter(item => trimValue(item?.targetType));
  const firstEnabled = ordered.find(item => Number(item.enabled) === 1);
  if (firstEnabled?.targetType) return String(firstEnabled.targetType);
  return ordered[0]?.targetType ? String(ordered[0].targetType) : '';
}

/**
 * 作用：打开配置弹窗时初始化各目标表单，并按详情回显定位默认 Tab。 */
/**
 * 作用：初始化：initDialogTargetTabs。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function initDialogTargetTabs() {
  const targetConfigs = (dialogForm.value?.targetConfigs as RowData[]) || [];
  const tabs = visibleTargetTabMetas.value;
  currentSceneTargetMetas.value.forEach(targetMeta => {
    const targetForm = getTargetForm(targetMeta.targetType);
    if (targetForm.enabled === 1) applyDefaultValuesIfBlank(targetMeta, targetForm);
  });
  snapshotInitialTargetEnabled();
  const resolved = resolveInitialActiveTargetType(targetConfigs);
  activeTargetType.value =
    resolved && tabs.some(item => item.targetType === resolved) ? resolved : tabs[0]?.targetType || '';
}

/**
 * 作用：页面内业务方法：addFieldMapping。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function addFieldMapping(targetType: string) {
  getTargetForm(targetType).fieldMapping.push(buildFieldMappingRow());
}

/**
 * 作用：删除记录：removeFieldMapping。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function removeFieldMapping(targetType: string, index: number) {
  const fieldMapping = getTargetForm(targetType).fieldMapping as Array<{
    field: string;
    value: string;
  }>;
  fieldMapping.splice(index, 1);
  if (!fieldMapping.length) fieldMapping.push(buildFieldMappingRow());
}

/**
 * 作用：页面内业务方法：openDetailDialog。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function openDetailDialog(sceneCode: string, mode: 'view' | 'edit') {
  await ensureOptionsLoaded();
  const flat = await getNotifyScene(sceneCode);
  if (consumeFlatError(flat)) return;
  dialogMode.value = mode;
  dialogForm.value = normalizeDetailForm(((flat as { data?: RowData }).data || {}) as RowData);
  mergeDialogTargetConfigs();
  initDialogTargetTabs();
  dialogOpen.value = true;
}

/**
 * 作用：列表行点击场景名称时打开查看弹窗（样式与通知记录页业务编号入口一致）。 */
/**
 * 作用：页面内业务方法：openSceneViewFromRow。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openSceneViewFromRow(row: RowData) {
  if (!hasAuth('system:notifyScene:view') || !row.sceneCode) return;
  openDetailDialog(String(row.sceneCode), 'view');
}

/** 场景名称是否展示为可点击的查看入口 */
/**
 * 作用：页面内业务方法：canOpenSceneViewFromName。
 * @returns boolean
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function canOpenSceneViewFromName(row: RowData) {
  return hasAuth('system:notifyScene:view') && Boolean(row.sceneCode);
}

/**
 * 作用：构造数据或配置：buildSubmitPayload。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildSubmitPayload(): NotifySceneConfigSaveDTO | null {
  if (!dialogForm.value) return null;
  const targetConfigs: NotifySceneTargetConfigDTO[] = [];
  for (const targetMeta of currentSceneTargetMetas.value) {
    const targetForm = getTargetForm(targetMeta.targetType);
    const enabled = Number(targetForm.enabled === 1 ? 1 : 0);
    if (isMiniProgramTarget(targetMeta) && enabled === 1 && !trimValue(targetForm.channelScene)) {
      window.$message?.error(`${targetMeta.targetTypeDesc}启用时必须选择发送小程序`);
      return null;
    }
    const fieldMapping = Array.isArray(targetForm.fieldMapping)
      ? targetForm.fieldMapping
          .map((item: RowData) => ({
            field: trimValue(item.field) || '',
            value: trimValue(item.value) || ''
          }))
          .filter((item: { field: string; value: string }) => item.field || item.value)
      : [];
    targetConfigs.push({
      targetType: targetMeta.targetType,
      enabled,
      titleTemplate: trimValue(targetForm.titleTemplate) || undefined,
      contentTemplate: trimValue(targetForm.contentTemplate) || undefined,
      routeType: trimValue(targetForm.routeType) || undefined,
      routeValueTemplate: trimValue(targetForm.routeValueTemplate) || undefined,
      templateId: trimValue(targetForm.templateId) || undefined,
      channelScene: trimValue(targetForm.channelScene) || undefined,
      pagePathTemplate: trimValue(targetForm.pagePathTemplate) || undefined,
      fieldMapping,
      remark: trimValue(targetForm.remark) || undefined
    });
  }
  return {
    status: dialogForm.value.status,
    remark: trimValue(dialogForm.value.remark) || undefined,
    targetConfigs
  };
}

/**
 * 作用：校验并提交：submitSceneConfig。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitSceneConfig() {
  const payload = buildSubmitPayload();
  if (!payload || !dialogForm.value?.sceneCode) return;
  submitLoading.value = true;
  try {
    const flat = await updateNotifyScene(dialogForm.value.sceneCode, payload);
    if (!notifyOnceSuccessFromFlatResult(flat, '保存成功')) return;
    dialogOpen.value = false;
    await loadList();
  } finally {
    submitLoading.value = false;
  }
}

/**
 * 作用：构造数据或配置：buildPreviewVariablesText。
 * @returns 对应类型或 void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildPreviewVariablesText() {
  const variables: Record<string, string> = {};
  (dialogForm.value?.variables || []).forEach((item: RowData) => {
    if (item?.name) variables[item.name] = item.example || '';
  });
  return JSON.stringify(variables, null, 2);
}

/**
 * 作用：打开预览。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openPreviewDialog(targetMeta: TargetMeta) {
  const targetForm = getTargetForm(targetMeta.targetType);
  previewForm.sceneCode = dialogForm.value?.sceneCode || '';
  previewForm.sceneName = dialogForm.value?.sceneName || '';
  previewForm.targetType = targetMeta.targetType;
  previewForm.targetTypeDesc = targetMeta.targetTypeDesc;
  previewForm.variablesText = buildPreviewVariablesText();
  previewForm.titleTemplate = targetForm.titleTemplate || '';
  previewForm.contentTemplate = targetForm.contentTemplate || '';
  previewForm.routeType = targetForm.routeType || '';
  previewForm.routeValueTemplate = targetForm.routeValueTemplate || '';
  previewForm.templateId = targetForm.templateId || '';
  previewForm.channelScene = targetForm.channelScene || '';
  previewForm.pagePathTemplate = targetForm.pagePathTemplate || '';
  previewForm.fieldMapping = (targetForm.fieldMapping || []).map((item: RowData) => ({
    field: item.field || '',
    value: item.value || ''
  }));
  previewForm.result = null;
  previewOpen.value = true;
}

/**
 * 作用：加载数据：loadPreview。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadPreview() {
  let variables: Record<string, string> = {};
  try {
    variables = previewForm.variablesText ? JSON.parse(previewForm.variablesText) : {};
  } catch {
    window.$message?.error('示例变量 JSON 格式不正确');
    return;
  }
  previewLoading.value = true;
  try {
    const dto: NotifyScenePreviewDTO = {
      sceneCode: previewForm.sceneCode,
      targetType: previewForm.targetType,
      titleTemplate: trimValue(previewForm.titleTemplate) || undefined,
      contentTemplate: trimValue(previewForm.contentTemplate) || undefined,
      routeType: trimValue(previewForm.routeType) || undefined,
      routeValueTemplate: trimValue(previewForm.routeValueTemplate) || undefined,
      templateId: trimValue(previewForm.templateId) || undefined,
      channelScene: trimValue(previewForm.channelScene) || undefined,
      pagePathTemplate: trimValue(previewForm.pagePathTemplate) || undefined,
      fieldMapping: previewForm.fieldMapping
        .map(item => ({
          field: trimValue(item.field) || undefined,
          value: trimValue(item.value) || undefined
        }))
        .filter(item => item.field || item.value),
      variables
    };
    const flat = await previewNotifyScene(dto);
    if (consumeFlatError(flat)) return;
    previewForm.result = ((flat as { data?: RowData }).data || null) as RowData | null;
  } finally {
    previewLoading.value = false;
  }
}

onMounted(async () => {
  await loadOptions();
  await loadList();
});
</script>

<template>
  <!-- 通知场景：场景定义、渠道/模板绑定与试发（多抽屉分步配置） -->
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <!-- 筛选区：场景名称、编码、业务类型、状态等 -->
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
                  'page-search-toolbar__filter-col--collapsed': sceneSearchFilter.isSearchFilterHidden(0)
                }"
              >
                <AFormItem label="场景名称" class="m-0">
                  <AInput v-model:value="queryParams.sceneName" allow-clear placeholder="请输入场景名称" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': sceneSearchFilter.isSearchFilterHidden(1)
                }"
              >
                <AFormItem label="场景编码" class="m-0">
                  <AInput v-model:value="queryParams.sceneCode" allow-clear placeholder="请输入场景编码" />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': sceneSearchFilter.isSearchFilterHidden(2)
                }"
              >
                <AFormItem label="业务类型" class="m-0">
                  <ASelect
                    v-model:value="queryParams.bizType"
                    allow-clear
                    show-search
                    placeholder="全部"
                    class="w-full"
                    :options="bizTypeOptions"
                  />
                </AFormItem>
              </ACol>
              <ACol
                :span="24"
                :md="12"
                :lg="6"
                :class="{
                  'page-search-toolbar__filter-col--collapsed': sceneSearchFilter.isSearchFilterHidden(3)
                }"
              >
                <AFormItem label="通知目标" class="m-0">
                  <ASelect
                    v-model:value="queryParams.targetType"
                    allow-clear
                    show-search
                    placeholder="全部"
                    class="w-full"
                    :options="
                      targetTypeOptions.map(i => ({
                        label: i.desc,
                        value: i.code
                      }))
                    "
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleQuery">搜索</AButton>
            <AButton @click="resetQuery">重置</AButton>
            <PageSearchExpandButton
              v-if="sceneSearchFilter.showSearchFilterExpandToggle"
              :expanded="sceneSearchFilter.searchFilterExpanded"
              @click="sceneSearchFilter.toggleSearchFilterExpand"
            />
          </div>
        </div>
      </AForm>
    </ACard>

    <!-- 列表区：场景表格，场景名可进入查看/配置抽屉 -->
    <ACard
      :title="pageMenuTitle"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading || optionsLoading"
        :locale="tableListLocale"
        :scroll="scrollConfig"
        row-key="sceneCode"
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
          <template v-if="column.key === 'sceneName'">
            <span
              v-if="canOpenSceneViewFromName(record)"
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openSceneViewFromRow(record)"
              @keydown.enter.prevent="openSceneViewFromRow(record)"
            >
              {{ record.sceneName || '-' }}
            </span>
            <span v-else>{{ record.sceneName || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'enabledTargets'">
            <template v-if="record.enabledTargetTypeDescs?.length">
              <ATag v-for="item in record.enabledTargetTypeDescs" :key="item" color="success" class="mb-4px mr-6px">
                {{ item }}
              </ATag>
            </template>
            <span v-else class="muted-text">未启用</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <ATag :color="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <AButton
              v-if="hasAuth('system:notifyScene:update')"
              type="link"
              size="small"
              @click="openDetailDialog(record.sceneCode, 'edit')"
            >
              配置
            </AButton>
          </template>
        </template>
      </ATable>
    </ACard>

    <!-- 抽屉：场景查看/编辑（渠道、模板、试发等分步配置） -->
    <ADrawer
      v-model:open="dialogOpen"
      :title="dialogMode === 'view' ? '查看通知场景配置' : '编辑通知场景配置'"
      placement="right"
      :width="1100"
      :body-style="{ paddingTop: 0 }"
      destroy-on-close
    >
      <AForm
        v-if="dialogForm"
        class="notify-scene-drawer-form"
        :model="dialogForm"
        label-align="left"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <div class="section-title">基础信息</div>
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="场景名称">
              <AInput :value="dialogForm.sceneName" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="场景编码">
              <AInput :value="dialogForm.sceneCode" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="业务类型">
              <AInput :value="dialogForm.bizType" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="事件编码">
              <AInput :value="dialogForm.eventCode" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="场景状态">
              <ARadioGroup v-model:value="dialogForm.status" :disabled="dialogReadonly">
                <ARadio :value="1">启用</ARadio>
                <ARadio :value="0">停用</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="场景备注">
              <ATextarea
                v-model:value="dialogForm.remark"
                :disabled="dialogReadonly"
                :rows="2"
                placeholder="请输入场景备注"
              />
            </AFormItem>
          </ACol>
        </ARow>

        <div class="notify-scene-target-section">
          <div class="section-title notify-scene-target-section__title">通知目标配置</div>
          <ATabs
            v-if="visibleTargetTabMetas.length"
            v-model:active-key="activeTargetType"
            class="notify-scene-target-tabs notify-scene-target-section__tabs"
            @change="handleTargetTabChange"
          >
            <ATabPane v-for="targetMeta in visibleTargetTabMetas" :key="targetMeta.targetType">
              <template #tab>
                <span>{{ targetMeta.targetTypeDesc }}</span>
                <ATag :color="getTargetTabTag(targetMeta).color" class="notify-scene-target-tabs__tag">
                  {{ getTargetTabTag(targetMeta).label }}
                </ATag>
              </template>
              <div class="target-card target-card-form">
                <div class="target-card__header">
                  <div>
                    <div class="target-card__sub">
                      {{ buildReceiverDesc(targetMeta) }}
                    </div>
                  </div>
                  <div class="target-card__actions">
                    <ASwitch
                      v-model:checked="getTargetForm(targetMeta.targetType).enabled"
                      :disabled="dialogReadonly"
                      :checked-value="1"
                      :un-checked-value="0"
                      checked-children="启用"
                      un-checked-children="停用"
                    />
                    <AButton
                      v-if="hasAuth('system:notifyScene:preview')"
                      type="primary"
                      ghost
                      size="small"
                      @click="openPreviewDialog(targetMeta)"
                    >
                      预览
                    </AButton>
                  </div>
                </div>

                <ARow :gutter="16" class="target-card-form-row">
                  <ACol :span="12">
                    <AFormItem
                      label="标题模板"
                      :label-col="TARGET_CARD_FORM_LABEL_COL"
                      :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                    >
                      <AInput
                        v-model:value="getTargetForm(targetMeta.targetType).titleTemplate"
                        :disabled="dialogReadonly"
                        placeholder="请输入标题模板"
                        class="w-full"
                      />
                    </AFormItem>
                  </ACol>
                  <ACol :span="12">
                    <AFormItem
                      label="跳转类型"
                      :label-col="TARGET_CARD_FORM_LABEL_COL"
                      :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                    >
                      <ASelect
                        v-model:value="getTargetForm(targetMeta.targetType).routeType"
                        :disabled="dialogReadonly"
                        allow-clear
                        placeholder="请选择跳转类型"
                        class="w-full"
                        :options="
                          routeTypeOptions.map(i => ({
                            label: i.desc,
                            value: i.code
                          }))
                        "
                      />
                    </AFormItem>
                  </ACol>
                  <ACol :span="24">
                    <AFormItem
                      label="内容模板"
                      :label-col="TARGET_CARD_FORM_LABEL_COL"
                      :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                    >
                      <ATextarea
                        v-model:value="getTargetForm(targetMeta.targetType).contentTemplate"
                        :disabled="dialogReadonly"
                        :rows="3"
                        placeholder="请输入内容模板"
                        class="w-full"
                      />
                    </AFormItem>
                  </ACol>
                  <ACol v-if="!isMiniProgramTarget(targetMeta)" :span="24">
                    <AFormItem
                      label="跳转值模板"
                      :label-col="TARGET_CARD_FORM_LABEL_COL"
                      :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                    >
                      <AInput
                        v-model:value="getTargetForm(targetMeta.targetType).routeValueTemplate"
                        :disabled="dialogReadonly"
                        placeholder="例如 ${workOrderId}"
                        class="w-full"
                      />
                    </AFormItem>
                  </ACol>
                </ARow>

                <template v-if="isMiniProgramTarget(targetMeta)">
                  <ARow :gutter="16" class="target-card-form-row">
                    <ACol :span="12">
                      <AFormItem
                        label="跳转值模板"
                        :label-col="TARGET_CARD_FORM_LABEL_COL"
                        :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                      >
                        <AInput
                          v-model:value="getTargetForm(targetMeta.targetType).routeValueTemplate"
                          :disabled="dialogReadonly"
                          placeholder="例如 ${workOrderId}"
                          class="w-full"
                        />
                      </AFormItem>
                    </ACol>
                    <ACol :span="12">
                      <AFormItem
                        label="模板ID"
                        :label-col="TARGET_CARD_FORM_LABEL_COL"
                        :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                      >
                        <AInput
                          v-model:value="getTargetForm(targetMeta.targetType).templateId"
                          :disabled="dialogReadonly"
                          placeholder="请输入小程序订阅消息模板ID"
                          class="w-full"
                        />
                      </AFormItem>
                    </ACol>
                    <ACol :span="12">
                      <AFormItem
                        label="页面路径模板"
                        :label-col="TARGET_CARD_FORM_LABEL_COL"
                        :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                      >
                        <AInput
                          v-model:value="getTargetForm(targetMeta.targetType).pagePathTemplate"
                          :disabled="dialogReadonly"
                          placeholder="例如 pages/order/detail?workOrderId=${workOrderId}"
                          class="w-full"
                        />
                      </AFormItem>
                    </ACol>
                    <ACol :span="12">
                      <AFormItem
                        label="发送小程序"
                        :label-col="TARGET_CARD_FORM_LABEL_COL"
                        :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                      >
                        <ASelect
                          v-model:value="getTargetForm(targetMeta.targetType).channelScene"
                          :disabled="dialogReadonly"
                          allow-clear
                          placeholder="请选择发送小程序"
                          class="w-full"
                          :options="
                            channelSceneOptions.map(i => ({
                              label: i.desc,
                              value: i.code
                            }))
                          "
                        />
                      </AFormItem>
                    </ACol>
                    <ACol :span="24">
                      <AFormItem
                        label="字段映射"
                        :label-col="TARGET_CARD_FORM_LABEL_COL"
                        :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                      >
                        <div class="mapping-toolbar">
                          <div class="field-tip">
                            MP_SUBSCRIBE 的渠道参数统一落在 config_json，页面仅维护结构化字段，不直接编辑原始 JSON。
                          </div>
                          <AButton
                            v-if="!dialogReadonly"
                            type="primary"
                            ghost
                            size="small"
                            @click="addFieldMapping(targetMeta.targetType)"
                          >
                            新增字段
                          </AButton>
                        </div>
                        <ATable
                          :columns="fieldMappingColumns"
                          :data-source="getTargetForm(targetMeta.targetType).fieldMapping"
                          :pagination="false"
                          size="small"
                          bordered
                          row-key="field"
                        >
                          <template #bodyCell="{ column, record, index }">
                            <template v-if="column.key === 'field'">
                              <AInput
                                v-model:value="record.field"
                                :disabled="dialogReadonly"
                                placeholder="例如 thing1"
                              />
                            </template>
                            <template v-else-if="column.key === 'value'">
                              <AInput
                                v-model:value="record.value"
                                :disabled="dialogReadonly"
                                placeholder="例如 ${orderNo}"
                              />
                            </template>
                            <template v-else-if="column.key === 'actions'">
                              <AButton
                                v-if="!dialogReadonly"
                                type="link"
                                size="small"
                                danger
                                @click="removeFieldMapping(targetMeta.targetType, index)"
                              >
                                删除
                              </AButton>
                            </template>
                          </template>
                        </ATable>
                      </AFormItem>
                    </ACol>
                  </ARow>
                </template>

                <ARow :gutter="16" class="target-card-form-row">
                  <ACol :span="24">
                    <AFormItem
                      label="目标备注"
                      :label-col="TARGET_CARD_FORM_LABEL_COL"
                      :wrapper-col="TARGET_CARD_FORM_WRAPPER_COL"
                    >
                      <ATextarea
                        v-model:value="getTargetForm(targetMeta.targetType).remark"
                        :disabled="dialogReadonly"
                        :rows="2"
                        placeholder="请输入目标备注"
                        class="w-full"
                      />
                    </AFormItem>
                  </ACol>
                </ARow>
              </div>
            </ATabPane>
          </ATabs>
        </div>

        <div class="section-title">可用变量</div>
        <ATable
          :columns="variableTransposedColumns"
          :data-source="variableTransposedRows"
          :pagination="false"
          :scroll="{ x: 'max-content' }"
          :show-header="false"
          size="small"
          bordered
          row-key="rowKey"
          class="notify-scene-variables-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'rowLabel'">
              <span class="notify-scene-variables-table__row-label">{{ record.rowLabel }}</span>
            </template>
          </template>
        </ATable>
      </AForm>

      <template v-if="!dialogReadonly" #footer>
        <ASpace :size="16">
          <AButton @click="dialogOpen = false">取消</AButton>
          <AButton type="primary" :loading="submitLoading" @click="submitSceneConfig">保存</AButton>
        </ASpace>
      </template>
      <template v-else #footer>
        <AButton @click="dialogOpen = false">关闭</AButton>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="previewOpen"
      title="目标配置预览"
      placement="right"
      :width="820"
      :body-style="{ paddingTop: 0 }"
      destroy-on-close
    >
      <AForm label-align="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <AFormItem label="场景">
          <AInput :value="previewForm.sceneName" disabled />
        </AFormItem>
        <AFormItem label="通知目标">
          <AInput :value="previewForm.targetTypeDesc" disabled />
        </AFormItem>
        <AFormItem label="示例变量 JSON">
          <ATextarea v-model:value="previewForm.variablesText" :rows="8" />
        </AFormItem>
      </AForm>
      <div class="preview-toolbar">
        <AButton type="primary" :loading="previewLoading" @click="loadPreview">执行预览</AButton>
      </div>
      <ADescriptions v-if="previewForm.result" bordered size="small" :column="1" class="mt-12px">
        <ADescriptionsItem label="标题">{{ previewForm.result.title || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="内容">{{ previewForm.result.content || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="跳转类型">{{ formatRouteType(previewForm.result.routeType) }}</ADescriptionsItem>
        <ADescriptionsItem label="跳转值">{{ previewForm.result.routeValue || '-' }}</ADescriptionsItem>
        <ADescriptionsItem v-if="isMiniProgramTarget(previewForm.targetType)" label="页面路径">
          {{ previewForm.result.pagePath || '-' }}
        </ADescriptionsItem>
      </ADescriptions>
      <div v-if="previewForm.result?.fieldMapping?.length" class="preview-mapping">
        <div class="section-title">字段映射预览</div>
        <ATable
          :columns="previewMappingColumns"
          :data-source="previewForm.result.fieldMapping"
          :pagination="false"
          size="small"
          bordered
          row-key="field"
        />
      </div>
      <template #footer>
        <AButton @click="previewOpen = false">关闭</AButton>
      </template>
    </ADrawer>
  </div>
</template>

<style scoped>
.section-title {
  margin: 18px 0 12px;
  font-weight: 600;
  color: #303133;
}
.muted-text {
  color: #909399;
}
.notify-scene-drawer-form :deep(.ant-form-item-label > label) {
  justify-content: flex-start;
}
.notify-scene-drawer-form :deep(.ant-form-item-control-input) {
  text-align: left;
}
.notify-scene-variables-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.notify-scene-variables-table :deep(.ant-table-tbody > tr > td:first-child) {
  background: #fafafa;
}
.notify-scene-variables-table__row-label {
  font-weight: 600;
  color: #303133;
}
.notify-scene-target-section {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  column-gap: 16px;
  row-gap: 12px;
  margin: 0 0 12px;
}
.notify-scene-target-section__title {
  margin: 0;
  flex-shrink: 0;
  line-height: 32px;
  white-space: nowrap;
}
/* 让 Tabs 导航与标题同一行，面板内容独占下一行 */
.notify-scene-target-section__tabs {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
  display: contents;
}
.notify-scene-target-section__tabs :deep(.ant-tabs-nav) {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}
.notify-scene-target-section__tabs :deep(.ant-tabs-content-holder) {
  width: 100%;
  flex-basis: 100%;
}
.notify-scene-target-tabs__tag {
  margin-left: 6px;
  vertical-align: middle;
}
.notify-scene-target-section__tabs :deep(.ant-tabs-tabpane) {
  padding-left: 16px;
  padding-right: 16px;
}
.target-card {
  margin-top: 12px;
  margin-bottom: 0;
  padding: 16px 8px 4px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
/* Tab 内表单：标签列宽与「标题模板」一致，控件占满行内剩余宽度 */
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: flex-start;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item-label) {
  flex: 0 0 108px !important;
  width: 108px !important;
  max-width: 108px !important;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item-label > label) {
  width: 100%;
  height: auto;
  white-space: normal;
  word-break: keep-all;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item-control) {
  flex: 1 1 0 !important;
  min-width: 0 !important;
  max-width: none !important;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item-control-input),
.notify-scene-drawer-form .target-card-form :deep(.ant-form-item-control-input-content) {
  width: 100%;
  max-width: 100%;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-input),
.notify-scene-drawer-form .target-card-form :deep(.ant-input-affix-wrapper),
.notify-scene-drawer-form .target-card-form :deep(textarea.ant-input),
.notify-scene-drawer-form .target-card-form :deep(.ant-select) {
  width: 100%;
}
.notify-scene-drawer-form .target-card-form :deep(.ant-table-wrapper) {
  width: 100%;
}
.target-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
.target-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.target-card__sub {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
.target-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.mapping-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.mapping-toolbar .field-tip {
  flex: 1;
  min-width: 0;
  margin: 0;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}
.preview-toolbar {
  margin-bottom: 12px;
}
.preview-mapping {
  margin-top: 16px;
}
</style>
