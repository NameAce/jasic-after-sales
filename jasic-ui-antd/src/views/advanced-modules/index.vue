<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { tagColorEnabled, tagColorPositiveNeutral } from '@/constants/list-status-tag';
import {
  addConfig,
  addDictType,
  addFaultRepairConfig,
  addNotifyTemplateCustom,
  addRegion,
  addRoleTemplate,
  addSyncTask,
  deleteConfig,
  deleteDictType,
  deleteNotifyTemplateCustom,
  deleteRegion,
  deleteRoleTemplate,
  executeSyncTask,
  fullSyncMachineBarcode,
  getFaultRepairConfig,
  getMachineBarcode,
  getNotifyTemplate,
  getSyncTask,
  listCompany,
  listCompanyType,
  listDictType,
  listFaultRepairConfig,
  listFaultRepairConfigCompanyOptions,
  listMachineBarcode,
  listNotifyTemplate,
  listNotifyTemplateChannels,
  listRegion,
  listRoleTemplate,
  listSyncTask,
  listSyncTaskHandlerOptions,
  listSyncTaskLog,
  listSystemConfig,
  previewNotifyTemplate,
  refreshConfigCache,
  refreshDictTypeCache,
  refreshNotifyTemplateCache,
  roleTemplateDataScopeOptionMap,
  roleTemplateDataScopeOptions,
  saveNotifyTemplateChannels,
  syncRoleTemplate,
  typeCodeMenuTree,
  updateConfig,
  updateDictType,
  updateFaultRepairConfig,
  updateNotifyTemplateCustom,
  updateRegion,
  updateRoleTemplate,
  updateSyncTask
} from '@/service/api';
import { useAuth } from '@/hooks/business/auth';
import { useTableScroll } from '@/hooks/common/table';

type RowData = Record<string, any>;
type DataScopeOption = {
  value: string;
  label: string;
  defaultOption?: boolean;
  disabled?: boolean;
};
type ModuleKey = 'dict' | 'config' | 'notifyTemplate' | 'barcode' | 'syncTask' | 'fault' | 'roleTemplate' | 'region';

const { tableWrapperRef, scrollConfig } = useTableScroll(960);
const route = useRoute();
const { hasAuth } = useAuth();

const loading = ref(false);
const activeKey = ref<ModuleKey>('dict');
const rows = ref<RowData[]>([]);
const total = ref(0);
const pageQuery = reactive({ pageNum: 1, pageSize: 10 });

const dictQuery = reactive({
  dictName: '',
  dictType: '',
  status: undefined as number | undefined
});

const configQuery = reactive({
  configName: '',
  configKey: '',
  configType: undefined as number | undefined
});

const notifyQuery = reactive({
  templateCode: '',
  templateName: '',
  templateSource: undefined as string | undefined
});

const barcodeQuery = reactive({
  barcode: '',
  deliverNumber: '',
  productCode: '',
  machineNo: '',
  productModel: '',
  status: undefined as number | undefined
});

const syncTaskQuery = reactive({
  taskCode: '',
  taskName: '',
  handlerCode: undefined as string | undefined,
  status: undefined as number | undefined
});

const faultQuery = reactive({
  companyId: undefined as number | undefined,
  productCode: '',
  productModel: '',
  faultDesc: '',
  status: undefined as number | undefined
});

const roleTemplateTypeCode = ref<string | undefined>(undefined);
const typeCodeLabelMap = ref<Record<string, string>>({});

const regionHqId = ref<number | undefined>(undefined);
const hqCompanyOptions = ref<RowData[]>([]);

const handlerOptions = ref<RowData[]>([]);
const faultCompanyOptions = ref<RowData[]>([]);

const tabOptions = [
  { key: 'dict' as const, label: '字典管理' },
  { key: 'config' as const, label: '参数配置' },
  { key: 'notifyTemplate' as const, label: '通知模板' },
  { key: 'barcode' as const, label: '机器条码档案' },
  { key: 'syncTask' as const, label: '同步任务' },
  { key: 'fault' as const, label: '故障维修配置' },
  { key: 'roleTemplate' as const, label: '角色模板' },
  { key: 'region' as const, label: '系统大区' }
];

const ROUTE_NAME_TO_MODULE_KEY: Record<string, ModuleKey> = {
  'system_role-template': 'roleTemplate',
  system_config: 'config',
  'system_dict-type': 'dict',
  'system_dict-data': 'dict',
  'system_notify-template': 'notifyTemplate',
  'system_machine-barcode': 'barcode',
  'system_sync-task': 'syncTask',
  'system_fault-repair-config': 'fault',
  system_region: 'region'
};

const formOpen = ref(false);
const formModel = reactive<RowData>({});
const formTitle = ref('');

const barcodeDetailOpen = ref(false);
const barcodeDetail = ref<RowData | null>(null);
const barcodeDetailRows = computed(() => {
  const detail = barcodeDetail.value || {};
  const statusValue = Number(detail.status);
  const statusLabel = statusValue === 1 ? '启用' : statusValue === 0 ? '停用' : '-';
  return [
    { key: 'barcode', label: '条码', value: detail.barcode || '-' },
    { key: 'deliverNumber', label: '发货单号', value: detail.deliverNumber || '-' },
    { key: 'hqCompanyName', label: '归属总部', value: detail.hqCompanyName || '-' },
    { key: 'custId', label: 'CRM公司ID', value: detail.custId || '-' },
    { key: 'salesOrg', label: '销售组织', value: detail.salesOrg || '-' },
    { key: 'productCode', label: '物料编码', value: detail.productCode || '-' },
    { key: 'productName', label: '商品名称', value: detail.productName || '-' },
    { key: 'productModel', label: '产品型号', value: detail.productModel || '-' },
    { key: 'machineNo', label: '机器小号', value: detail.machineNo || '-' },
    { key: 'scanDate', label: '条码扫码时间', value: detail.scanDate || '-' },
    { key: 'lastOutDate', label: '最后出库日期', value: detail.lastOutDate || '-' },
    { key: 'crmAddTime', label: 'CRM创建时间', value: detail.crmAddTime || '-' },
    { key: 'lastSyncTime', label: '最近同步时间', value: detail.lastSyncTime || '-' },
    { key: 'warrantyStatus', label: '质保状态', value: detail.warrantyStatus || '-' },
    { key: 'status', label: '状态', value: statusLabel },
    { key: 'remark', label: '备注', value: detail.remark || '-' }
  ];
});

const notifyViewOpen = ref(false);
const notifyViewRecord = ref<RowData | null>(null);
const notifyFormOpen = ref(false);
const notifyFormTitle = ref('');
const notifyFormReadonly = ref(false);
const notifyFormSubmitting = ref(false);
const notifyForm = reactive<RowData>({});
const notifyPreviewOpen = ref(false);
const notifyPreviewLoading = ref(false);
const notifyPreviewVariablesText = ref('');
const notifyPreviewResult = ref<RowData | null>(null);
const notifyPreviewPayload = ref<RowData | null>(null);
const notifyRouteTypeOptions = [
  { label: '工单详情', value: 'WORK_ORDER_DETAIL' },
  { label: '工单评价', value: 'WORK_ORDER_EVALUATE' }
];

const channelsOpen = ref(false);
const channelsLoading = ref(false);
const channelsReadonly = ref(false);
const channelsTemplateCode = ref('');
const channelsRows = ref<RowData[]>([]);

const syncFormOpen = ref(false);
const syncFormTitle = ref('');
const syncFormModel = reactive<RowData>({});

const logOpen = ref(false);
const logDialogTitle = ref('执行日志');
const logTaskId = ref<number | string | undefined>(undefined);
const logRows = ref<RowData[]>([]);
const logTotal = ref(0);
const logPage = reactive({ pageNum: 1, pageSize: 10 });
const logLoading = ref(false);
const logQuery = reactive({
  status: undefined as string | undefined
});

const roleTemplateDataScopeMap = ref<Record<string, DataScopeOption[]>>({});
const roleTemplateScopeOptions = ref<Array<{ label: string; value: string }>>([]);
const menuAssignOpen = ref(false);
const menuAssignSubmitting = ref(false);
const menuAssignTypeCode = ref('');
const menuAssignTemplate = ref<RowData | null>(null);
const menuTreeData = ref<any[]>([]);
const menuCheckedKeys = ref<Array<string | number>>([]);

const regionFormOpen = ref(false);
const regionFormTitle = ref('');
const regionForm = reactive<RowData>({});

const faultDetailOpen = ref(false);
const faultDetail = ref<RowData | null>(null);
type FaultRepairItem = {
  faultDesc: string;
  repairOptions: string[];
};

function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

function pickTotal(data: any) {
  return Number(data?.total) || 0;
}

function listParams() {
  return { pageNum: pageQuery.pageNum, pageSize: pageQuery.pageSize };
}

async function ensureHandlerOptions() {
  if (handlerOptions.value.length) return;
  try {
    const { data } = await listSyncTaskHandlerOptions();
    handlerOptions.value = Array.isArray(data) ? data : pickRows(data);
  } catch {
    handlerOptions.value = [];
  }
}

async function ensureFaultCompanyOptions() {
  if (faultCompanyOptions.value.length) return;
  try {
    const { data } = await listFaultRepairConfigCompanyOptions();
    faultCompanyOptions.value = Array.isArray(data) ? data : pickRows(data);
  } catch {
    faultCompanyOptions.value = [];
  }
}

async function loadHqForRegion() {
  const { data } = await listCompany({ pageNum: 1, pageSize: 999, category: 'HQ' });
  const list = pickRows(data);
  hqCompanyOptions.value = list;
  if (!regionHqId.value && list.length) {
    regionHqId.value = list[0].id;
  }
}

async function loadTypeCodeLabels() {
  try {
    const { data } = await listCompanyType();
    const list = Array.isArray(data) ? data : pickRows(data);
    const map: Record<string, string> = {};
    for (const t of list) {
      if (t?.typeCode) map[t.typeCode] = t.typeName ?? t.typeCode;
    }
    typeCodeLabelMap.value = map;
  } catch {
    typeCodeLabelMap.value = {};
  }
  try {
    const { data } = await roleTemplateDataScopeOptionMap();
    roleTemplateDataScopeMap.value = ((data as unknown as Record<string, DataScopeOption[]>) || {}) as Record<
      string,
      DataScopeOption[]
    >;
  } catch {
    roleTemplateDataScopeMap.value = {};
  }
}

function loadByModule() {
  const p = listParams();
  switch (activeKey.value) {
    case 'dict':
      return listDictType({
        ...p,
        dictName: dictQuery.dictName || undefined,
        dictType: dictQuery.dictType || undefined,
        status: dictQuery.status
      });
    case 'config':
      return listSystemConfig({
        ...p,
        configName: configQuery.configName || undefined,
        configKey: configQuery.configKey || undefined,
        configType: configQuery.configType
      });
    case 'notifyTemplate':
      return listNotifyTemplate({
        ...p,
        templateCode: notifyQuery.templateCode || undefined,
        templateName: notifyQuery.templateName || undefined,
        templateSource: notifyQuery.templateSource
      });
    case 'barcode':
      return listMachineBarcode({
        ...p,
        barcode: barcodeQuery.barcode || undefined,
        deliverNumber: barcodeQuery.deliverNumber || undefined,
        productCode: barcodeQuery.productCode || undefined,
        machineNo: barcodeQuery.machineNo || undefined,
        productModel: barcodeQuery.productModel || undefined,
        status: barcodeQuery.status
      });
    case 'syncTask':
      return listSyncTask({
        ...p,
        taskCode: syncTaskQuery.taskCode || undefined,
        taskName: syncTaskQuery.taskName || undefined,
        handlerCode: syncTaskQuery.handlerCode,
        status: syncTaskQuery.status
      });
    case 'fault':
      return listFaultRepairConfig({
        ...p,
        companyId: faultQuery.companyId,
        productCode: faultQuery.productCode || undefined,
        productModel: faultQuery.productModel || undefined,
        faultDesc: faultQuery.faultDesc || undefined,
        status: faultQuery.status
      });
    case 'roleTemplate':
      return listRoleTemplate(roleTemplateTypeCode.value || undefined, p);
    case 'region':
      if (regionHqId.value == null) {
        return Promise.resolve({ data: [] });
      }
      return listRegion(regionHqId.value);
    default:
      return listDictType(p);
  }
}

const columns = computed(() => {
  const actionCol = { title: '操作', key: 'actions', width: 220, fixed: 'right' as const };
  switch (activeKey.value) {
    case 'dict':
      return [
        { title: '字典名称', dataIndex: 'dictName', key: 'dictName', width: 160 },
        { title: '字典类型', dataIndex: 'dictType', key: 'dictType', width: 180 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
        { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
        actionCol
      ];
    case 'config':
      return [
        { title: '参数名称', dataIndex: 'configName', key: 'configName', width: 160 },
        { title: '参数键', dataIndex: 'configKey', key: 'configKey', width: 180 },
        { title: '参数值', dataIndex: 'configValue', key: 'configValue', ellipsis: true },
        { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
        { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
        actionCol
      ];
    case 'notifyTemplate':
      return [
        { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode', width: 200 },
        { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: 180 },
        { title: '模板来源', dataIndex: 'templateSource', key: 'templateSource', width: 110 },
        { title: '通知开关', dataIndex: 'notifyEnabled', key: 'notifyEnabled', width: 100 },
        { title: '覆盖开关', dataIndex: 'overrideEnabled', key: 'overrideEnabled', width: 100 },
        { title: '路由类型', dataIndex: 'routeType', key: 'routeType', width: 160 },
        { title: '标题模板', dataIndex: 'titleTemplate', key: 'titleTemplate', ellipsis: true },
        { title: '摘要模板', dataIndex: 'summaryTemplate', key: 'summaryTemplate', ellipsis: true },
        { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
        actionCol
      ];
    case 'barcode':
      return [
        { title: '条码', dataIndex: 'barcode', key: 'barcode', width: 180 },
        { title: '发货单号', dataIndex: 'deliverNumber', key: 'deliverNumber', width: 140 },
        { title: '归属总部', dataIndex: 'hqCompanyName', key: 'hqCompanyName', width: 160 },
        { title: 'CRM公司ID', dataIndex: 'custId', key: 'custId', width: 120 },
        { title: '销售组织', dataIndex: 'salesOrg', key: 'salesOrg', width: 120 },
        { title: '物料编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
        { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 160, ellipsis: true },
        { title: '产品型号', dataIndex: 'productModel', key: 'productModel', width: 140 },
        { title: '机器小号', dataIndex: 'machineNo', key: 'machineNo', width: 140 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
        { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 }
      ];
    case 'syncTask':
      return [
        { title: '任务编码', dataIndex: 'taskCode', key: 'taskCode', width: 160 },
        { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 180 },
        { title: '处理器', dataIndex: 'handlerName', key: 'handlerName', width: 140 },
        { title: 'Cron', dataIndex: 'cronExpression', key: 'cronExpression', width: 160 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
        { title: '最近状态', dataIndex: 'lastStatus', key: 'lastStatus', width: 100 },
        { title: '最近结束', dataIndex: 'lastEndTime', key: 'lastEndTime', width: 170 },
        { title: '下次触发', dataIndex: 'nextFireTime', key: 'nextFireTime', width: 170 },
        actionCol
      ];
    case 'fault':
      return [
        { title: '归属总部', dataIndex: 'companyName', key: 'companyName', width: 180 },
        { title: '物料编码', dataIndex: 'productCode', key: 'productCode', width: 140 },
        { title: '产品型号', dataIndex: 'productModel', key: 'productModel', width: 140 },
        { title: '故障摘要', dataIndex: 'faultDescSummary', key: 'faultDescSummary', ellipsis: true },
        { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
        { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
        actionCol
      ];
    case 'roleTemplate':
      return [
        { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 180 },
        { title: '角色标识', dataIndex: 'roleKey', key: 'roleKey', width: 160 },
        { title: '所属类型', dataIndex: 'typeCode', key: 'typeCode', width: 140 },
        { title: '管理员', dataIndex: 'isAdmin', key: 'isAdmin', width: 90 },
        { title: '数据范围', dataIndex: 'dataScope', key: 'dataScope', width: 140 },
        { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
        actionCol
      ];
    case 'region':
      return [
        { title: '大区编码', dataIndex: 'regionCode', key: 'regionCode', width: 140 },
        { title: '大区名称', dataIndex: 'regionName', key: 'regionName', width: 200 },
        { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
        actionCol
      ];
    default:
      return [];
  }
});

/** 字典/参数：表格行内「编辑/删除」 */
const hasDictConfigRowActions = computed(() => activeKey.value === 'dict' || activeKey.value === 'config');

function resolveRowKey(r: RowData) {
  return r.id ?? r.dictId ?? r.configId ?? r.templateCode ?? String(r.taskCode ?? r.barcode ?? Math.random());
}

async function loadList() {
  loading.value = true;
  try {
    if (activeKey.value === 'region') {
      await loadHqForRegion();
    }
    if (activeKey.value === 'syncTask') {
      await ensureHandlerOptions();
    }
    if (activeKey.value === 'fault') {
      await ensureFaultCompanyOptions();
    }
    if (activeKey.value === 'roleTemplate') {
      await loadTypeCodeLabels();
    }

    const { data } = await loadByModule();
    rows.value = pickRows(data);
    if (activeKey.value === 'region') {
      total.value = rows.value.length;
    } else {
      total.value = pickTotal(data) || rows.value.length;
    }
  } finally {
    loading.value = false;
  }
}

function syncActiveModuleByRouteName(routeName: unknown) {
  const key = String(routeName || '');
  const moduleKey = ROUTE_NAME_TO_MODULE_KEY[key];
  if (!moduleKey || moduleKey === activeKey.value) return false;

  activeKey.value = moduleKey;
  pageQuery.pageNum = 1;
  formOpen.value = false;
  syncFormOpen.value = false;
  loadList();
  return true;
}

function handleSearch() {
  pageQuery.pageNum = 1;
  loadList();
}

function resetSearch() {
  switch (activeKey.value) {
    case 'dict':
      dictQuery.dictName = '';
      dictQuery.dictType = '';
      dictQuery.status = undefined;
      break;
    case 'config':
      configQuery.configName = '';
      configQuery.configKey = '';
      configQuery.configType = undefined;
      break;
    case 'notifyTemplate':
      notifyQuery.templateCode = '';
      notifyQuery.templateName = '';
      notifyQuery.templateSource = undefined;
      break;
    case 'barcode':
      barcodeQuery.barcode = '';
      barcodeQuery.deliverNumber = '';
      barcodeQuery.productCode = '';
      barcodeQuery.machineNo = '';
      barcodeQuery.productModel = '';
      barcodeQuery.status = undefined;
      break;
    case 'syncTask':
      syncTaskQuery.taskCode = '';
      syncTaskQuery.taskName = '';
      syncTaskQuery.handlerCode = undefined;
      syncTaskQuery.status = undefined;
      break;
    case 'fault':
      faultQuery.companyId = undefined;
      faultQuery.productCode = '';
      faultQuery.productModel = '';
      faultQuery.faultDesc = '';
      faultQuery.status = undefined;
      break;
    case 'roleTemplate':
      roleTemplateTypeCode.value = undefined;
      break;
    case 'region':
      regionHqId.value = undefined;
      break;
    default:
      break;
  }

  handleSearch();
}

function onPageChange(page: number, pageSize?: number) {
  if (pageSize !== undefined && pageSize !== pageQuery.pageSize) {
    pageQuery.pageSize = pageSize;
    pageQuery.pageNum = 1;
  } else {
    pageQuery.pageNum = page;
    if (pageSize !== undefined) pageQuery.pageSize = pageSize;
  }
  loadList();
}

async function openForm(record?: RowData, title?: string) {
  if (activeKey.value === 'roleTemplate') {
    formTitle.value = title || (record?.id ? '编辑模板' : '新增模板');
  } else {
    formTitle.value = title || (record?.id ? '编辑' : '新增');
  }
  Object.keys(formModel).forEach(k => delete formModel[k]);
  if (record) Object.assign(formModel, JSON.parse(JSON.stringify(record)));
  if (activeKey.value === 'dict' && !record) {
    formModel.status = 1;
  }
  if (activeKey.value === 'config' && !record) {
    formModel.configType = 0;
  }
  if (activeKey.value === 'config' && record) {
    if (formModel.configType === undefined || formModel.configType === null || formModel.configType === '') {
      formModel.configType = formModel.status;
    }
  }
  if (activeKey.value === 'fault') {
    if (record?.id != null) {
      try {
        const { data } = await getFaultRepairConfig(record.id);
        Object.assign(formModel, normalizeFaultFormData((data as RowData) || record));
      } catch {
        Object.assign(formModel, normalizeFaultFormData(record));
      }
    } else {
      const defaultCompanyId =
        faultCompanyOptions.value.length === 1
          ? Number(faultCompanyOptions.value[0].id ?? faultCompanyOptions.value[0].value)
          : undefined;
      Object.assign(formModel, normalizeFaultFormData({ companyId: defaultCompanyId, status: 1 }));
    }
  }

  if (activeKey.value === 'roleTemplate') {
    const defaultTypeCode = String(roleTemplateTypeCode.value || Object.keys(typeCodeLabelMap.value)[0] || '');
    if (!record) {
      formModel.roleName = '';
      formModel.roleKey = '';
      formModel.remark = '';
      formModel.isAdmin = 0;
      formModel.typeCode = defaultTypeCode;
      formModel.dataScope = '';
    }
    formModel.isAdmin = Number(formModel.isAdmin) === 1 ? 1 : 0;
    await onRoleTemplateDataScopeInit(Boolean(record));
  }
  formOpen.value = true;
}

function isFormFieldRequired(key: string) {
  if (activeKey.value === 'dict') {
    return key === 'dictName' || key === 'dictType' || key === 'status';
  }
  if (activeKey.value === 'config') {
    return key === 'configName' || key === 'configKey' || key === 'configValue' || key === 'configType';
  }
  return false;
}

function formFields(): { label: string; key: string; type: 'input' | 'textarea' | 'number' | 'radio' }[] {
  switch (activeKey.value) {
    case 'dict':
      return [
        { label: '字典名称', key: 'dictName', type: 'input' },
        { label: '字典类型', key: 'dictType', type: 'input' },
        { label: '状态', key: 'status', type: 'radio' },
        { label: '备注', key: 'remark', type: 'textarea' }
      ];
    case 'config':
      return [
        { label: '参数名称', key: 'configName', type: 'input' },
        { label: '参数键名', key: 'configKey', type: 'input' },
        { label: '参数键值', key: 'configValue', type: 'textarea' },
        { label: '是否内置', key: 'configType', type: 'radio' },
        { label: '备注', key: 'remark', type: 'textarea' }
      ];
    case 'notifyTemplate':
      return [
        { label: '模板编码', key: 'templateCode', type: 'input' },
        { label: '模板名称', key: 'templateName', type: 'input' },
        { label: '通知开关(1/0)', key: 'notifyEnabled', type: 'number' },
        { label: '覆盖开关(1/0)', key: 'overrideEnabled', type: 'number' },
        { label: '路由类型', key: 'routeType', type: 'input' },
        { label: '标题模板', key: 'titleTemplate', type: 'textarea' },
        { label: '摘要模板', key: 'summaryTemplate', type: 'textarea' },
        { label: '路由值模板', key: 'routeValueTemplate', type: 'input' }
      ];
    case 'fault':
      return [
        { label: '归属总部公司ID', key: 'companyId', type: 'number' },
        { label: '物料编码', key: 'productCode', type: 'input' },
        { label: '产品型号', key: 'productModel', type: 'input' },
        { label: '状态', key: 'status', type: 'number' },
        { label: '备注', key: 'remark', type: 'textarea' }
      ];
    case 'roleTemplate':
      return [
        { label: '角色名称', key: 'roleName', type: 'input' },
        { label: '角色标识', key: 'roleKey', type: 'input' },
        { label: '类型编码 typeCode', key: 'typeCode', type: 'input' },
        { label: '数据范围 dataScope', key: 'dataScope', type: 'input' },
        { label: '备注', key: 'remark', type: 'textarea' }
      ];
    default:
      return [];
  }
}

function getFormRadioOptions(key: string) {
  if (key === 'status') {
    return [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ];
  }
  if (key === 'configType') {
    return [
      { label: '是', value: 1 },
      { label: '否', value: 0 }
    ];
  }
  return [];
}

function getFormPlaceholder(key: string, type: 'input' | 'textarea' | 'number' | 'radio') {
  if (activeKey.value === 'dict') {
    if (key === 'dictName') return '请输入字典名称';
    if (key === 'dictType') return '如 sys_yes_no';
    if (key === 'remark' && type === 'textarea') return '请输入备注';
  }
  if (activeKey.value === 'config') {
    if (key === 'configName') return '请输入参数名称';
    if (key === 'configKey') return '请输入参数键名';
    if (key === 'configValue' && type === 'textarea') return '请输入参数键值';
    if (key === 'remark' && type === 'textarea') return '请输入备注';
  }
  return undefined;
}

function createFaultItem(): FaultRepairItem {
  return {
    faultDesc: '',
    repairOptions: ['']
  };
}

function normalizeFaultFormData(data?: RowData) {
  const faultsRaw = Array.isArray(data?.faults) ? data.faults : [];
  const faults: FaultRepairItem[] = (faultsRaw.length ? faultsRaw : [createFaultItem()]).map((item: RowData) => {
    const repairOptions = Array.isArray(item?.repairOptions)
      ? item.repairOptions.map((x: unknown) => String(x ?? '').trim()).filter(Boolean)
      : [];
    return {
      faultDesc: String(item?.faultDesc ?? ''),
      repairOptions: repairOptions.length ? repairOptions : ['']
    };
  });

  return {
    id: data?.id,
    companyId: data?.companyId,
    companyName: data?.companyName,
    productCode: String(data?.productCode ?? ''),
    productModel: String(data?.productModel ?? ''),
    status: data?.status === 0 ? 0 : 1,
    remark: String(data?.remark ?? ''),
    faults
  };
}

function addFaultItem() {
  const list = Array.isArray(formModel.faults) ? (formModel.faults as FaultRepairItem[]) : [];
  list.push(createFaultItem());
  formModel.faults = list;
}

function removeFaultItem(index: number) {
  const list = Array.isArray(formModel.faults) ? (formModel.faults as FaultRepairItem[]) : [];
  if (list.length <= 1) return;
  list.splice(index, 1);
}

function addRepairOption(item: FaultRepairItem) {
  item.repairOptions.push('');
}

function removeRepairOption(item: FaultRepairItem, index: number) {
  if (item.repairOptions.length <= 1) return;
  item.repairOptions.splice(index, 1);
}

function validateFaultForm(data: RowData) {
  if (data.companyId == null || data.companyId === '') {
    window.$message?.warning?.('请选择归属总部');
    return false;
  }
  if (data.status == null || data.status === '') {
    window.$message?.warning?.('请选择状态');
    return false;
  }
  if (!String(data.productCode || '').trim() && !String(data.productModel || '').trim()) {
    window.$message?.warning?.('物料编码和产品型号不能同时为空');
    return false;
  }
  const faults = Array.isArray(data.faults) ? data.faults : [];
  if (!faults.length) {
    window.$message?.warning?.('请至少添加一条故障信息');
    return false;
  }
  const descSet = new Set<string>();
  for (const item of faults) {
    const desc = String(item?.faultDesc || '').trim();
    if (!desc) {
      window.$message?.warning?.('故障描述不能为空');
      return false;
    }
    if (descSet.has(desc)) {
      window.$message?.warning?.('同一配置下故障描述不能重复');
      return false;
    }
    descSet.add(desc);
    const options = (Array.isArray(item?.repairOptions) ? item.repairOptions : [])
      .map((x: unknown) => String(x || '').trim())
      .filter(Boolean);
    if (!options.length) {
      window.$message?.warning?.('维修说明不能为空');
      return false;
    }
    if (new Set(options).size !== options.length) {
      window.$message?.warning?.('同一故障下维修说明不能重复');
      return false;
    }
  }
  return true;
}

function buildFaultSubmitPayload(data: RowData) {
  return {
    id: data.id,
    companyId: Number(data.companyId),
    productCode: String(data.productCode || '').trim(),
    productModel: String(data.productModel || '').trim(),
    status: Number(data.status ?? 1),
    remark: String(data.remark || '').trim(),
    faults: (Array.isArray(data.faults) ? data.faults : []).map((item: RowData) => ({
      faultDesc: String(item.faultDesc || '').trim(),
      repairOptions: (Array.isArray(item.repairOptions) ? item.repairOptions : [])
        .map((x: unknown) => String(x || '').trim())
        .filter(Boolean)
    }))
  };
}

async function submitForm() {
  const data = { ...formModel };
  switch (activeKey.value) {
    case 'dict':
      if (!String(data.dictName || '').trim()) {
        window.$message?.warning?.('请输入字典名称');
        return;
      }
      if (!String(data.dictType || '').trim()) {
        window.$message?.warning?.('请输入字典类型');
        return;
      }
      if (data.status === undefined || data.status === null || data.status === '') {
        window.$message?.warning?.('请选择状态');
        return;
      }
      if (data.dictId ?? data.id) await updateDictType(data);
      else await addDictType(data);
      break;
    case 'config':
      if (!String(data.configName || '').trim()) {
        window.$message?.warning?.('请输入参数名称');
        return;
      }
      if (!String(data.configKey || '').trim()) {
        window.$message?.warning?.('请输入参数键名');
        return;
      }
      if (!String(data.configValue || '').trim()) {
        window.$message?.warning?.('请输入参数键值');
        return;
      }
      if (data.configType === undefined || data.configType === null || data.configType === '') {
        window.$message?.warning?.('请选择是否内置');
        return;
      }
      if (data.configId ?? data.id) await updateConfig(data);
      else await addConfig(data);
      break;
    case 'notifyTemplate':
      if (data.id) await updateNotifyTemplateCustom(data);
      else await addNotifyTemplateCustom(data);
      break;
    case 'fault':
      if (!validateFaultForm(data)) return;
      if (data.id) await updateFaultRepairConfig(buildFaultSubmitPayload(data));
      else await addFaultRepairConfig(buildFaultSubmitPayload(data));
      break;
    case 'roleTemplate':
      if (!String(data.roleName || '').trim()) {
        window.$message?.warning?.('请输入角色名称');
        return;
      }
      if (!String(data.roleKey || '').trim()) {
        window.$message?.warning?.('请输入角色标识');
        return;
      }
      if (!data.typeCode) {
        window.$message?.warning?.('请先选择类型编码');
        return;
      }
      if (!data.dataScope) {
        window.$message?.warning?.('请先选择数据范围');
        return;
      }
      if (!roleTemplateScopeOptions.value.some(item => String(item.value) === String(data.dataScope))) {
        window.$message?.warning?.('请选择当前公司类型允许的数据范围');
        return;
      }
      if (data.id) await updateRoleTemplate(data);
      else await addRoleTemplate(data);
      break;
    default:
      return;
  }
  window.$message?.success?.('操作成功');
  formOpen.value = false;
  loadList();
}

async function removeRow(record: RowData) {
  switch (activeKey.value) {
    case 'dict':
      await deleteDictType(record.dictId ?? record.id);
      break;
    case 'config':
      await deleteConfig(record.configId ?? record.id);
      break;
    case 'notifyTemplate':
      await deleteNotifyTemplateCustom(record.id);
      break;
    case 'fault':
      window.$message?.warning?.('jasic 后端无删除接口，请使用停用或联系管理员');
      return;
    case 'roleTemplate':
      await deleteRoleTemplate(record.id);
      break;
    case 'region':
      await deleteRegion(record.id);
      break;
    default:
      return;
  }
  window.$message?.success?.(activeKey.value === 'notifyTemplate' ? '删除成功' : '已删除');
  loadList();
}

async function onRefreshCache() {
  if (activeKey.value === 'dict') {
    await refreshDictTypeCache();
    window.$message?.success?.('字典缓存已刷新');
  } else if (activeKey.value === 'config') {
    await refreshConfigCache();
    window.$message?.success?.('参数缓存已刷新');
  } else if (activeKey.value === 'notifyTemplate') {
    await refreshNotifyTemplateCache();
    window.$message?.success?.('通知模板缓存已刷新');
  }
  loadList();
}

async function onRunSyncTask(record: RowData) {
  const id = record.id ?? record.taskId;
  if (id == null) return;
  await executeSyncTask(id);
  window.$message?.success?.('已触发执行');
  loadList();
}

async function onSyncRoleTemplateRow(record: RowData) {
  const id = record.id;
  if (id == null) return;
  await syncRoleTemplate(id);
  window.$message?.success?.('全量同步成功');
}

async function onFullSyncBarcode() {
  await fullSyncMachineBarcode();
  window.$message?.success?.('已触发全量同步');
  loadList();
}

async function openBarcodeDetail(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const { data } = await getMachineBarcode(id);
  barcodeDetail.value = (data as RowData) || record;
  barcodeDetailOpen.value = true;
}

function hasCustomNotifyTemplate(templateCode?: string) {
  const code = String(templateCode || '');
  if (!code) return false;
  return rows.value.some(
    item => String(item.templateCode || '') === code && String(item.templateSource || '') === 'CUSTOM'
  );
}

function buildNotifyPreviewVariables(templateCode?: string) {
  // 给预览接口准备一个“尽量全”的变量集合，
  // 避免不同模板的占位符差异导致标题/摘要/路由值渲染时缺变量。
  const commonVars = {
    // 通用业务类变量
    bizId: 88,
    bizNo: 'WO202604180001',
    receiverName: '张三',
    // 工单类变量（部分模板/渠道会用到）
    workOrderId: 10001,
    orderNo: 'WO202604210001',
    customerMobile: '13800138000',
    companyName: '深圳南山服务网点',
    closedTime: '2026-04-21 15:30:00'
  };

  if (templateCode === 'WORK_ORDER_EVALUATION_INVITE') {
    return JSON.stringify({ ...commonVars }, null, 2);
  }
  return JSON.stringify(commonVars, null, 2);
}

function fillNotifyForm(detail: RowData) {
  Object.keys(notifyForm).forEach(k => delete notifyForm[k]);
  Object.assign(notifyForm, {
    ...detail,
    notifyEnabled: Number(detail.notifyEnabled ?? 1),
    overrideEnabled: Number(detail.overrideEnabled ?? 0)
  });
}

async function openNotifyView(record: RowData) {
  notifyFormTitle.value = '查看通知模板';
  notifyFormReadonly.value = true;
  const detail = record.id == null ? record : ((await getNotifyTemplate(record.id)).data as RowData) || record;
  fillNotifyForm(detail);
  notifyFormOpen.value = true;
}

async function openNotifyAddCustom(record: RowData) {
  notifyFormTitle.value = '新增自定义模板';
  notifyFormReadonly.value = false;
  fillNotifyForm({
    templateCode: record.templateCode,
    templateName: record.templateName,
    notifyEnabled: 1,
    overrideEnabled: 0,
    routeType: record.routeType || '',
    titleTemplate: '',
    summaryTemplate: '',
    routeValueTemplate: '',
    remark: '',
    variablesJson: record.variablesJson || ''
  });
  notifyFormOpen.value = true;
}

async function openNotifyEdit(record: RowData) {
  notifyFormTitle.value = '编辑自定义模板';
  notifyFormReadonly.value = false;
  const detail = ((await getNotifyTemplate(record.id)).data as RowData) || record;
  fillNotifyForm(detail);
  notifyFormOpen.value = true;
}

async function submitNotifyForm() {
  if (!notifyForm.templateCode) return window.$message?.warning?.('模板编码不能为空');
  if (notifyForm.notifyEnabled == null) return window.$message?.warning?.('请选择通知开关');
  if (notifyForm.overrideEnabled == null) return window.$message?.warning?.('请选择覆盖开关');
  notifyFormSubmitting.value = true;
  try {
    const payload = {
      id: notifyForm.id,
      templateCode: notifyForm.templateCode,
      templateName: notifyForm.templateName,
      notifyEnabled: Number(notifyForm.notifyEnabled),
      overrideEnabled: Number(notifyForm.overrideEnabled),
      routeType: notifyForm.routeType,
      titleTemplate: notifyForm.titleTemplate,
      summaryTemplate: notifyForm.summaryTemplate,
      routeValueTemplate: notifyForm.routeValueTemplate,
      remark: notifyForm.remark
    };
    if (notifyForm.id) await updateNotifyTemplateCustom(payload);
    else await addNotifyTemplateCustom(payload);
    notifyFormOpen.value = false;
    window.$message?.success?.('操作成功');
    await loadList();
  } finally {
    notifyFormSubmitting.value = false;
  }
}

async function onPreviewNotify(record: RowData) {
  const payload = {
    templateCode: record.templateCode,
    notifyEnabled: record.notifyEnabled,
    overrideEnabled: record.overrideEnabled,
    routeType: record.routeType,
    titleTemplate: record.titleTemplate,
    summaryTemplate: record.summaryTemplate,
    routeValueTemplate: record.routeValueTemplate
  };
  notifyPreviewVariablesText.value = buildNotifyPreviewVariables(record.templateCode);
  notifyPreviewResult.value = null;
  notifyPreviewPayload.value = payload;
  notifyPreviewOpen.value = true;
  await runNotifyPreview(payload);
}

async function onPreviewNotifyForm() {
  const payload = {
    templateCode: notifyForm.templateCode,
    notifyEnabled: notifyForm.notifyEnabled,
    overrideEnabled: notifyForm.overrideEnabled,
    routeType: notifyForm.routeType,
    titleTemplate: notifyForm.titleTemplate,
    summaryTemplate: notifyForm.summaryTemplate,
    routeValueTemplate: notifyForm.routeValueTemplate
  };
  notifyPreviewVariablesText.value =
    notifyPreviewVariablesText.value || buildNotifyPreviewVariables(notifyForm.templateCode);
  notifyPreviewResult.value = null;
  notifyPreviewPayload.value = payload;
  notifyPreviewOpen.value = true;
  await runNotifyPreview(payload);
}

async function runNotifyPreview(payload: RowData) {
  let variables = {};
  try {
    variables = notifyPreviewVariablesText.value ? JSON.parse(notifyPreviewVariablesText.value) : {};
  } catch {
    window.$message?.error?.('示例变量 JSON 格式不正确');
    return;
  }
  notifyPreviewLoading.value = true;
  try {
    const { data } = await previewNotifyTemplate({ ...payload, variables });
    notifyPreviewResult.value = (data as RowData) || null;
  } finally {
    notifyPreviewLoading.value = false;
  }
}

async function openChannelsEditor(record: RowData) {
  channelsReadonly.value = !hasAuth('system:notifyTemplate:update');
  channelsTemplateCode.value = String(record.templateCode || '');
  channelsRows.value = [];
  channelsOpen.value = true;
  channelsLoading.value = true;
  try {
    const { data } = await listNotifyTemplateChannels(channelsTemplateCode.value);
    const rows = Array.isArray(data) ? data : [];
    channelsRows.value = rows.length ? rows.map(item => normalizeChannelRow(item as RowData)) : defaultChannelRows();
  } finally {
    channelsLoading.value = false;
  }
}

function createChannelRow() {
  if (channelsTemplateCode.value === 'WORK_ORDER_EVALUATION_INVITE') {
    return {
      channelType: 'MP_SUBSCRIBE',
      channelEnabled: 1,
      channelScene: 'C',
      templateId: '',
      pagePathTemplate: 'pages/order/evaluate?workOrderId=${workOrderId}',
      fieldMapping: [
        { field: 'thing1', value: '${orderNo}' },
        { field: 'phone_number2', value: '${customerMobile}' },
        { field: 'thing3', value: '${companyName}' }
      ],
      remark: '客户满意度评价通知默认渠道配置'
    };
  }
  return {
    channelType: 'MP_SUBSCRIBE',
    channelEnabled: 1,
    channelScene: '',
    templateId: '',
    pagePathTemplate: '',
    fieldMapping: [],
    remark: ''
  };
}

function defaultChannelRows() {
  return [createChannelRow()];
}

function normalizeChannelRow(item: RowData) {
  return {
    id: item.id,
    channelType: item.channelType || 'MP_SUBSCRIBE',
    channelEnabled: item.channelEnabled == null ? 1 : Number(item.channelEnabled),
    channelScene: item.channelScene || '',
    templateId: item.templateId || '',
    pagePathTemplate: item.pagePathTemplate || '',
    fieldMapping: (Array.isArray(item.fieldMapping) ? item.fieldMapping : []).map((mapping: RowData) => ({
      field: mapping.field || '',
      value: mapping.value || ''
    })),
    remark: item.remark || ''
  };
}

function buildChannelPayload(item: RowData) {
  return {
    id: item.id,
    templateCode: channelsTemplateCode.value,
    channelType: item.channelType,
    channelEnabled: Number(item.channelEnabled ?? 1),
    channelScene: item.channelScene,
    templateId: item.templateId,
    pagePathTemplate: item.pagePathTemplate,
    fieldMapping: (Array.isArray(item.fieldMapping) ? item.fieldMapping : []).map((mapping: RowData) => ({
      field: String(mapping.field || ''),
      value: String(mapping.value || '')
    })),
    remark: item.remark
  };
}

function addChannelRow() {
  channelsRows.value.push(createChannelRow());
}

function removeChannelRow(index: number) {
  channelsRows.value.splice(index, 1);
}

function addFieldMapping(item: RowData) {
  if (!Array.isArray(item.fieldMapping)) item.fieldMapping = [];
  item.fieldMapping.push({ field: '', value: '' });
}

function removeFieldMapping(item: RowData, index: number) {
  if (!Array.isArray(item.fieldMapping)) return;
  item.fieldMapping.splice(index, 1);
}

async function saveChannels() {
  channelsLoading.value = true;
  try {
    const payload = channelsRows.value.map(item => buildChannelPayload(item));
    await saveNotifyTemplateChannels(channelsTemplateCode.value, payload);
    window.$message?.success?.('渠道配置已保存');
    channelsOpen.value = false;
  } finally {
    channelsLoading.value = false;
  }
}

function openSyncForm(record?: RowData) {
  syncFormTitle.value = record ? '编辑同步任务' : '新增同步任务';
  Object.keys(syncFormModel).forEach(k => delete syncFormModel[k]);
  if (record) {
    Object.assign(syncFormModel, JSON.parse(JSON.stringify(record)));
  } else {
    syncFormModel.status = 0;
    syncFormModel.cronExpression = '0 0 2 * * ?';
  }
  syncFormOpen.value = true;
}

async function submitSyncForm() {
  const data = { ...syncFormModel };
  if (data.id) await updateSyncTask(data);
  else await addSyncTask(data);
  window.$message?.success?.('已保存');
  syncFormOpen.value = false;
  loadList();
}

async function openSyncFormEdit(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const { data } = await getSyncTask(id);
  openSyncForm((data as RowData) || record);
}

async function openLogDialog(record: RowData) {
  logTaskId.value = record.id;
  logPage.pageNum = 1;
  logDialogTitle.value = `执行日志 - ${record.taskName || ''}`;
  logQuery.status = undefined;
  logOpen.value = true;
  await loadSyncLogs();
}

async function loadSyncLogs() {
  if (logTaskId.value == null) return;
  logLoading.value = true;
  try {
    const { data } = await listSyncTaskLog({
      taskId: logTaskId.value,
      status: logQuery.status,
      pageNum: logPage.pageNum,
      pageSize: logPage.pageSize
    });
    logRows.value = pickRows(data);
    logTotal.value = pickTotal(data);
  } finally {
    logLoading.value = false;
  }
}

function onLogPageChange(page: number, pageSize?: number) {
  logPage.pageNum = page;
  if (pageSize) logPage.pageSize = pageSize;
  loadSyncLogs();
}

function handleLogStatusChange() {
  logPage.pageNum = 1;
  loadSyncLogs();
}

function syncLogStatusTagColor(status: unknown) {
  const s = String(status || '');
  if (s === 'SUCCESS') return 'success';
  if (s === 'FAILED') return 'error';
  if (s === 'RUNNING') return 'processing';
  return 'default';
}

async function openRoleTemplateMenuAssign(record: RowData) {
  const typeCode = String(record.typeCode || '');
  if (!typeCode) {
    window.$message?.warning?.('当前模板缺少 typeCode');
    return;
  }
  menuAssignTypeCode.value = typeCode;
  menuAssignTemplate.value = { ...record };
  menuAssignOpen.value = true;
  const { data: treeData } = await typeCodeMenuTree(typeCode);
  menuTreeData.value = Array.isArray(treeData) ? treeData : pickRows(treeData);
  menuCheckedKeys.value = (Array.isArray(record.menuIds) ? record.menuIds : []) as Array<string | number>;
}

async function submitRoleTemplateMenuAssign() {
  if (!menuAssignTypeCode.value || !menuAssignTemplate.value?.id) return;
  menuAssignSubmitting.value = true;
  try {
    await updateRoleTemplate({
      id: menuAssignTemplate.value.id,
      typeCode: menuAssignTemplate.value.typeCode,
      roleName: menuAssignTemplate.value.roleName,
      roleKey: menuAssignTemplate.value.roleKey,
      dataScope: menuAssignTemplate.value.dataScope,
      isAdmin: menuAssignTemplate.value.isAdmin === 1 ? 1 : 0,
      orderNum: menuAssignTemplate.value.orderNum,
      remark: menuAssignTemplate.value.remark,
      menuIds: menuCheckedKeys.value
    });
    menuAssignOpen.value = false;
    window.$message?.success?.('模板菜单已分配');
    await loadList();
  } finally {
    menuAssignSubmitting.value = false;
  }
}

function openRegionForm(record?: RowData) {
  if (!regionHqId.value) {
    window.$message?.warning?.('请先选择总部公司');
    return;
  }
  regionFormTitle.value = record ? '编辑大区' : '新增大区';
  Object.keys(regionForm).forEach(k => delete regionForm[k]);
  if (record) {
    Object.assign(regionForm, JSON.parse(JSON.stringify(record)));
  } else {
    regionForm.companyId = regionHqId.value;
  }
  regionFormOpen.value = true;
}

async function submitRegionForm() {
  const data = { ...regionForm };
  if (data.id) await updateRegion(data);
  else await addRegion(data);
  window.$message?.success?.('已保存');
  regionFormOpen.value = false;
  loadList();
}

async function openFaultDetail(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const { data } = await getFaultRepairConfig(id);
  faultDetail.value = normalizeFaultFormData((data as RowData) || record);
  faultDetailOpen.value = true;
}

function typeCodeLabel(code: string) {
  return typeCodeLabelMap.value[code] || code;
}

function getRoleTemplateDataScopeLabel(row: RowData) {
  const typeCode = String(row.typeCode || '');
  const dataScope = String(row.dataScope || '');
  if (!typeCode || !dataScope) return row.dataScope || '-';

  const options = roleTemplateDataScopeMap.value[typeCode] || [];
  const matched = options.find(option => option.value === dataScope);
  return matched?.label || dataScope;
}

function onRoleTemplateTypeChange() {
  pageQuery.pageNum = 1;
  loadList();
}

async function onRoleTemplateDataScopeInit(keepCurrentValue = false) {
  const typeCode = String(formModel.typeCode || '');
  if (!typeCode) {
    roleTemplateScopeOptions.value = [];
    formModel.dataScope = '';
    return;
  }
  try {
    const { data } = await roleTemplateDataScopeOptions(typeCode);
    const list = (Array.isArray(data) ? data : pickRows(data)) as RowData[];
    const options: DataScopeOption[] = list.map((item: RowData) => ({
      label: String(item.label ?? item.name ?? item.value),
      value: String(item.value ?? item.code ?? item.key),
      defaultOption: Boolean(item.defaultOption),
      disabled: Boolean(item.disabled)
    }));
    roleTemplateScopeOptions.value = options.map(item => ({ label: item.label, value: item.value }));
    const currentValue = String(formModel.dataScope || '');
    const currentValid = options.some(item => item.value === currentValue);
    if (!keepCurrentValue || !currentValid) {
      formModel.dataScope = options.find(item => item.defaultOption)?.value || options[0]?.value || '';
    }
  } catch {
    roleTemplateScopeOptions.value = [];
    if (!keepCurrentValue) formModel.dataScope = '';
  }
}

watch(regionHqId, () => {
  if (activeKey.value === 'region') loadList();
});

watch(
  () => route.name,
  name => {
    syncActiveModuleByRouteName(name);
  }
);

onMounted(() => {
  const changedByRoute = syncActiveModuleByRouteName(route.name);
  if (!changedByRoute) {
    loadList();
  }
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard :bordered="false" class="card-wrapper">
      <div class="flex flex-col gap-12px">
        <AForm v-if="activeKey === 'dict'" :model="dictQuery" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="字典名称" class="m-0">
                    <AInput v-model:value="dictQuery.dictName" allow-clear placeholder="请输入字典名称" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="字典类型" class="m-0">
                    <AInput v-model:value="dictQuery.dictType" allow-clear placeholder="请输入字典类型" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="dictQuery.status"
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
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'config'" :model="configQuery" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="参数名称" class="m-0">
                    <AInput v-model:value="configQuery.configName" allow-clear placeholder="请输入参数名称" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="参数键名" class="m-0">
                    <AInput v-model:value="configQuery.configKey" allow-clear placeholder="请输入参数键名" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="是否内置" class="m-0">
                    <ASelect
                      v-model:value="configQuery.configType"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '是', value: 1 },
                        { label: '否', value: 0 }
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'notifyTemplate'" :model="notifyQuery" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="模板编码" class="m-0">
                    <AInput v-model:value="notifyQuery.templateCode" allow-clear placeholder="请输入模板编码" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="模板名称" class="m-0">
                    <AInput v-model:value="notifyQuery.templateName" allow-clear placeholder="请输入模板名称" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="模板来源" class="m-0">
                    <ASelect
                      v-model:value="notifyQuery.templateSource"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '内置', value: 'BUILT_IN' },
                        { label: '自定义', value: 'CUSTOM' }
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'barcode'" :model="barcodeQuery" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="条码" class="m-0">
                    <AInput v-model:value="barcodeQuery.barcode" allow-clear placeholder="请输入条码" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="发货单号" class="m-0">
                    <AInput v-model:value="barcodeQuery.deliverNumber" allow-clear placeholder="请输入发货单号" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="物料编码" class="m-0">
                    <AInput v-model:value="barcodeQuery.productCode" allow-clear placeholder="请输入物料编码" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="机器小号" class="m-0">
                    <AInput v-model:value="barcodeQuery.machineNo" allow-clear placeholder="请输入机器小号" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="产品型号" class="m-0">
                    <AInput v-model:value="barcodeQuery.productModel" allow-clear placeholder="请输入产品型号" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="barcodeQuery.status"
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
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'syncTask'" :model="syncTaskQuery" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="任务编码" class="m-0">
                    <AInput v-model:value="syncTaskQuery.taskCode" allow-clear placeholder="请输入任务编码" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="任务名称" class="m-0">
                    <AInput v-model:value="syncTaskQuery.taskName" allow-clear placeholder="请输入任务名称" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="处理器" class="m-0">
                    <ASelect
                      v-model:value="syncTaskQuery.handlerCode"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="
                        handlerOptions.map((h: RowData) => ({
                          label: h.handlerName ?? h.label,
                          value: h.handlerCode ?? h.value
                        }))
                      "
                    />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="syncTaskQuery.status"
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
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'fault'" :model="faultQuery" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="归属总部" class="m-0">
                    <ASelect
                      v-model:value="faultQuery.companyId"
                      allow-clear
                      show-search
                      option-filter-prop="label"
                      placeholder="请选择总部"
                      class="w-full"
                      :options="
                        faultCompanyOptions.map((c: RowData) => ({
                          label: c.companyName ?? c.label,
                          value: c.id ?? c.value
                        }))
                      "
                    />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="物料编码" class="m-0">
                    <AInput v-model:value="faultQuery.productCode" allow-clear placeholder="请输入物料编码" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="产品型号" class="m-0">
                    <AInput v-model:value="faultQuery.productModel" allow-clear placeholder="请输入产品型号" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="故障描述" class="m-0">
                    <AInput v-model:value="faultQuery.faultDesc" allow-clear placeholder="请输入故障描述" />
                  </AFormItem>
                </ACol>
                <ACol :span="24" :md="12" :lg="6">
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="faultQuery.status"
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
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'roleTemplate'" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="8">
                  <AFormItem label="公司类型" class="m-0">
                    <ASelect
                      v-model:value="roleTemplateTypeCode"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="Object.entries(typeCodeLabelMap).map(([value, label]) => ({ value, label }))"
                      @change="onRoleTemplateTypeChange"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
          </div>
        </AForm>

        <AForm v-if="activeKey === 'region'" :label-col="{ span: 5, md: 7 }">
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol :span="24" :md="12" :lg="8">
                  <AFormItem label="总部公司" class="m-0">
                    <ASelect
                      v-model:value="regionHqId"
                      allow-clear
                      show-search
                      option-filter-prop="label"
                      placeholder="请选择总部"
                      class="w-full"
                      :options="hqCompanyOptions.map((c: RowData) => ({ label: c.companyName, value: c.id }))"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
          </div>
        </AForm>
      </div>
    </ACard>
    <ACard
      title="高级业务模块"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <ASpace wrap>
          <AButton v-if="activeKey === 'dict'" @click="onRefreshCache">刷新字典缓存</AButton>
          <AButton v-if="activeKey === 'config'" @click="onRefreshCache">刷新参数缓存</AButton>
          <AButton v-if="activeKey === 'notifyTemplate'" @click="onRefreshCache">刷新模板缓存</AButton>
          <AButton v-if="activeKey === 'barcode'" type="primary" ghost :loading="loading" @click="onFullSyncBarcode">
            执行同步任务
          </AButton>
          <AButton v-if="activeKey === 'syncTask'" type="primary" ghost @click="openSyncForm()">新增任务</AButton>
          <AButton
            v-if="hasDictConfigRowActions || activeKey === 'fault' || activeKey === 'roleTemplate'"
            type="primary"
            @click="openForm(undefined, '新增')"
          >
            新增
          </AButton>
          <AButton v-if="activeKey === 'region'" type="primary" :disabled="!regionHqId" @click="openRegionForm()">
            新增大区
          </AButton>
        </ASpace>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        class="h-full"
        :pagination="
          activeKey !== 'region'
            ? {
                current: pageQuery.pageNum,
                pageSize: pageQuery.pageSize,
                total,
                showSizeChanger: true,
                showTotal: (t: number) => `共 ${t} 条`,
                onChange: onPageChange
              }
            : false
        "
        :row-key="resolveRowKey"
        size="small"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'barcode' && activeKey === 'barcode'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openBarcodeDetail(record)"
              @keydown.enter.prevent="openBarcodeDetail(record)"
            >
              {{ record.barcode || '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'templateCode' && activeKey === 'notifyTemplate'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openNotifyView(record)"
              @keydown.enter.prevent="openNotifyView(record)"
            >
              {{ record.templateCode || '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'companyName' && activeKey === 'fault'">
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openFaultDetail(record)"
              @keydown.enter.prevent="openFaultDetail(record)"
            >
              {{ record.companyName || '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'status' && activeKey !== 'notifyTemplate'">
            <ATag :color="tagColorEnabled(Number(record.status) === 1)">
              {{ Number(record.status) === 1 ? '正常' : '停用' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'templateSource'">
            <ATag :color="record.templateSource === 'BUILT_IN' ? 'warning' : 'success'">
              {{ record.templateSource === 'BUILT_IN' ? '内置模板' : '自定义模板' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'notifyEnabled' || column.key === 'overrideEnabled'">
            <ATag :color="tagColorPositiveNeutral(Number(record[column.key]) === 1)">
              <template v-if="column.key === 'overrideEnabled'">
                {{ Number(record[column.key]) === 1 ? '覆盖' : '回退' }}
              </template>
              <template v-else>
                {{ Number(record[column.key]) === 1 ? '开启' : '关闭' }}
              </template>
            </ATag>
          </template>
          <template v-else-if="column.key === 'typeCode' && activeKey === 'roleTemplate'">
            {{ typeCodeLabel(record.typeCode) }}
          </template>
          <template v-else-if="column.key === 'dataScope' && activeKey === 'roleTemplate'">
            {{ getRoleTemplateDataScopeLabel(record) }}
          </template>
          <template v-else-if="column.key === 'isAdmin' && activeKey === 'roleTemplate'">
            <ATag v-if="record.isAdmin === 1" color="error">是</ATag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'actions' && activeKey === 'notifyTemplate'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--info" @click="onPreviewNotify(record)">
                预览
              </AButton>
              <AButton
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openChannelsEditor(record)"
              >
                渠道配置
              </AButton>
              <AButton
                v-if="record.templateSource === 'BUILT_IN' && !hasCustomNotifyTemplate(record.templateCode)"
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openNotifyAddCustom(record)"
              >
                新增自定义
              </AButton>
              <AButton
                v-if="record.templateSource === 'CUSTOM'"
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openNotifyEdit(record)"
              >
                编辑
              </AButton>
              <APopconfirm
                v-if="record.templateSource === 'CUSTOM'"
                :title="`确认删除自定义模板“${record.templateName || '-'}”吗？删除后会回退到内置模板。`"
                @confirm="removeRow(record)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeKey === 'syncTask'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openSyncFormEdit(record)">
                编辑
              </AButton>
              <APopconfirm
                :title="`确认立即执行任务“${record.taskName || record.taskCode || '-'}”吗？`"
                @confirm="onRunSyncTask(record)"
              >
                <AButton type="link" size="small" class="table-action-link--warning" :loading="loading">执行</AButton>
              </APopconfirm>
              <AButton type="link" size="small" class="table-action-link--info" @click="openLogDialog(record)">
                日志
              </AButton>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeKey === 'fault'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openForm(record, '编辑')">
                编辑
              </AButton>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeKey === 'roleTemplate'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openForm(record, '编辑')">
                编辑
              </AButton>
              <AButton
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openRoleTemplateMenuAssign(record)"
              >
                分配菜单
              </AButton>
              <AButton
                type="link"
                size="small"
                class="table-action-link--warning"
                @click="onSyncRoleTemplateRow(record)"
              >
                全量同步到公司
              </AButton>
              <APopconfirm title="确认删除？" @confirm="removeRow(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeKey === 'region'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openRegionForm(record)">
                编辑
              </AButton>
              <APopconfirm title="确认删除？" @confirm="removeRow(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && hasDictConfigRowActions">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openForm(record, '编辑')">
                编辑
              </AButton>
              <APopconfirm title="确认删除？" @confirm="removeRow(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer
      v-model:open="formOpen"
      :title="`${tabOptions.find(t => t.key === activeKey)?.label} — ${formTitle}`"
      :width="activeKey === 'fault' ? 600 : 420"
    >
      <AForm layout="vertical" class="mt-8px">
        <template v-if="activeKey === 'roleTemplate'">
          <AFormItem label="角色名称" required>
            <AInput v-model:value="formModel.roleName" placeholder="如：管理员" />
          </AFormItem>
          <AFormItem label="角色标识" required>
            <AInput v-model:value="formModel.roleKey" placeholder="如：admin" />
          </AFormItem>
          <AFormItem label="类型编码" required>
            <ASelect
              v-model:value="formModel.typeCode"
              show-search
              option-filter-prop="label"
              :options="Object.entries(typeCodeLabelMap).map(([value, label]) => ({ value, label }))"
              @change="() => onRoleTemplateDataScopeInit()"
            />
          </AFormItem>
          <AFormItem label="数据范围" required>
            <ASelect
              v-model:value="formModel.dataScope"
              :options="roleTemplateScopeOptions"
              placeholder="请选择数据范围"
            />
            <div class="mt-4px text-12px text-gray-500">
              说明：{{
                getRoleTemplateDataScopeLabel({ typeCode: formModel.typeCode, dataScope: formModel.dataScope })
              }}
            </div>
          </AFormItem>
          <AFormItem label="管理员模板">
            <ASwitch v-model:checked="formModel.isAdmin" :checked-value="1" :un-checked-value="0" />
            <div class="mt-4px text-12px text-gray-500">
              每种公司类型最多保留一个管理员模板，用于初始化公司管理员角色。
            </div>
          </AFormItem>
          <AFormItem label="备注">
            <ATextarea v-model:value="formModel.remark" :rows="3" />
          </AFormItem>
        </template>
        <template v-else-if="activeKey === 'fault'">
          <ARow :gutter="16">
            <ACol :span="12">
              <AFormItem label="归属总部" required>
                <ASelect
                  v-model:value="formModel.companyId"
                  show-search
                  option-filter-prop="label"
                  placeholder="请选择归属总部"
                  :options="
                    faultCompanyOptions.map((c: RowData) => ({ label: c.companyName ?? c.label, value: c.id ?? c.value }))
                  "
                />
              </AFormItem>
            </ACol>
            <ACol :span="12">
              <AFormItem label="状态" required>
                <ARadioGroup v-model:value="formModel.status">
                  <ARadio :value="1">启用</ARadio>
                  <ARadio :value="0">停用</ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
          </ARow>

          <ARow :gutter="16">
            <ACol :span="12">
              <AFormItem label="物料编码">
                <AInput v-model:value="formModel.productCode" placeholder="请输入物料编码" />
              </AFormItem>
            </ACol>
            <ACol :span="12">
              <AFormItem label="产品型号">
                <AInput v-model:value="formModel.productModel" placeholder="请输入产品型号" />
              </AFormItem>
            </ACol>
          </ARow>

          <ARow :gutter="16">
            <ACol :span="24">
              <AFormItem label="备注">
                <ATextarea v-model:value="formModel.remark" :rows="2" placeholder="请输入备注" />
              </AFormItem>
            </ACol>
          </ARow>

          <div class="mb-8px mt-4px flex items-center justify-between font-600">
            <span>故障信息</span>
            <AButton type="link" size="small" @click="addFaultItem">新增故障信息</AButton>
          </div>
          <div
            v-for="(item, index) in formModel.faults || []"
            :key="`fault-${index}`"
            class="mb-12px border border-gray-200 rounded-6px border-solid bg-gray-50 p-12px dark:bg-dark-900"
          >
            <div class="mb-8px flex items-center justify-between">
              <span class="font-500">故障 {{ Number(index) + 1 }}</span>
              <AButton
                v-if="(formModel.faults || []).length > 1"
                type="link"
                size="small"
                danger
                @click="removeFaultItem(Number(index))"
              >
                删除
              </AButton>
            </div>
            <AFormItem label="故障描述">
              <AInput v-model:value="item.faultDesc" placeholder="请输入故障描述" />
            </AFormItem>
            <div class="mb-8px flex items-center justify-between font-500">
              <span>维修说明</span>
              <AButton type="link" size="small" @click="addRepairOption(item)">新增维修说明</AButton>
            </div>
            <div
              v-for="(repairDesc, repairIndex) in item.repairOptions"
              :key="`repair-${index}-${repairIndex}`"
              class="mb-8px flex"
            >
              <AInput v-model:value="item.repairOptions[repairIndex]" placeholder="请输入维修说明" />
              <AButton
                v-if="item.repairOptions.length > 1"
                type="link"
                size="small"
                danger
                class="ml-8px"
                @click="removeRepairOption(item, Number(repairIndex))"
              >
                删除
              </AButton>
            </div>
          </div>
        </template>
        <template v-for="field in formFields()" v-else :key="field.key">
          <AFormItem :label="field.label" :required="isFormFieldRequired(field.key)">
            <ARadioGroup v-if="field.type === 'radio'" v-model:value="formModel[field.key]">
              <ARadio v-for="option in getFormRadioOptions(field.key)" :key="option.value" :value="option.value">
                {{ option.label }}
              </ARadio>
            </ARadioGroup>
            <AInputNumber v-else-if="field.type === 'number'" v-model:value="formModel[field.key]" class="w-full" />
            <AInput
              v-else-if="field.type === 'input'"
              v-model:value="formModel[field.key]"
              :placeholder="getFormPlaceholder(field.key, field.type)"
            />
            <ATextarea
              v-else
              v-model:value="formModel[field.key]"
              :rows="3"
              :placeholder="getFormPlaceholder(field.key, field.type)"
            />
          </AFormItem>
        </template>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="formOpen = false">取消</AButton>
          <AButton type="primary" @click="submitForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <AModal v-model:open="barcodeDetailOpen" title="条码档案详情" width="760px">
      <ADescriptions bordered size="small" :column="1">
        <ADescriptionsItem v-for="item in barcodeDetailRows" :key="item.key" :label="item.label">
          <div class="max-h-120px overflow-y-auto break-all">{{ item.value }}</div>
        </ADescriptionsItem>
      </ADescriptions>
      <template #footer>
        <AButton @click="barcodeDetailOpen = false">关闭</AButton>
      </template>
    </AModal>

    <ADrawer v-model:open="notifyFormOpen" :title="notifyFormTitle" :width="960">
      <AForm layout="vertical" class="mt-8px">
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="模板编码">
              <AInput v-model:value="notifyForm.templateCode" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="模板名称">
              <AInput v-model:value="notifyForm.templateName" :disabled="notifyFormReadonly" />
            </AFormItem>
          </ACol>
        </ARow>
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="通知开关">
              <ARadioGroup v-model:value="notifyForm.notifyEnabled" :disabled="notifyFormReadonly">
                <ARadio :value="1">开启</ARadio>
                <ARadio :value="0">关闭</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="覆盖开关">
              <ARadioGroup v-model:value="notifyForm.overrideEnabled" :disabled="notifyFormReadonly">
                <ARadio :value="1">启用覆盖</ARadio>
                <ARadio :value="0">回退内置</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
        </ARow>
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="路由类型">
              <ASelect
                v-model:value="notifyForm.routeType"
                :disabled="notifyFormReadonly"
                allow-clear
                placeholder="请选择路由类型"
                :options="notifyRouteTypeOptions"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="路由值模板">
              <AInput
                v-model:value="notifyForm.routeValueTemplate"
                :disabled="notifyFormReadonly"
                placeholder="例如 ${bizId}"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="标题模板">
              <AInput
                v-model:value="notifyForm.titleTemplate"
                :disabled="notifyFormReadonly"
                placeholder="留空则回退内置模板"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="摘要模板">
              <ATextarea
                v-model:value="notifyForm.summaryTemplate"
                :disabled="notifyFormReadonly"
                :rows="3"
                placeholder="留空则回退内置模板"
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="变量说明">
              <ATextarea :value="String(notifyForm.variablesJson || '')" :rows="4" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="备注">
              <ATextarea v-model:value="notifyForm.remark" :disabled="notifyFormReadonly" :rows="2" />
            </AFormItem>
          </ACol>
        </ARow>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="notifyFormOpen = false">取消</AButton>
          <AButton v-if="!notifyFormReadonly" @click="onPreviewNotifyForm">预览</AButton>
          <AButton v-if="!notifyFormReadonly" type="primary" :loading="notifyFormSubmitting" @click="submitNotifyForm">
            确定
          </AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="notifyPreviewOpen" title="模板预览" :width="720">
      <AForm :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <AFormItem label="示例变量 JSON">
          <ATextarea v-model:value="notifyPreviewVariablesText" :rows="8" />
        </AFormItem>
      </AForm>
      <div class="mb-12px">
        <AButton
          type="primary"
          :loading="notifyPreviewLoading"
          :disabled="!notifyPreviewPayload"
          @click="notifyPreviewPayload && runNotifyPreview(notifyPreviewPayload)"
        >
          执行预览
        </AButton>
      </div>
      <ADescriptions v-if="notifyPreviewResult" :column="1" bordered size="small">
        <ADescriptionsItem label="是否发送">{{ Number(notifyPreviewResult.notifyEnabled) === 1 ? '是' : '否' }}</ADescriptionsItem>
        <ADescriptionsItem label="实际来源">{{ notifyPreviewResult.templateSource || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="标题">{{ notifyPreviewResult.title || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="摘要">{{ notifyPreviewResult.summary || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="路由类型">{{ notifyPreviewResult.routeType || '-' }}</ADescriptionsItem>
        <ADescriptionsItem label="路由值">{{ notifyPreviewResult.routeValue || '-' }}</ADescriptionsItem>
      </ADescriptions>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="notifyPreviewOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="channelsOpen" :title="`渠道配置 - ${channelsTemplateCode || ''}`" :width="980">
      <ASpin :spinning="channelsLoading">
        <div class="mb-12px">
          <AButton v-if="!channelsReadonly" type="primary" ghost size="small" @click="addChannelRow">新增渠道</AButton>
        </div>
        <div
          v-for="(item, index) in channelsRows"
          :key="`channel-${index}`"
          class="mb-12px border border-gray-200 rounded-6px border-solid bg-gray-50 p-12px dark:bg-dark-900"
        >
          <div class="mb-8px flex items-center justify-between font-600">
            <span>渠道 {{ Number(index) + 1 }}</span>
            <AButton v-if="!channelsReadonly" type="link" size="small" danger @click="removeChannelRow(Number(index))">
              删除
            </AButton>
          </div>
          <ARow :gutter="16">
            <ACol :span="8">
              <AFormItem label="渠道类型" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" class="mb-8px">
                <ASelect
                  v-model:value="item.channelType"
                  :disabled="channelsReadonly"
                  :options="[
                    { label: '小程序订阅消息', value: 'MP_SUBSCRIBE' },
                    { label: '短信', value: 'SMS' },
                    { label: '邮件', value: 'EMAIL' }
                  ]"
                />
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem label="渠道开关" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" class="mb-8px">
                <ARadioGroup v-model:value="item.channelEnabled" :disabled="channelsReadonly">
                  <ARadio :value="1">开启</ARadio>
                  <ARadio :value="0">关闭</ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem label="小程序场景" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" class="mb-8px">
                <ASelect
                  v-model:value="item.channelScene"
                  :disabled="channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'"
                  :options="[
                    { label: 'B 端小程序', value: 'B' },
                    { label: 'C 端小程序', value: 'C' }
                  ]"
                />
              </AFormItem>
            </ACol>
            <ACol :span="10">
              <AFormItem label="模板 ID" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" class="mb-8px">
                <AInput
                  v-model:value="item.templateId"
                  :disabled="channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'"
                />
              </AFormItem>
            </ACol>
            <ACol :span="14">
              <AFormItem label="页面路径模板" :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }" class="mb-8px">
                <AInput
                  v-model:value="item.pagePathTemplate"
                  :disabled="channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'"
                  placeholder="例如 pages/order/evaluate?workOrderId=${workOrderId}"
                />
              </AFormItem>
            </ACol>
          </ARow>
          <div class="mb-8px">
            <ASpace>
              <span class="font-500">字段映射</span>
              <AButton
                v-if="!channelsReadonly && item.channelType === 'MP_SUBSCRIBE'"
                type="link"
                size="small"
                @click="addFieldMapping(item)"
              >
                新增字段
              </AButton>
            </ASpace>
          </div>
          <ATable
            :columns="[
              { title: '模板字段名', dataIndex: 'field', key: 'field' },
              { title: '变量表达式', dataIndex: 'value', key: 'value' },
              { title: '操作', key: 'actions', width: 90 }
            ]"
            :data-source="item.fieldMapping || []"
            :pagination="false"
            size="small"
            row-key="field"
          >
            <template #bodyCell="{ column, record: mapping, index: mappingIndex }">
              <template v-if="column.key === 'field'">
                <AInput
                  v-model:value="mapping.field"
                  :disabled="channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'"
                  placeholder="例如 thing1"
                />
              </template>
              <template v-else-if="column.key === 'value'">
                <AInput
                  v-model:value="mapping.value"
                  :disabled="channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'"
                  placeholder="例如 ${orderNo}"
                />
              </template>
              <template v-else-if="column.key === 'actions'">
                <AButton
                  v-if="!channelsReadonly && item.channelType === 'MP_SUBSCRIBE'"
                  type="link"
                  size="small"
                  danger
                  @click="removeFieldMapping(item, Number(mappingIndex))"
                >
                  删除
                </AButton>
              </template>
            </template>
          </ATable>
          <div class="mt-8px text-12px text-gray-500">
            可用展示变量：`orderNo`、`customerMobile`、`companyName`、`closedTime`。评价通知的页面路由变量仅支持
            `workOrderId`。
          </div>
          <AFormItem label="备注" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }" class="mb-0 mt-8px">
            <ATextarea v-model:value="item.remark" :disabled="channelsReadonly" :rows="2" />
          </AFormItem>
        </div>
      </ASpin>
      <template #footer>
        <ASpace>
          <AButton @click="channelsOpen = false">取消</AButton>
          <AButton v-if="!channelsReadonly" type="primary" :loading="channelsLoading" @click="saveChannels">
            保存
          </AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="syncFormOpen" :title="syncFormTitle" :width="420">
      <AForm layout="vertical" class="mt-8px">
        <AFormItem label="任务编码" required>
          <AInput v-model:value="syncFormModel.taskCode" :disabled="!!syncFormModel.id" placeholder="请输入任务编码" />
        </AFormItem>
        <AFormItem label="任务名称" required>
          <AInput v-model:value="syncFormModel.taskName" placeholder="请输入任务名称" />
        </AFormItem>
        <AFormItem label="处理器" required>
          <ASelect
            v-model:value="syncFormModel.handlerCode"
            placeholder="请选择处理器"
            class="w-full"
            :options="handlerOptions.map((h: RowData) => ({ label: h.handlerName, value: h.handlerCode }))"
          />
        </AFormItem>
        <AFormItem label="Cron 表达式" required>
          <AInput v-model:value="syncFormModel.cronExpression" placeholder="请输入Cron表达式，例如 0 0 2 * * ?" />
        </AFormItem>
        <AFormItem label="状态">
          <ARadioGroup v-model:value="syncFormModel.status">
            <ARadio :value="1">启用</ARadio>
            <ARadio :value="0">停用</ARadio>
          </ARadioGroup>
        </AFormItem>
        <AFormItem label="备注">
          <ATextarea v-model:value="syncFormModel.remark" :rows="2" placeholder="请输入备注" />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="syncFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitSyncForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="logOpen" :title="logDialogTitle" :width="1100">
      <AForm layout="inline" class="page-search-toolbar--inline mb-12px">
        <AFormItem label="状态">
          <ASelect
            v-model:value="logQuery.status"
            allow-clear
            class="min-w-160px"
            placeholder="全部"
            :options="[
              { label: 'RUNNING', value: 'RUNNING' },
              { label: 'SUCCESS', value: 'SUCCESS' },
              { label: 'FAILED', value: 'FAILED' }
            ]"
            @change="handleLogStatusChange"
          />
        </AFormItem>
      </AForm>
      <ATable
        :columns="[
          { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
          { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
          { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 170 },
          { title: '数据开始时间', dataIndex: 'dataStartTime', key: 'dataStartTime', width: 170 },
          { title: '数据结束时间', dataIndex: 'dataEndTime', key: 'dataEndTime', width: 170 },
          { title: '执行信息', dataIndex: 'message', key: 'message', ellipsis: true }
        ]"
        :data-source="logRows"
        :loading="logLoading"
        size="small"
        row-key="id"
        :scroll="{ y: 460 }"
        :pagination="{
          current: logPage.pageNum,
          pageSize: logPage.pageSize,
          total: logTotal,
          showSizeChanger: true,
          onChange: onLogPageChange
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <ATag :color="syncLogStatusTagColor(record.status)">{{ record.status || '-' }}</ATag>
          </template>
        </template>
      </ATable>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="logOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="menuAssignOpen" title="模板菜单分配" :width="680">
      <ATree
        v-model:checked-keys="menuCheckedKeys"
        checkable
        :tree-data="menuTreeData"
        :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
        class="max-h-420px overflow-auto"
      />
      <template #footer>
        <ASpace :size="16">
          <AButton @click="menuAssignOpen = false">取消</AButton>
          <AButton type="primary" :loading="menuAssignSubmitting" @click="submitRoleTemplateMenuAssign">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="regionFormOpen" :title="`大区 — ${regionFormTitle}`" :width="420">
      <AForm layout="vertical" class="mt-8px">
        <AFormItem label="大区编码" required>
          <AInput v-model:value="regionForm.regionCode" />
        </AFormItem>
        <AFormItem label="大区名称" required>
          <AInput v-model:value="regionForm.regionName" />
        </AFormItem>
        <AFormItem label="备注">
          <ATextarea v-model:value="regionForm.remark" :rows="2" />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="regionFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitRegionForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <AModal v-model:open="faultDetailOpen" title="查看故障与维修配置" width="860px">
      <template v-if="faultDetail">
        <ADescriptions :column="2" bordered size="small">
          <ADescriptionsItem label="归属总部">{{ faultDetail.companyName || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="状态">{{ Number(faultDetail.status) === 1 ? '启用' : '停用' }}</ADescriptionsItem>
          <ADescriptionsItem label="物料编码">{{ faultDetail.productCode || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="产品型号">{{ faultDetail.productModel || '-' }}</ADescriptionsItem>
          <ADescriptionsItem label="备注" :span="2">{{ faultDetail.remark || '-' }}</ADescriptionsItem>
        </ADescriptions>
        <div class="mb-8px mt-16px font-600">故障信息</div>
        <div
          v-for="(item, index) in faultDetail.faults || []"
          :key="`detail-fault-${index}`"
          class="mb-12px border border-gray-200 rounded-6px border-solid p-12px"
        >
          <div class="mb-8px font-600">{{ item.faultDesc || '-' }}</div>
          <ASpace wrap>
            <ATag
              v-for="(repairDesc, repairIndex) in item.repairOptions || []"
              :key="`detail-repair-${index}-${repairIndex}`"
            >
              {{ repairDesc }}
            </ATag>
          </ASpace>
        </div>
      </template>
      <template #footer>
        <AButton @click="faultDetailOpen = false">关闭</AButton>
      </template>
    </AModal>
  </div>
</template>
