<script setup lang="ts">
/**
 * 高级/运维配置聚合页：字典、参数、通知模板、角色模板、同步任务等多 Tab（对接 system 等接口）。
 */
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import type { FormInstance } from "ant-design-vue";
import { useRouteMenuTitle } from "@/hooks/common/route-menu-title";
import {
  adaptiveModalFormGridClass,
  adaptiveModalWidth,
} from "@/hooks/common/modal-form-layout";
import { computeExpandedKeysForCheckedMenuTree } from "@/utils/tree-expand-keys";
import { notifyOnceSuccessFromFlatResult } from "@/service/request/shared";
import {
  tagColorEnabled,
  tagColorPositiveNeutral,
} from "@/constants/list-status-tag";
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
  listMachineBarcodeHqOptions,
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
  updateSyncTask,
} from "@/service/api";
import { useAuth } from "@/hooks/business/auth";
import PageSearchExpandButton from "@/components/custom/page-search-expand-button.vue";
import { usePageSearchFilterCollapse } from "@/hooks/common/page-search-filter-collapse";
import { useTableScroll } from "@/hooks/common/table";
import { estimateAntTableActionColWidth } from "@/utils/table-action-width";
import {
  createAntTableListLocale,
  useListRequestTableMsgs,
} from "@/utils/list-table-empty-state";

type RowData = Record<string, any>;
type DataScopeOption = {
  value: string;
  label: string;
  defaultOption?: boolean;
  disabled?: boolean;
};
type ModuleKey =
  | "dict"
  | "config"
  | "notifyTemplate"
  | "barcode"
  | "syncTask"
  | "fault"
  | "roleTemplate"
  | "region";

const route = useRoute();
const pageMenuTitle = useRouteMenuTitle();
const { hasAuth } = useAuth();

const dictSearchFilter = usePageSearchFilterCollapse(3);
const configSearchFilter = usePageSearchFilterCollapse(3);
const notifySearchFilter = usePageSearchFilterCollapse(3);
const barcodeSearchFilter = usePageSearchFilterCollapse(7);
const syncTaskSearchFilter = usePageSearchFilterCollapse(4);
const faultSearchFilter = usePageSearchFilterCollapse(5);

// 主列表加载与 Tab
const loading = ref(false);
const activeKey = ref<ModuleKey>("dict");
const rows = ref<RowData[]>([]);
const total = ref(0);
const {
  listFetchErrorMsg,
  listEmptyBackendMsg,
  clearListMsgs,
  consumeFlatError,
  refreshEmptySuccessMsg,
  setMsgFromCatch,
} = useListRequestTableMsgs();
// 通用分页
const pageQuery = reactive({ pageNum: 1, pageSize: 10 });

/** 各子模块操作列宽度：按「当前表格该行最多操作按钮」文案横排估算 */
const ADV_MODULE_ACTION_COL_WIDTH = {
  dict: estimateAntTableActionColWidth(["编辑", "删除"]),
  config: estimateAntTableActionColWidth(["编辑", "删除"]),
  notifyTemplate: estimateAntTableActionColWidth([
    "预览",
    "渠道配置",
    "编辑",
    "删除",
  ]),
  syncTask: estimateAntTableActionColWidth(["编辑", "执行", "日志"]),
  fault: estimateAntTableActionColWidth(["编辑"]),
  roleTemplate: estimateAntTableActionColWidth([
    "编辑",
    "分配菜单",
    "全量同步到公司",
    "删除",
  ]),
  region: estimateAntTableActionColWidth(["编辑", "删除"]),
} as const;

/** 通知模板渠道弹窗内「字段映射」子表：单行仅「删除」 */
const NOTIFY_FIELD_MAPPING_ACTION_COL_W = estimateAntTableActionColWidth([
  "删除",
]);

/** 与当前 Tab 列宽之和匹配的最小 scroll.x，避免固定操作列被挤压、需横向拖动才能点全按钮 */
function advancedModuleTableMinScrollX(key: ModuleKey): number {
  const w = ADV_MODULE_ACTION_COL_WIDTH;
  switch (key) {
    case "dict":
      return 160 + 180 + 90 + 140 + w.dict;
    case "config":
      return 160 + 180 + 220 + 90 + 140 + w.config;
    case "notifyTemplate":
      return (
        200 + 180 + 110 + 100 + 100 + 160 + 140 + 140 + 170 + w.notifyTemplate
      );
    case "barcode":
      return 180 + 140 + 160 + 120 + 120 + 120 + 160 + 140 + 140 + 90 + 170;
    case "syncTask":
      return 160 + 180 + 140 + 160 + 90 + 100 + 170 + 170 + w.syncTask;
    case "fault":
      return 180 + 140 + 140 + 160 + 90 + 170 + w.fault;
    case "roleTemplate":
      return 180 + 160 + 140 + 90 + 140 + 160 + 170 + w.roleTemplate;
    case "region":
      return 140 + 200 + 120 + 170 + w.region;
    default:
      return 960;
  }
}

const tableScrollMinX = computed(() =>
  advancedModuleTableMinScrollX(activeKey.value),
);
const { tableWrapperRef, scrollConfig } = useTableScroll(tableScrollMinX);

const tableListLocale = createAntTableListLocale(
  listFetchErrorMsg,
  listEmptyBackendMsg,
  rows,
);

// 字典筛选
const dictQuery = reactive({
  dictName: "",
  dictType: "",
  status: undefined as number | undefined,
});

// 参数配置筛选
const configQuery = reactive({
  configName: "",
  configKey: "",
  configType: undefined as number | undefined,
});

// 通知模板筛选
const notifyQuery = reactive({
  templateCode: "",
  templateName: "",
  templateSource: undefined as string | undefined,
});

// 机器条码档案筛选
const barcodeQuery = reactive({
  ownerHqId: undefined as number | undefined,
  barcode: "",
  deliverNumber: "",
  productCode: "",
  machineNo: "",
  productModel: "",
  status: undefined as number | undefined,
});

// 同步任务筛选
const syncTaskQuery = reactive({
  taskCode: "",
  taskName: "",
  handlerCode: undefined as string | undefined,
  status: undefined as number | undefined,
});

// 故障维修配置筛选
const faultQuery = reactive({
  companyId: undefined as number | undefined,
  productCode: "",
  productModel: "",
  faultDesc: "",
  status: undefined as number | undefined,
});

// 角色模板：当前筛选的类型编码
const roleTemplateTypeCode = ref<string | undefined>(undefined);
// typeCode -> 类型名称
const typeCodeLabelMap = ref<Record<string, string>>({});

// 系统大区：当前总部
const regionHqId = ref<number | undefined>(undefined);
const hqCompanyOptions = ref<RowData[]>([]);

// 同步任务处理器下拉 / 故障配置归属公司下拉
const handlerOptions = ref<RowData[]>([]);
const faultCompanyOptions = ref<RowData[]>([]);
const barcodeHqOptions = ref<RowData[]>([]);

// 顶部 Tab 选项
const tabOptions = [
  { key: "dict" as const, label: "字典管理" },
  { key: "config" as const, label: "参数配置" },
  { key: "notifyTemplate" as const, label: "通知模板" },
  { key: "barcode" as const, label: "机器条码档案" },
  { key: "syncTask" as const, label: "同步任务" },
  { key: "fault" as const, label: "故障维修配置" },
  { key: "roleTemplate" as const, label: "角色模板" },
  { key: "region" as const, label: "系统大区" },
];

// 路由 name -> 子模块 key
const ROUTE_NAME_TO_MODULE_KEY: Record<string, ModuleKey> = {
  "system_role-template": "roleTemplate",
  system_config: "config",
  "system_dict-type": "dict",
  "system_dict-data": "dict",
  "system_notify-template": "notifyTemplate",
  "system_machine-barcode": "barcode",
  "system_sync-task": "syncTask",
  "system_fault-repair-config": "fault",
  system_region: "region",
};

// 字典/参数配置等通用表单抽屉
const formOpen = ref(false);
const unifiedFormRef = ref<FormInstance | null>(null);
const formModel = reactive<RowData>({});
const formTitle = ref("");

// 条码档案详情抽屉
const barcodeDetailOpen = ref(false);
const barcodeDetail = ref<RowData | null>(null);
// 条码详情描述列表（computed）
const barcodeDetailRows = computed(() => {
  const detail = barcodeDetail.value || {};
  const statusValue = Number(detail.status);
  const statusLabel =
    statusValue === 1 ? "启用" : statusValue === 0 ? "停用" : "-";
  return [
    { key: "barcode", label: "条码", value: detail.barcode || "-" },
    {
      key: "deliverNumber",
      label: "发货单号",
      value: detail.deliverNumber || "-",
    },
    {
      key: "hqCompanyName",
      label: "归属总部",
      value: detail.hqCompanyName || "-",
    },
    { key: "custId", label: "CRM公司ID", value: detail.custId || "-" },
    { key: "salesOrg", label: "销售组织", value: detail.salesOrg || "-" },
    { key: "productCode", label: "物料编码", value: detail.productCode || "-" },
    { key: "productName", label: "商品名称", value: detail.productName || "-" },
    {
      key: "productModel",
      label: "产品型号",
      value: detail.productModel || "-",
    },
    { key: "machineNo", label: "机器小号", value: detail.machineNo || "-" },
    { key: "scanDate", label: "条码扫码时间", value: detail.scanDate || "-" },
    {
      key: "lastOutDate",
      label: "最后出库日期",
      value: detail.lastOutDate || "-",
    },
    {
      key: "crmAddTime",
      label: "CRM创建时间",
      value: detail.crmAddTime || "-",
    },
    {
      key: "lastSyncTime",
      label: "最近同步时间",
      value: detail.lastSyncTime || "-",
    },
    {
      key: "warrantyStatus",
      label: "质保状态",
      value: detail.warrantyStatus || "-",
    },
    { key: "status", label: "状态", value: statusLabel },
    { key: "remark", label: "备注", value: detail.remark || "-" },
  ];
});

// 通知模板：列表查看抽屉与关联行
const notifyViewOpen = ref(false);
// 通知模板：抽屉表单与预览弹窗状态
const notifyFormOpen = ref(false);
const notifyFormTitle = ref("");
const notifyFormReadonly = ref(false);
const notifyFormSubmitting = ref(false);
const notifyFormRef = ref<FormInstance | null>(null);
const notifyForm = reactive<RowData>({});
const notifyPreviewOpen = ref(false);
const notifyPreviewLoading = ref(false);
const notifyPreviewVariablesText = ref("");
const notifyPreviewResult = ref<RowData | null>(null);
const notifyPreviewPayload = ref<RowData | null>(null);
const notifyRouteTypeOptions = [
  { label: "工单详情", value: "WORK_ORDER_DETAIL" },
  { label: "工单评价", value: "WORK_ORDER_EVALUATE" },
];

// 通知渠道配置抽屉
const channelsOpen = ref(false);
const channelsLoading = ref(false);
const channelsReadonly = ref(false);
const channelsTemplateCode = ref("");
const channelsRows = ref<RowData[]>([]);

// 同步任务编辑抽屉
const syncFormOpen = ref(false);
const syncFormTitle = ref("");
const syncTaskFormRef = ref<FormInstance | null>(null);
const syncFormModel = reactive<RowData>({});

// 同步任务执行日志弹窗
const logOpen = ref(false);
const logDialogTitle = ref("执行日志");
const logTaskId = ref<number | string | undefined>(undefined);
const logRows = ref<RowData[]>([]);
const logTotal = ref(0);
const logPage = reactive({ pageNum: 1, pageSize: 10 });
const logLoading = ref(false);
const logQuery = reactive({
  status: undefined as string | undefined,
});

// 角色模板数据范围映射（按 typeCode，供表格列展示标签）
const roleTemplateDataScopeMap = ref<Record<string, DataScopeOption[]>>({});
// 角色模板表单：当前 typeCode 对应的数据范围下拉
const roleTemplateScopeOptions = ref<Array<{ label: string; value: string }>>(
  [],
);
// 角色模板菜单分配
const menuAssignOpen = ref(false);
const menuAssignSubmitting = ref(false);
const menuAssignTypeCode = ref("");
const menuAssignTemplate = ref<RowData | null>(null);
const menuTreeData = ref<any[]>([]);
const menuCheckedKeys = ref<Array<string | number>>([]);
const menuExpandedKeys = ref<Array<string | number>>([]);

// 系统大区表单抽屉
const regionFormOpen = ref(false);
const regionFormTitle = ref("");
const regionForm = reactive<RowData>({});
const advRegionFormRef = ref<FormInstance | null>(null);

// 故障配置只读详情弹窗
const faultDetailOpen = ref(false);
const faultDetail = ref<RowData | null>(null);
type FaultRepairItem = {
  faultDesc: string;
  repairOptions: string[];
};

function buildFaultDetailParams(record?: RowData | null) {
  const ownerHqId = Number(record?.companyId ?? faultQuery.companyId);
  return Number.isFinite(ownerHqId) ? { ownerHqId } : undefined;
}

/**
 * 作用：解析分页列表字段 records。
 * @param data - 接口数据
 * @returns 行数组
 */
function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

/**
 * 作用：解析分页 total。
 * @param data - 接口数据
 * @returns 总条数
 */
function pickTotal(data: any) {
  return Number(data?.total) || 0;
}

/**
 * 作用：当前列表请求的统一分页参数。
 * @returns pageNum/pageSize
 */
function listParams() {
  return { pageNum: pageQuery.pageNum, pageSize: pageQuery.pageSize };
}

/**
 * 作用：懒加载同步任务处理器选项。
 * @returns 无
 */
async function ensureHandlerOptions() {
  if (handlerOptions.value.length) return;
  try {
    const { data } = await listSyncTaskHandlerOptions();
    handlerOptions.value = Array.isArray(data) ? data : pickRows(data);
  } catch {
    handlerOptions.value = [];
  }
}

/**
 * 作用：懒加载故障配置可选归属公司列表。
 * @returns 无
 */
async function ensureFaultCompanyOptions() {
  if (faultCompanyOptions.value.length) return;
  try {
    const { data } = await listFaultRepairConfigCompanyOptions();
    faultCompanyOptions.value = Array.isArray(data) ? data : pickRows(data);
  } catch {
    faultCompanyOptions.value = [];
  }
}

/**
 * 作用：故障维修配置在未选归属总部时默认选中选项第一项（**仅路由进入该子模块时调用**；用户清空归属总部后点查询不再回填，以按「未选总部」条件请求列表）。
 * @returns 无
 */
function applyDefaultFaultRepairCompanyFilter() {
  if (faultQuery.companyId != null) return;
  const list = faultCompanyOptions.value;
  if (!list.length) return;
  faultQuery.companyId = Number(list[0].id ?? list[0].value);
}

/**
 * 作用：懒加载条码档案可维护总部下拉数据（仅拉选项，不改动当前筛选值）。
 */
async function ensureBarcodeHqOptions() {
  if (!barcodeHqOptions.value.length) {
    const { data } = await listMachineBarcodeHqOptions();
    barcodeHqOptions.value = Array.isArray(data) ? data : pickRows(data);
  }
}

/**
 * 作用：条码档案筛选项「归属总部」未选时默认第一项（**仅路由进入该子模块时调用**；用户清空后点查询不再回填）。
 */
function applyDefaultBarcodeOwnerHqFilter() {
  if (barcodeQuery.ownerHqId != null) return;
  const list = barcodeHqOptions.value;
  if (!list.length) return;
  barcodeQuery.ownerHqId = Number(list[0].id ?? list[0].value);
}

/**
 * 作用：加载总部列表供「系统大区」筛选；默认选中第一项。
 * @returns 无
 */
async function loadHqForRegion() {
  const { data } = await listCompany({
    pageNum: 1,
    pageSize: 999,
    category: "HQ",
  });
  const list = pickRows(data);
  hqCompanyOptions.value = list;
  if (!regionHqId.value && list.length) {
    regionHqId.value = list[0].id;
  }
}

/**
 * 作用：加载公司类型标签映射与角色模板数据范围选项映射。
 * @returns 无
 */
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
    roleTemplateDataScopeMap.value = ((data as unknown as Record<
      string,
      DataScopeOption[]
    >) || {}) as Record<string, DataScopeOption[]>;
  } catch {
    roleTemplateDataScopeMap.value = {};
  }
}

/**
 * 作用：按当前子模块构造列表请求 Promise。
 * @returns 接口 Promise
 */
function loadByModule() {
  const p = listParams();
  switch (activeKey.value) {
    case "dict":
      return listDictType({
        ...p,
        dictName: dictQuery.dictName || undefined,
        dictType: dictQuery.dictType || undefined,
        status: dictQuery.status,
      });
    case "config":
      return listSystemConfig({
        ...p,
        configName: configQuery.configName || undefined,
        configKey: configQuery.configKey || undefined,
        configType: configQuery.configType,
      });
    case "notifyTemplate":
      return listNotifyTemplate({
        ...p,
        templateCode: notifyQuery.templateCode || undefined,
        templateName: notifyQuery.templateName || undefined,
        templateSource: notifyQuery.templateSource,
      });
    case "barcode":
      return listMachineBarcode({
        ...p,
        ownerHqId: barcodeQuery.ownerHqId,
        barcode: barcodeQuery.barcode || undefined,
        deliverNumber: barcodeQuery.deliverNumber || undefined,
        productCode: barcodeQuery.productCode || undefined,
        machineNo: barcodeQuery.machineNo || undefined,
        productModel: barcodeQuery.productModel || undefined,
        status: barcodeQuery.status,
      });
    case "syncTask":
      return listSyncTask({
        ...p,
        taskCode: syncTaskQuery.taskCode || undefined,
        taskName: syncTaskQuery.taskName || undefined,
        handlerCode: syncTaskQuery.handlerCode,
        status: syncTaskQuery.status,
      });
    case "fault":
      return listFaultRepairConfig({
        ...p,
        companyId: faultQuery.companyId,
        productCode: faultQuery.productCode || undefined,
        productModel: faultQuery.productModel || undefined,
        faultDesc: faultQuery.faultDesc || undefined,
        status: faultQuery.status,
      });
    case "roleTemplate":
      return listRoleTemplate(roleTemplateTypeCode.value || undefined, p);
    case "region":
      if (regionHqId.value == null) {
        return Promise.resolve({ data: [] });
      }
      return listRegion(regionHqId.value);
    default:
      return listDictType(p);
  }
}

// 当前子模块表格列定义
const columns = computed(() => {
  const aw = ADV_MODULE_ACTION_COL_WIDTH;
  const actionCol = (width: number) => ({
    title: "操作",
    key: "actions",
    width,
    fixed: "right" as const,
  });
  switch (activeKey.value) {
    case "dict":
      return [
        {
          title: "字典名称",
          dataIndex: "dictName",
          key: "dictName",
          width: 160,
        },
        {
          title: "字典类型",
          dataIndex: "dictType",
          key: "dictType",
          width: 180,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 90 },
        { title: "备注", dataIndex: "remark", key: "remark", ellipsis: true },
        actionCol(aw.dict),
      ];
    case "config":
      return [
        {
          title: "参数名称",
          dataIndex: "configName",
          key: "configName",
          width: 160,
        },
        {
          title: "参数键",
          dataIndex: "configKey",
          key: "configKey",
          width: 180,
        },
        {
          title: "参数值",
          dataIndex: "configValue",
          key: "configValue",
          ellipsis: true,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 90 },
        { title: "备注", dataIndex: "remark", key: "remark", ellipsis: true },
        actionCol(aw.config),
      ];
    case "notifyTemplate":
      return [
        {
          title: "模板编码",
          dataIndex: "templateCode",
          key: "templateCode",
          width: 200,
        },
        {
          title: "模板名称",
          dataIndex: "templateName",
          key: "templateName",
          width: 180,
        },
        {
          title: "模板来源",
          dataIndex: "templateSource",
          key: "templateSource",
          width: 110,
        },
        {
          title: "通知开关",
          dataIndex: "notifyEnabled",
          key: "notifyEnabled",
          width: 100,
        },
        {
          title: "覆盖开关",
          dataIndex: "overrideEnabled",
          key: "overrideEnabled",
          width: 100,
        },
        {
          title: "路由类型",
          dataIndex: "routeType",
          key: "routeType",
          width: 160,
        },
        {
          title: "标题模板",
          dataIndex: "titleTemplate",
          key: "titleTemplate",
          ellipsis: true,
        },
        {
          title: "摘要模板",
          dataIndex: "summaryTemplate",
          key: "summaryTemplate",
          ellipsis: true,
        },
        {
          title: "更新时间",
          dataIndex: "updateTime",
          key: "updateTime",
          width: 170,
        },
        actionCol(aw.notifyTemplate),
      ];
    case "barcode":
      return [
        { title: "条码", dataIndex: "barcode", key: "barcode", width: 180 },
        {
          title: "发货单号",
          dataIndex: "deliverNumber",
          key: "deliverNumber",
          width: 140,
        },
        {
          title: "归属总部",
          dataIndex: "hqCompanyName",
          key: "hqCompanyName",
          width: 160,
        },
        { title: "CRM公司ID", dataIndex: "custId", key: "custId", width: 120 },
        {
          title: "销售组织",
          dataIndex: "salesOrg",
          key: "salesOrg",
          width: 120,
        },
        {
          title: "物料编码",
          dataIndex: "productCode",
          key: "productCode",
          width: 120,
        },
        {
          title: "商品名称",
          dataIndex: "productName",
          key: "productName",
          width: 160,
          ellipsis: true,
        },
        {
          title: "产品型号",
          dataIndex: "productModel",
          key: "productModel",
          width: 140,
        },
        {
          title: "机器小号",
          dataIndex: "machineNo",
          key: "machineNo",
          width: 140,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 90 },
        {
          title: "更新时间",
          dataIndex: "updateTime",
          key: "updateTime",
          width: 170,
        },
      ];
    case "syncTask":
      return [
        {
          title: "任务编码",
          dataIndex: "taskCode",
          key: "taskCode",
          width: 160,
        },
        {
          title: "任务名称",
          dataIndex: "taskName",
          key: "taskName",
          width: 180,
        },
        {
          title: "处理器",
          dataIndex: "handlerName",
          key: "handlerName",
          width: 140,
        },
        {
          title: "Cron",
          dataIndex: "cronExpression",
          key: "cronExpression",
          width: 160,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 90 },
        {
          title: "最近状态",
          dataIndex: "lastStatus",
          key: "lastStatus",
          width: 100,
        },
        {
          title: "最近结束",
          dataIndex: "lastEndTime",
          key: "lastEndTime",
          width: 170,
        },
        {
          title: "下次触发",
          dataIndex: "nextFireTime",
          key: "nextFireTime",
          width: 170,
        },
        actionCol(aw.syncTask),
      ];
    case "fault":
      return [
        {
          title: "归属总部",
          dataIndex: "companyName",
          key: "companyName",
          width: 180,
        },
        {
          title: "物料编码",
          dataIndex: "productCode",
          key: "productCode",
          width: 140,
        },
        {
          title: "产品型号",
          dataIndex: "productModel",
          key: "productModel",
          width: 140,
        },
        {
          title: "故障摘要",
          dataIndex: "faultDescSummary",
          key: "faultDescSummary",
          ellipsis: true,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 90 },
        {
          title: "更新时间",
          dataIndex: "updateTime",
          key: "updateTime",
          width: 170,
        },
        actionCol(aw.fault),
      ];
    case "roleTemplate":
      return [
        {
          title: "角色名称",
          dataIndex: "roleName",
          key: "roleName",
          width: 180,
        },
        { title: "角色标识", dataIndex: "roleKey", key: "roleKey", width: 160 },
        {
          title: "所属类型",
          dataIndex: "typeCode",
          key: "typeCode",
          width: 140,
        },
        { title: "管理员", dataIndex: "isAdmin", key: "isAdmin", width: 90 },
        {
          title: "数据范围",
          dataIndex: "dataScope",
          key: "dataScope",
          width: 140,
        },
        { title: "备注", dataIndex: "remark", key: "remark", ellipsis: true },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 170,
        },
        actionCol(aw.roleTemplate),
      ];
    case "region":
      return [
        {
          title: "大区编码",
          dataIndex: "regionCode",
          key: "regionCode",
          width: 140,
        },
        {
          title: "大区名称",
          dataIndex: "regionName",
          key: "regionName",
          width: 200,
        },
        { title: "备注", dataIndex: "remark", key: "remark", ellipsis: true },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 170,
        },
        actionCol(aw.region),
      ];
    default:
      return [];
  }
});

// 字典/参数模块是否展示行内编辑删除
const hasDictConfigRowActions = computed(
  () => activeKey.value === "dict" || activeKey.value === "config",
);

/**
 * 作用：为表格行生成稳定 key。
 * @param r - 行数据
 * @returns 唯一键字符串
 */
function resolveRowKey(r: RowData) {
  return (
    r.id ??
    r.dictId ??
    r.configId ??
    r.templateCode ??
    String(r.taskCode ?? r.barcode ?? Math.random())
  );
}

/**
 * 作用：拉取当前子模块列表数据并写入 rows/total。
 * @returns 无
 */
async function loadList() {
  loading.value = true;
  clearListMsgs();
  try {
    if (activeKey.value === "region") {
      await loadHqForRegion();
    }
    if (activeKey.value === "syncTask") {
      await ensureHandlerOptions();
    }
    if (activeKey.value === "fault") {
      await ensureFaultCompanyOptions();
    }
    if (activeKey.value === "barcode") {
      await ensureBarcodeHqOptions();
    }
    if (activeKey.value === "roleTemplate") {
      await loadTypeCodeLabels();
    }

    const flat = await loadByModule();
    if (consumeFlatError(flat)) {
      rows.value = [];
      total.value = 0;
      return;
    }
    const data =
      flat != null && typeof flat === "object" && "data" in flat
        ? (flat as { data?: unknown }).data
        : flat;
    rows.value = pickRows(data);
    if (activeKey.value === "region") {
      total.value = rows.value.length;
    } else {
      total.value = pickTotal(data) || rows.value.length;
    }
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
 * 作用：根据路由 name 切换 Tab 并触发列表加载。
 * @param routeName - 当前路由 name
 * @returns 是否发生了 Tab 切换
 */
async function syncActiveModuleByRouteName(routeName: unknown): Promise<boolean> {
  const key = String(routeName || "");
  const moduleKey = ROUTE_NAME_TO_MODULE_KEY[key];
  if (!moduleKey || moduleKey === activeKey.value) return false;

  activeKey.value = moduleKey;
  pageQuery.pageNum = 1;
  formOpen.value = false;
  syncFormOpen.value = false;
  if (moduleKey === "fault") {
    await ensureFaultCompanyOptions();
    applyDefaultFaultRepairCompanyFilter();
  }
  if (moduleKey === "barcode") {
    await ensureBarcodeHqOptions();
    applyDefaultBarcodeOwnerHqFilter();
  }
  await loadList();
  return true;
}

/**
 * 作用：查询：重置到第一页并重新加载列表。
 * @returns 无
 */
function handleSearch() {
  pageQuery.pageNum = 1;
  loadList();
}

/**
 * 作用：清空当前子模块筛选条件并重新查询。
 * @returns 无
 */
function resetSearch() {
  switch (activeKey.value) {
    case "dict":
      dictQuery.dictName = "";
      dictQuery.dictType = "";
      dictQuery.status = undefined;
      break;
    case "config":
      configQuery.configName = "";
      configQuery.configKey = "";
      configQuery.configType = undefined;
      break;
    case "notifyTemplate":
      notifyQuery.templateCode = "";
      notifyQuery.templateName = "";
      notifyQuery.templateSource = undefined;
      break;
    case "barcode":
      barcodeQuery.ownerHqId = undefined;
      barcodeQuery.barcode = "";
      barcodeQuery.deliverNumber = "";
      barcodeQuery.productCode = "";
      barcodeQuery.machineNo = "";
      barcodeQuery.productModel = "";
      barcodeQuery.status = undefined;
      break;
    case "syncTask":
      syncTaskQuery.taskCode = "";
      syncTaskQuery.taskName = "";
      syncTaskQuery.handlerCode = undefined;
      syncTaskQuery.status = undefined;
      break;
    case "fault":
      faultQuery.companyId = undefined;
      faultQuery.productCode = "";
      faultQuery.productModel = "";
      faultQuery.faultDesc = "";
      faultQuery.status = undefined;
      break;
    case "roleTemplate":
      roleTemplateTypeCode.value = undefined;
      break;
    case "region":
      regionHqId.value = undefined;
      break;
    default:
      break;
  }

  handleSearch();
}

/**
 * 作用：表格分页变更。
 * @param page - 页码
 * @param pageSize - 每页条数（可选）
 * @returns 无
 */
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

/**
 * 作用：打开通用表单抽屉并按子模块初始化表单模型。
 * @param record - 编辑时的行数据，新增时省略
 * @param title - 抽屉标题，可选
 */
async function openForm(record?: RowData, title?: string) {
  if (activeKey.value === "roleTemplate") {
    formTitle.value = title || (record?.id ? "编辑模板" : "新增模板");
  } else {
    formTitle.value = title || (record?.id ? "编辑" : "新增");
  }
  Object.keys(formModel).forEach((k) => delete formModel[k]);
  if (record) Object.assign(formModel, JSON.parse(JSON.stringify(record)));
  if (activeKey.value === "dict" && !record) {
    formModel.status = 1;
  }
  if (activeKey.value === "config" && !record) {
    formModel.configType = 0;
  }
  if (activeKey.value === "config" && record) {
    if (
      formModel.configType === undefined ||
      formModel.configType === null ||
      formModel.configType === ""
    ) {
      formModel.configType = formModel.status;
    }
  }
  if (activeKey.value === "fault") {
    if (record?.id != null) {
      try {
        const { data } = await getFaultRepairConfig(
          record.id,
          buildFaultDetailParams(record),
        );
        Object.assign(
          formModel,
          normalizeFaultFormData((data as RowData) || record),
        );
      } catch {
        Object.assign(formModel, normalizeFaultFormData(record));
      }
    } else {
      const defaultCompanyId =
        faultCompanyOptions.value.length === 1
          ? Number(
              faultCompanyOptions.value[0].id ??
                faultCompanyOptions.value[0].value,
            )
          : undefined;
      Object.assign(
        formModel,
        normalizeFaultFormData({ companyId: defaultCompanyId, status: 1 }),
      );
    }
  }

  if (activeKey.value === "roleTemplate") {
    const defaultTypeCode = String(
      roleTemplateTypeCode.value ||
        Object.keys(typeCodeLabelMap.value)[0] ||
        "",
    );
    if (!record) {
      formModel.roleName = "";
      formModel.roleKey = "";
      formModel.remark = "";
      formModel.isAdmin = 0;
      formModel.typeCode = defaultTypeCode;
      formModel.dataScope = "";
    }
    formModel.isAdmin = Number(formModel.isAdmin) === 1 ? 1 : 0;
    await onRoleTemplateDataScopeInit(Boolean(record));
  }
  formOpen.value = true;
}

/**
 * 作用：判断字典/参数表单字段是否必填。
 * @param key - 字段名
 * @returns 是否必填
 */
function isFormFieldRequired(key: string) {
  if (activeKey.value === "dict") {
    return key === "dictName" || key === "dictType" || key === "status";
  }
  if (activeKey.value === "config") {
    return (
      key === "configName" ||
      key === "configKey" ||
      key === "configValue" ||
      key === "configType"
    );
  }
  if (activeKey.value === "notifyTemplate") {
    return (
      key === "templateCode" ||
      key === "templateName" ||
      key === "notifyEnabled" ||
      key === "overrideEnabled"
    );
  }
  return false;
}

/**
 * 作用：返回当前子模块抽屉内表单字段定义。
 * @returns 字段列表
 */
function formFields(): {
  label: string;
  key: string;
  type: "input" | "textarea" | "number" | "radio";
}[] {
  switch (activeKey.value) {
    case "dict":
      return [
        { label: "字典名称", key: "dictName", type: "input" },
        { label: "字典类型", key: "dictType", type: "input" },
        { label: "状态", key: "status", type: "radio" },
        { label: "备注", key: "remark", type: "textarea" },
      ];
    case "config":
      return [
        { label: "参数名称", key: "configName", type: "input" },
        { label: "参数键名", key: "configKey", type: "input" },
        { label: "参数键值", key: "configValue", type: "textarea" },
        { label: "是否内置", key: "configType", type: "radio" },
        { label: "备注", key: "remark", type: "textarea" },
      ];
    case "notifyTemplate":
      return [
        { label: "模板编码", key: "templateCode", type: "input" },
        { label: "模板名称", key: "templateName", type: "input" },
        { label: "通知开关(1/0)", key: "notifyEnabled", type: "number" },
        { label: "覆盖开关(1/0)", key: "overrideEnabled", type: "number" },
        { label: "路由类型", key: "routeType", type: "input" },
        { label: "标题模板", key: "titleTemplate", type: "textarea" },
        { label: "摘要模板", key: "summaryTemplate", type: "textarea" },
        { label: "路由值模板", key: "routeValueTemplate", type: "input" },
      ];
    case "fault":
      return [
        { label: "归属总部公司ID", key: "companyId", type: "number" },
        { label: "物料编码", key: "productCode", type: "input" },
        { label: "产品型号", key: "productModel", type: "input" },
        { label: "状态", key: "status", type: "number" },
        { label: "备注", key: "remark", type: "textarea" },
      ];
    case "roleTemplate":
      return [
        { label: "角色名称", key: "roleName", type: "input" },
        { label: "角色标识", key: "roleKey", type: "input" },
        { label: "类型编码 typeCode", key: "typeCode", type: "input" },
        { label: "数据范围 dataScope", key: "dataScope", type: "input" },
        { label: "备注", key: "remark", type: "textarea" },
      ];
    default:
      return [];
  }
}

/** 通知模板编辑抽屉内 AFormItem 数量（与模板结构一致，用于多于 6 项即 ≥7 时宽度至少 720） */
const NOTIFY_TEMPLATE_FORM_FIELD_COUNT = 10;

const unifiedFormDrawerBaseWidth = computed(() =>
  activeKey.value === "fault" ? 600 : 420,
);

const unifiedFormFieldCount = computed(() => {
  const key = activeKey.value;
  if (key === "roleTemplate") {
    return 6;
  }
  if (key === "fault") {
    const base = 5;
    let extra = 0;
    for (const f of formModel.faults || []) {
      extra += 1 + (f.repairOptions?.length || 0);
    }
    return base + extra;
  }
  return formFields().length;
});

const unifiedFormDrawerWidth = computed(() =>
  adaptiveModalWidth(
    unifiedFormDrawerBaseWidth.value,
    unifiedFormFieldCount.value,
  ),
);

/** 故障配置抽屉已用 ARow 两列，不再套 grid，仅宽度自适应 */
const unifiedFormGridClass = computed(() =>
  activeKey.value === "fault"
    ? ""
    : adaptiveModalFormGridClass(unifiedFormFieldCount.value),
);

const notifyFormDrawerWidth = computed(() =>
  adaptiveModalWidth(960, NOTIFY_TEMPLATE_FORM_FIELD_COUNT),
);

const channelsDrawerFieldCount = computed(
  () => (channelsRows.value?.length || 0) * 6,
);
const channelsDrawerWidth = computed(() =>
  adaptiveModalWidth(980, channelsDrawerFieldCount.value),
);

/**
 * 作用：字典/参数.radio 类型字段的选项。
 * @param key - 字段名
 * @returns 选项列表
 */
function getFormRadioOptions(key: string) {
  if (key === "status") {
    return [
      { label: "启用", value: 1 },
      { label: "停用", value: 0 },
    ];
  }
  if (key === "configType") {
    return [
      { label: "是", value: 1 },
      { label: "否", value: 0 },
    ];
  }
  return [];
}

/**
 * 作用：表单控件占位提示文案。
 * @param key - 字段名
 * @param type - 控件类型
 * @returns 占位符或 undefined
 */
function getFormPlaceholder(
  key: string,
  type: "input" | "textarea" | "number" | "radio",
) {
  if (activeKey.value === "dict") {
    if (key === "dictName") return "请输入字典名称";
    if (key === "dictType") return "如 sys_yes_no";
    if (key === "remark" && type === "textarea") return "请输入备注";
  }
  if (activeKey.value === "config") {
    if (key === "configName") return "请输入参数名称";
    if (key === "configKey") return "请输入参数键名";
    if (key === "configValue" && type === "textarea") return "请输入参数键值";
    if (key === "remark" && type === "textarea") return "请输入备注";
  }
  return undefined;
}

/**
 * 作用：新建一条空的故障-维修项。
 * @returns 故障项结构
 */
function createFaultItem(): FaultRepairItem {
  return {
    faultDesc: "",
    repairOptions: [""],
  };
}

/**
 * 作用：将接口/行数据规整为故障维修表单可用的结构。
 * @param data - 原始数据，可选
 * @returns 表单模型片段
 */
function normalizeFaultFormData(data?: RowData) {
  const faultsRaw = Array.isArray(data?.faults) ? data.faults : [];
  const faults: FaultRepairItem[] = (
    faultsRaw.length ? faultsRaw : [createFaultItem()]
  ).map((item: RowData) => {
    const repairOptions = Array.isArray(item?.repairOptions)
      ? item.repairOptions
          .map((x: unknown) => String(x ?? "").trim())
          .filter(Boolean)
      : [];
    return {
      faultDesc: String(item?.faultDesc ?? ""),
      repairOptions: repairOptions.length ? repairOptions : [""],
    };
  });

  return {
    id: data?.id,
    companyId: data?.companyId,
    companyName: data?.companyName,
    productCode: String(data?.productCode ?? ""),
    productModel: String(data?.productModel ?? ""),
    status: data?.status === 0 ? 0 : 1,
    remark: String(data?.remark ?? ""),
    faults,
  };
}

/**
 * 作用：在故障表单中追加一条故障信息。
 * @returns 无
 */
function addFaultItem() {
  const list = Array.isArray(formModel.faults)
    ? (formModel.faults as FaultRepairItem[])
    : [];
  list.push(createFaultItem());
  formModel.faults = list;
}

/**
 * 作用：移除指定索引的故障信息（至少保留一条）。
 * @param index - 下标
 */
function removeFaultItem(index: number) {
  const list = Array.isArray(formModel.faults)
    ? (formModel.faults as FaultRepairItem[])
    : [];
  if (list.length <= 1) return;
  list.splice(index, 1);
}

/**
 * 作用：为某故障追加一项维修说明输入框。
 * @param item - 故障项
 * @returns 无
 */
function addRepairOption(item: FaultRepairItem) {
  item.repairOptions.push("");
}

/**
 * 作用：移除指定维修说明项（至少保留一项）。
 * @param item - 故障项
 * @param index - 下标
 */
function removeRepairOption(item: FaultRepairItem, index: number) {
  if (item.repairOptions.length <= 1) return;
  item.repairOptions.splice(index, 1);
}

/**
 * 作用：校验故障描述 / 维修说明是否重复（单项必填由子表单项 rules 处理）。
 * @param data - 表单数据
 * @returns 首条错误文案或 null
 */
function getFaultDuplicateError(data: RowData): string | null {
  const faults = Array.isArray(data.faults) ? data.faults : [];
  const descSet = new Set<string>();
  for (const item of faults) {
    const desc = String(item?.faultDesc || "").trim();
    if (desc && descSet.has(desc)) {
      return "同一配置下故障描述不能重复";
    }
    if (desc) descSet.add(desc);
    const options = (
      Array.isArray(item?.repairOptions) ? item.repairOptions : []
    )
      .map((x: unknown) => String(x || "").trim())
      .filter(Boolean);
    if (new Set(options).size !== options.length) {
      return "同一故障下维修说明不能重复";
    }
  }
  return null;
}

const unifiedFormRules = computed(() => {
  const key = activeKey.value;
  if (key === "dict") {
    return {
      dictName: [
        { required: true, message: "请输入字典名称", trigger: "blur" },
      ],
      dictType: [
        { required: true, message: "请输入字典类型", trigger: "blur" },
      ],
      status: [
        { required: true, message: "请选择状态", trigger: "change" },
        {
          validator: async (_rule: unknown, v: unknown) => {
            if (v === undefined || v === null || v === "") {
              return Promise.reject(new Error("请选择状态"));
            }
            return Promise.resolve();
          },
          trigger: "change",
        },
      ],
    };
  }
  if (key === "config") {
    return {
      configName: [
        { required: true, message: "请输入参数名称", trigger: "blur" },
      ],
      configKey: [
        { required: true, message: "请输入参数键名", trigger: "blur" },
      ],
      configValue: [
        { required: true, message: "请输入参数键值", trigger: "blur" },
      ],
      configType: [
        { required: true, message: "请选择是否内置", trigger: "change" },
        {
          validator: async (_rule: unknown, v: unknown) => {
            if (v === undefined || v === null || v === "") {
              return Promise.reject(new Error("请选择是否内置"));
            }
            return Promise.resolve();
          },
          trigger: "change",
        },
      ],
    };
  }
  if (key === "notifyTemplate") {
    return {
      templateCode: [
        { required: true, message: "请输入模板编码", trigger: "blur" },
      ],
      templateName: [
        { required: true, message: "请输入模板名称", trigger: "blur" },
      ],
    };
  }
  if (key === "roleTemplate") {
    return {
      roleName: [
        { required: true, message: "请输入角色名称", trigger: "blur" },
      ],
      roleKey: [{ required: true, message: "请输入角色标识", trigger: "blur" }],
      typeCode: [
        { required: true, message: "请先选择类型编码", trigger: "change" },
      ],
      dataScope: [
        { required: true, message: "请先选择数据范围", trigger: "change" },
        {
          validator: async () => {
            const v = String(formModel.dataScope || "");
            if (!v) {
              return Promise.reject(new Error("请先选择数据范围"));
            }
            if (
              !roleTemplateScopeOptions.value.some(
                (item) => String(item.value) === String(formModel.dataScope),
              )
            ) {
              return Promise.reject(
                new Error("请选择当前公司类型允许的数据范围"),
              );
            }
            return Promise.resolve();
          },
          trigger: "change",
        },
      ],
    };
  }
  if (key === "fault") {
    return {
      companyId: [
        { required: true, message: "请选择归属总部", trigger: "change" },
      ],
      status: [
        { required: true, message: "请选择状态", trigger: "change" },
        {
          validator: async (_rule: unknown, v: unknown) => {
            if (v === undefined || v === null || v === "") {
              return Promise.reject(new Error("请选择状态"));
            }
            return Promise.resolve();
          },
          trigger: "change",
        },
      ],
      productCode: [
        {
          validator: async () => {
            const pc = String(formModel.productCode || "").trim();
            const pm = String(formModel.productModel || "").trim();
            if (!pc && !pm) {
              return Promise.reject(
                new Error("物料编码和产品型号不能同时为空"),
              );
            }
            return Promise.resolve();
          },
          trigger: "blur",
        },
      ],
      faults: [
        {
          type: "array",
          required: true,
          min: 1,
          message: "请至少添加一条故障信息",
          trigger: "change",
        },
        {
          validator: async () => {
            const msg = getFaultDuplicateError(formModel);
            return msg ? Promise.reject(new Error(msg)) : Promise.resolve();
          },
          trigger: "change",
        },
      ],
    };
  }
  return {};
});

const notifyFormRules = {
  templateCode: [
    { required: true, message: "模板编码不能为空", trigger: "blur" },
  ],
  templateName: [
    { required: true, message: "请输入模板名称", trigger: "blur" },
  ],
  notifyEnabled: [
    { required: true, message: "请选择通知开关", trigger: "change" },
    {
      validator: async (_rule: unknown, v: unknown) => {
        if (v === undefined || v === null || v === "") {
          return Promise.reject(new Error("请选择通知开关"));
        }
        return Promise.resolve();
      },
      trigger: "change",
    },
  ],
  overrideEnabled: [
    { required: true, message: "请选择覆盖开关", trigger: "change" },
    {
      validator: async (_rule: unknown, v: unknown) => {
        if (v === undefined || v === null || v === "") {
          return Promise.reject(new Error("请选择覆盖开关"));
        }
        return Promise.resolve();
      },
      trigger: "change",
    },
  ],
};

const syncTaskFormRules = {
  taskCode: [{ required: true, message: "请输入任务编码", trigger: "blur" }],
  taskName: [{ required: true, message: "请输入任务名称", trigger: "blur" }],
  handlerCode: [{ required: true, message: "请选择处理器", trigger: "change" }],
  cronExpression: [
    { required: true, message: "请输入Cron表达式", trigger: "blur" },
  ],
};

const advRegionFormRules = {
  regionCode: [{ required: true, message: "请输入大区编码", trigger: "blur" }],
  regionName: [{ required: true, message: "请输入大区名称", trigger: "blur" }],
};

/**
 * 作用：组装故障维修提交给后端的 payload。
 * @param data - 表单数据
 * @returns 提交体
 */
function buildFaultSubmitPayload(data: RowData) {
  return {
    id: data.id,
    companyId: Number(data.companyId),
    productCode: String(data.productCode || "").trim(),
    productModel: String(data.productModel || "").trim(),
    status: Number(data.status ?? 1),
    remark: String(data.remark || "").trim(),
    faults: (Array.isArray(data.faults) ? data.faults : []).map(
      (item: RowData) => ({
        faultDesc: String(item.faultDesc || "").trim(),
        repairOptions: (Array.isArray(item.repairOptions)
          ? item.repairOptions
          : []
        )
          .map((x: unknown) => String(x || "").trim())
          .filter(Boolean),
      }),
    ),
  };
}

/**
 * 作用：提交通用表单（字典/参数/通知/故障/角色模板等）。
 * @returns 无
 */
async function submitForm() {
  try {
    await unifiedFormRef.value?.validate();
  } catch {
    return;
  }
  const data = { ...formModel };
  let submitResult: unknown;
  switch (activeKey.value) {
    case "dict":
      submitResult =
        (data.dictId ?? data.id)
          ? await updateDictType(data)
          : await addDictType(data);
      break;
    case "config":
      submitResult =
        (data.configId ?? data.id)
          ? await updateConfig(data)
          : await addConfig(data);
      break;
    case "notifyTemplate":
      submitResult = data.id
        ? await updateNotifyTemplateCustom(data)
        : await addNotifyTemplateCustom(data);
      break;
    case "fault":
      submitResult = data.id
        ? await updateFaultRepairConfig(buildFaultSubmitPayload(data))
        : await addFaultRepairConfig(buildFaultSubmitPayload(data));
      break;
    case "roleTemplate":
      submitResult = data.id
        ? await updateRoleTemplate(data)
        : await addRoleTemplate(data);
      break;
    default:
      return;
  }
  if (!notifyOnceSuccessFromFlatResult(submitResult, "操作成功")) return;
  formOpen.value = false;
  loadList();
}

/**
 * 作用：按当前子模块删除表格行。
 * @param record - 行数据
 * @returns 无
 */
async function removeRow(record: RowData) {
  let delResult: unknown;
  switch (activeKey.value) {
    case "dict":
      delResult = await deleteDictType(record.dictId ?? record.id);
      break;
    case "config":
      delResult = await deleteConfig(record.configId ?? record.id);
      break;
    case "notifyTemplate":
      delResult = await deleteNotifyTemplateCustom(record.id);
      break;
    case "fault":
      window.$message?.warning?.(
        "jasic 后端无删除接口，请使用停用或联系管理员",
      );
      return;
    case "roleTemplate":
      delResult = await deleteRoleTemplate(record.id);
      break;
    case "region":
      delResult = await deleteRegion(record.id, {
        targetCompanyId: Number(record?.companyId ?? regionHqId.value),
      });
      break;
    default:
      return;
  }
  notifyOnceSuccessFromFlatResult(
    delResult,
    activeKey.value === "notifyTemplate" ? "删除成功" : "已删除",
  );
  loadList();
}

/**
 * 作用：刷新当前 Tab 对应的后端缓存（字典/参数/通知模板）。
 * @returns 无
 */
async function onRefreshCache() {
  if (activeKey.value === "dict") {
    const r = await refreshDictTypeCache();
    notifyOnceSuccessFromFlatResult(r, "字典缓存已刷新");
  } else if (activeKey.value === "config") {
    const r = await refreshConfigCache();
    notifyOnceSuccessFromFlatResult(r, "参数缓存已刷新");
  } else if (activeKey.value === "notifyTemplate") {
    const r = await refreshNotifyTemplateCache();
    notifyOnceSuccessFromFlatResult(r, "通知模板缓存已刷新");
  }
  loadList();
}

/**
 * 作用：手动触发一条同步任务执行。
 * @param record - 任务行
 * @returns 无
 */
async function onRunSyncTask(record: RowData) {
  const id = record.id ?? record.taskId;
  if (id == null) return;
  const r = await executeSyncTask(id);
  notifyOnceSuccessFromFlatResult(r, "已触发执行");
  loadList();
}

/**
 * 作用：同步单行角色模板至业务侧。
 * @param record - 模板行
 * @returns 无
 */
async function onSyncRoleTemplateRow(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const r = await syncRoleTemplate(id);
  notifyOnceSuccessFromFlatResult(r, "全量同步成功");
}

/**
 * 作用：触发机器条码全量同步任务。
 * @returns 无
 */
async function onFullSyncBarcode() {
  const r = await fullSyncMachineBarcode();
  notifyOnceSuccessFromFlatResult(r, "已触发全量同步");
  loadList();
}

/**
 * 作用：打开条码档案详情右侧抽屉并加载详情。
 * @param record - 列表行
 * @returns 无
 */
async function openBarcodeDetail(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const ownerHqId = Number(record.hqCompanyId ?? barcodeQuery.ownerHqId);
  const params = Number.isFinite(ownerHqId) ? { ownerHqId } : undefined;
  const { data } = await getMachineBarcode(id, params);
  barcodeDetail.value = (data as RowData) || record;
  barcodeDetailOpen.value = true;
}

/**
 * 作用：判断列表中是否已存在某编码的自定义通知模板。
 * @param templateCode - 模板编码
 * @returns 是否存在 CUSTOM 来源记录
 */
function hasCustomNotifyTemplate(templateCode?: string) {
  const code = String(templateCode || "");
  if (!code) return false;
  return rows.value.some(
    (item) =>
      String(item.templateCode || "") === code &&
      String(item.templateSource || "") === "CUSTOM",
  );
}

/**
 * 作用：生成通知模板预览用的示例变量 JSON 字符串。
 * @param templateCode - 模板编码，用于分支变量集合
 * @returns JSON 字符串
 */
function buildNotifyPreviewVariables(templateCode?: string) {
  // 给预览接口准备一个“尽量全”的变量集合，
  // 避免不同模板的占位符差异导致标题/摘要/路由值渲染时缺变量。
  const commonVars = {
    // 通用业务类变量
    bizId: 88,
    bizNo: "WO202604180001",
    receiverName: "张三",
    // 工单类变量（部分模板/渠道会用到）
    workOrderId: 10001,
    orderNo: "WO202604210001",
    customerMobile: "13800138000",
    companyName: "深圳南山服务网点",
    closedTime: "2026-04-21 15:30:00",
  };

  if (templateCode === "WORK_ORDER_EVALUATION_INVITE") {
    return JSON.stringify({ ...commonVars }, null, 2);
  }
  return JSON.stringify(commonVars, null, 2);
}

/**
 * 作用：用详情数据填充通知模板表单模型。
 * @param detail - 模板详情
 * @returns 无
 */
function fillNotifyForm(detail: RowData) {
  Object.keys(notifyForm).forEach((k) => delete notifyForm[k]);
  Object.assign(notifyForm, {
    ...detail,
    notifyEnabled: Number(detail.notifyEnabled ?? 1),
    overrideEnabled: Number(detail.overrideEnabled ?? 0),
  });
}

/**
 * 作用：只读打开通知模板详情抽屉。
 * @param record - 行或详情引用
 * @returns 无
 */
async function openNotifyView(record: RowData) {
  notifyFormTitle.value = "查看通知模板";
  notifyFormReadonly.value = true;
  const detail =
    record.id == null
      ? record
      : ((await getNotifyTemplate(record.id)).data as RowData) || record;
  fillNotifyForm(detail);
  notifyFormOpen.value = true;
}

/**
 * 作用：基于标准模板打开「新增自定义模板」表单。
 * @param record - 基准行（提供编码等）
 * @returns 无
 */
async function openNotifyAddCustom(record: RowData) {
  notifyFormTitle.value = "新增自定义模板";
  notifyFormReadonly.value = false;
  fillNotifyForm({
    templateCode: record.templateCode,
    templateName: record.templateName,
    notifyEnabled: 1,
    overrideEnabled: 0,
    routeType: record.routeType || "",
    titleTemplate: "",
    summaryTemplate: "",
    routeValueTemplate: "",
    remark: "",
    variablesJson: record.variablesJson || "",
  });
  notifyFormOpen.value = true;
}

/**
 * 作用：打开自定义模板编辑抽屉。
 * @param record - 列表行
 * @returns 无
 */
async function openNotifyEdit(record: RowData) {
  notifyFormTitle.value = "编辑自定义模板";
  notifyFormReadonly.value = false;
  const detail =
    ((await getNotifyTemplate(record.id)).data as RowData) || record;
  fillNotifyForm(detail);
  notifyFormOpen.value = true;
}

/**
 * 作用：提交通知模板表单（新增或更新自定义模板）。
 * @returns 无
 */
async function submitNotifyForm() {
  try {
    await notifyFormRef.value?.validate();
  } catch {
    return;
  }
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
      remark: notifyForm.remark,
    };
    let notifySubmitResult: unknown;
    if (notifyForm.id) {
      notifySubmitResult = await updateNotifyTemplateCustom(payload);
    } else {
      notifySubmitResult = await addNotifyTemplateCustom(payload);
    }
    if (!notifyOnceSuccessFromFlatResult(notifySubmitResult, "操作成功"))
      return;
    notifyFormOpen.value = false;
    await loadList();
  } finally {
    notifyFormSubmitting.value = false;
  }
}

/**
 * 作用：从列表行打开模板预览弹窗并请求预览接口。
 * @param record - 模板行
 * @returns 无
 */
async function onPreviewNotify(record: RowData) {
  const payload = {
    templateCode: record.templateCode,
    notifyEnabled: record.notifyEnabled,
    overrideEnabled: record.overrideEnabled,
    routeType: record.routeType,
    titleTemplate: record.titleTemplate,
    summaryTemplate: record.summaryTemplate,
    routeValueTemplate: record.routeValueTemplate,
  };
  notifyPreviewVariablesText.value = buildNotifyPreviewVariables(
    record.templateCode,
  );
  notifyPreviewResult.value = null;
  notifyPreviewPayload.value = payload;
  notifyPreviewOpen.value = true;
  await runNotifyPreview(payload);
}

/**
 * 作用：从抽屉表单打开预览并带上当前编辑内容。
 * @returns 无
 */
async function onPreviewNotifyForm() {
  const payload = {
    templateCode: notifyForm.templateCode,
    notifyEnabled: notifyForm.notifyEnabled,
    overrideEnabled: notifyForm.overrideEnabled,
    routeType: notifyForm.routeType,
    titleTemplate: notifyForm.titleTemplate,
    summaryTemplate: notifyForm.summaryTemplate,
    routeValueTemplate: notifyForm.routeValueTemplate,
  };
  notifyPreviewVariablesText.value =
    notifyPreviewVariablesText.value ||
    buildNotifyPreviewVariables(notifyForm.templateCode);
  notifyPreviewResult.value = null;
  notifyPreviewPayload.value = payload;
  notifyPreviewOpen.value = true;
  await runNotifyPreview(payload);
}

/**
 * 作用：调用预览接口并写入预览结果。
 * @param payload - 模板内容与开关等
 * @returns 无
 */
async function runNotifyPreview(payload: RowData) {
  let variables = {};
  try {
    variables = notifyPreviewVariablesText.value
      ? JSON.parse(notifyPreviewVariablesText.value)
      : {};
  } catch {
    window.$message?.error?.("示例变量 JSON 格式不正确");
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

/**
 * 作用：打开通知渠道配置抽屉并加载渠道列表。
 * @param record - 模板行（含 templateCode）
 * @returns 无
 */
async function openChannelsEditor(record: RowData) {
  channelsReadonly.value = !hasAuth("system:notifyTemplate:update");
  channelsTemplateCode.value = String(record.templateCode || "");
  channelsRows.value = [];
  channelsOpen.value = true;
  channelsLoading.value = true;
  try {
    const { data } = await listNotifyTemplateChannels(
      channelsTemplateCode.value,
    );
    const rows = Array.isArray(data) ? data : [];
    channelsRows.value = rows.length
      ? rows.map((item) => normalizeChannelRow(item as RowData))
      : defaultChannelRows();
  } finally {
    channelsLoading.value = false;
  }
}

/**
 * 作用：新建一行渠道配置默认值（按模板编码可返回预设）。
 * @returns 渠道行对象
 */
function createChannelRow() {
  if (channelsTemplateCode.value === "WORK_ORDER_EVALUATION_INVITE") {
    return {
      channelType: "MP_SUBSCRIBE",
      channelEnabled: 1,
      channelScene: "C",
      templateId: "",
      pagePathTemplate: "pages/order/evaluate?workOrderId=${workOrderId}",
      fieldMapping: [
        { field: "thing1", value: "${orderNo}" },
        { field: "phone_number2", value: "${customerMobile}" },
        { field: "thing3", value: "${companyName}" },
      ],
      remark: "客户满意度评价通知默认渠道配置",
    };
  }
  return {
    channelType: "MP_SUBSCRIBE",
    channelEnabled: 1,
    channelScene: "",
    templateId: "",
    pagePathTemplate: "",
    fieldMapping: [],
    remark: "",
  };
}

/**
 * 作用：无渠道数据时的默认一行。
 * @returns 渠道行数组
 */
function defaultChannelRows() {
  return [createChannelRow()];
}

/**
 * 作用：将接口渠道项规整为表单行结构。
 * @param item - 原始项
 * @returns 规整后的行
 */
function normalizeChannelRow(item: RowData) {
  return {
    id: item.id,
    channelType: item.channelType || "MP_SUBSCRIBE",
    channelEnabled:
      item.channelEnabled == null ? 1 : Number(item.channelEnabled),
    channelScene: item.channelScene || "",
    templateId: item.templateId || "",
    pagePathTemplate: item.pagePathTemplate || "",
    fieldMapping: (Array.isArray(item.fieldMapping)
      ? item.fieldMapping
      : []
    ).map((mapping: RowData) => ({
      field: mapping.field || "",
      value: mapping.value || "",
    })),
    remark: item.remark || "",
  };
}

/**
 * 作用：组装单条渠道保存请求体。
 * @param item - 表单行
 * @returns payload
 */
function buildChannelPayload(item: RowData) {
  return {
    id: item.id,
    templateCode: channelsTemplateCode.value,
    channelType: item.channelType,
    channelEnabled: Number(item.channelEnabled ?? 1),
    channelScene: item.channelScene,
    templateId: item.templateId,
    pagePathTemplate: item.pagePathTemplate,
    fieldMapping: (Array.isArray(item.fieldMapping)
      ? item.fieldMapping
      : []
    ).map((mapping: RowData) => ({
      field: String(mapping.field || ""),
      value: String(mapping.value || ""),
    })),
    remark: item.remark,
  };
}

/**
 * 作用：渠道表格追加一行。
 * @returns 无
 */
function addChannelRow() {
  channelsRows.value.push(createChannelRow());
}

/**
 * 作用：删除指定索引的渠道行。
 * @param index - 下标
 */
function removeChannelRow(index: number) {
  channelsRows.value.splice(index, 1);
}

/**
 * 作用：为渠道行追加一条字段映射空行。
 * @param item - 渠道行
 */
function addFieldMapping(item: RowData) {
  if (!Array.isArray(item.fieldMapping)) item.fieldMapping = [];
  item.fieldMapping.push({ field: "", value: "" });
}

/**
 * 作用：移除渠道行内指定字段映射。
 * @param item - 渠道行
 * @param index - 映射下标
 */
function removeFieldMapping(item: RowData, index: number) {
  if (!Array.isArray(item.fieldMapping)) return;
  item.fieldMapping.splice(index, 1);
}

/**
 * 作用：批量保存当前模板渠道配置。
 * @returns 无
 */
async function saveChannels() {
  channelsLoading.value = true;
  try {
    const payload = channelsRows.value.map((item) => buildChannelPayload(item));
    const r = await saveNotifyTemplateChannels(
      channelsTemplateCode.value,
      payload,
    );
    if (!notifyOnceSuccessFromFlatResult(r, "渠道配置已保存")) return;
    channelsOpen.value = false;
  } finally {
    channelsLoading.value = false;
  }
}

/**
 * 作用：打开同步任务新增/编辑抽屉。
 * @param record - 编辑时传入行数据，新增时省略
 * @returns 无
 */
function openSyncForm(record?: RowData) {
  syncFormTitle.value = record ? "编辑同步任务" : "新增同步任务";
  Object.keys(syncFormModel).forEach((k) => delete syncFormModel[k]);
  if (record) {
    Object.assign(syncFormModel, JSON.parse(JSON.stringify(record)));
  } else {
    syncFormModel.status = 0;
    syncFormModel.cronExpression = "0 0 2 * * ?";
  }
  syncFormOpen.value = true;
}

/**
 * 作用：提交同步任务表单。
 * @returns 无
 */
async function submitSyncForm() {
  try {
    await syncTaskFormRef.value?.validate();
  } catch {
    return;
  }
  const data = { ...syncFormModel };
  const syncSubmitResult = data.id
    ? await updateSyncTask(data)
    : await addSyncTask(data);
  if (!notifyOnceSuccessFromFlatResult(syncSubmitResult, "已保存")) return;
  syncFormOpen.value = false;
  loadList();
}

/**
 * 作用：拉取任务详情后打开编辑抽屉。
 * @param record - 列表行
 * @returns 无
 */
async function openSyncFormEdit(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const { data } = await getSyncTask(id);
  openSyncForm((data as RowData) || record);
}

/**
 * 作用：打开同步任务执行日志弹窗并加载第一页。
 * @param record - 任务行
 * @returns 无
 */
async function openLogDialog(record: RowData) {
  logTaskId.value = record.id;
  logPage.pageNum = 1;
  logDialogTitle.value = `执行日志 - ${record.taskName || ""}`;
  logQuery.status = undefined;
  logOpen.value = true;
  await loadSyncLogs();
}

/**
 * 作用：按当前任务与筛选分页加载执行日志。
 * @returns 无
 */
async function loadSyncLogs() {
  if (logTaskId.value == null) return;
  logLoading.value = true;
  try {
    const { data } = await listSyncTaskLog({
      taskId: logTaskId.value,
      status: logQuery.status,
      pageNum: logPage.pageNum,
      pageSize: logPage.pageSize,
    });
    logRows.value = pickRows(data);
    logTotal.value = pickTotal(data);
  } finally {
    logLoading.value = false;
  }
}

/**
 * 作用：日志表格分页变更。
 * @param page - 页码
 * @param pageSize - 每页条数，可选
 * @returns 无
 */
function onLogPageChange(page: number, pageSize?: number) {
  logPage.pageNum = page;
  if (pageSize) logPage.pageSize = pageSize;
  loadSyncLogs();
}

/**
 * 作用：日志状态筛选变更时回到第一页并刷新。
 * @returns 无
 */
function handleLogStatusChange() {
  logPage.pageNum = 1;
  loadSyncLogs();
}

/**
 * 作用：同步日志状态对应 Tag 颜色。
 * @param status - 状态枚举字符串
 * @returns Ant Tag color
 */
function syncLogStatusTagColor(status: unknown) {
  const s = String(status || "");
  if (s === "SUCCESS") return "success";
  if (s === "FAILED") return "error";
  if (s === "RUNNING") return "processing";
  return "default";
}

/**
 * 作用：同步日志触发人展示，定时任务统一展示系统任务身份。
 * @param record - 日志行
 * @returns 展示文案
 */
function syncLogTriggerUserLabel(record: RowData) {
  if (record.triggerType === "SCHEDULED" || Number(record.triggerUserId) === 0)
    return "系统任务";
  return record.triggerUserId || "-";
}

/**
 * 作用：打开角色模板菜单分配抽屉并加载菜单树。
 * @param record - 模板行
 * @returns 无
 */
async function openRoleTemplateMenuAssign(record: RowData) {
  const typeCode = String(record.typeCode || "");
  if (!typeCode) {
    window.$message?.warning?.("当前模板缺少 typeCode");
    return;
  }
  menuAssignTypeCode.value = typeCode;
  menuAssignTemplate.value = { ...record };
  menuExpandedKeys.value = [];
  menuAssignOpen.value = true;
  const { data: treeData } = await typeCodeMenuTree(typeCode);
  menuTreeData.value = Array.isArray(treeData) ? treeData : pickRows(treeData);
  const rawIds = Array.isArray(record.menuIds) ? record.menuIds : [];
  menuCheckedKeys.value = (rawIds as unknown[])
    .map((x) => Number(x))
    .filter((id) => !Number.isNaN(id));
  menuExpandedKeys.value = computeExpandedKeysForCheckedMenuTree(
    menuTreeData.value,
    menuCheckedKeys.value,
  );
  await nextTick();
}

/**
 * 作用：提交模板菜单勾选结果。
 * @returns 无
 */
async function submitRoleTemplateMenuAssign() {
  if (!menuAssignTypeCode.value || !menuAssignTemplate.value?.id) return;
  menuAssignSubmitting.value = true;
  try {
    const r = await updateRoleTemplate({
      id: menuAssignTemplate.value.id,
      typeCode: menuAssignTemplate.value.typeCode,
      roleName: menuAssignTemplate.value.roleName,
      roleKey: menuAssignTemplate.value.roleKey,
      dataScope: menuAssignTemplate.value.dataScope,
      isAdmin: menuAssignTemplate.value.isAdmin === 1 ? 1 : 0,
      orderNum: menuAssignTemplate.value.orderNum,
      remark: menuAssignTemplate.value.remark,
      menuIds: menuCheckedKeys.value,
    });
    if (!notifyOnceSuccessFromFlatResult(r, "模板菜单已分配")) return;
    menuAssignOpen.value = false;
    await loadList();
  } finally {
    menuAssignSubmitting.value = false;
  }
}

/**
 * 作用：打开系统大区新增/编辑表单。
 * @param record - 编辑时传入行，新增时省略
 * @returns 无
 */
function openRegionForm(record?: RowData) {
  if (!regionHqId.value) {
    window.$message?.warning?.("请先选择总部公司");
    return;
  }
  regionFormTitle.value = record ? "编辑大区" : "新增大区";
  Object.keys(regionForm).forEach((k) => delete regionForm[k]);
  if (record) {
    Object.assign(regionForm, JSON.parse(JSON.stringify(record)));
    regionForm.targetCompanyId = Number(record.companyId ?? regionHqId.value);
    delete regionForm.companyId;
  } else {
    regionForm.targetCompanyId = regionHqId.value;
  }
  regionFormOpen.value = true;
}

/**
 * 作用：提交大区表单（新增或更新）。
 * @returns 无
 */
async function submitRegionForm() {
  try {
    await advRegionFormRef.value?.validate();
  } catch {
    return;
  }
  const data = { ...regionForm };
  const regionSubmitResult = data.id
    ? await updateRegion(data)
    : await addRegion(data);
  if (!notifyOnceSuccessFromFlatResult(regionSubmitResult, "已保存")) return;
  regionFormOpen.value = false;
  loadList();
}

/**
 * 作用：打开故障维修配置只读详情。
 * @param record - 列表行
 * @returns 无
 */
async function openFaultDetail(record: RowData) {
  const id = record.id;
  if (id == null) return;
  const { data } = await getFaultRepairConfig(
    id,
    buildFaultDetailParams(record),
  );
  faultDetail.value = normalizeFaultFormData((data as RowData) || record);
  faultDetailOpen.value = true;
}

/**
 * 作用：将类型编码转为可读名称（查映射表）。
 * @param code - typeCode
 * @returns 展示文案
 */
function typeCodeLabel(code: string) {
  return typeCodeLabelMap.value[code] || code;
}

/**
 * 作用：表格中展示角色模板数据范围的可读标签。
 * @param row - 模板行
 * @returns 标签或原值
 */
function getRoleTemplateDataScopeLabel(row: RowData) {
  const typeCode = String(row.typeCode || "");
  const dataScope = String(row.dataScope || "");
  if (!typeCode || !dataScope) return row.dataScope || "-";

  const options = roleTemplateDataScopeMap.value[typeCode] || [];
  const matched = options.find(
    (option: DataScopeOption) => option.value === dataScope,
  );
  return matched?.label || dataScope;
}

/**
 * 作用：角色模板类型筛选变更后回到第一页并刷新列表。
 * @returns 无
 */
function onRoleTemplateTypeChange() {
  pageQuery.pageNum = 1;
  loadList();
}

/**
 * 作用：按表单中的 typeCode 拉取可选数据范围并重置 dataScope（可选保留当前值）。
 * @param keepCurrentValue - 为 true 时在合法时保留当前 dataScope
 * @returns 无
 */
async function onRoleTemplateDataScopeInit(keepCurrentValue = false) {
  const typeCode = String(formModel.typeCode || "");
  if (!typeCode) {
    roleTemplateScopeOptions.value = [];
    formModel.dataScope = "";
    return;
  }
  try {
    const { data } = await roleTemplateDataScopeOptions(typeCode);
    const list = (Array.isArray(data) ? data : pickRows(data)) as RowData[];
    const options: DataScopeOption[] = list.map((item: RowData) => ({
      label: String(item.label ?? item.name ?? item.value),
      value: String(item.value ?? item.code ?? item.key),
      defaultOption: Boolean(item.defaultOption),
      disabled: Boolean(item.disabled),
    }));
    roleTemplateScopeOptions.value = options.map((item) => ({
      label: item.label,
      value: item.value,
    }));
    const currentValue = String(formModel.dataScope || "");
    const currentValid = options.some((item) => item.value === currentValue);
    if (!keepCurrentValue || !currentValid) {
      formModel.dataScope =
        options.find((item) => item.defaultOption)?.value ||
        options[0]?.value ||
        "";
    }
  } catch {
    roleTemplateScopeOptions.value = [];
    if (!keepCurrentValue) formModel.dataScope = "";
  }
}

// 系统大区 Tab：切换所选总部后刷新列表
watch(regionHqId, () => {
  if (activeKey.value === "region") loadList();
});

// 路由 name 变化时同步当前高级配置子模块 Tab
watch(
  () => route.name,
  (name) => {
    void syncActiveModuleByRouteName(name);
  },
);

/**
 * 作用：挂载时按路由初始化 Tab；若路由未切换 Tab 则直接加载列表。
 */
onMounted(async () => {
  const changedByRoute = await syncActiveModuleByRouteName(route.name);
  if (!changedByRoute) {
    await loadList();
  }
});
</script>

<template>
  <div
    class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto"
  >
    <ACard :bordered="false" class="card-wrapper">
      <div class="flex flex-col gap-12px">
        <AForm
          v-if="activeKey === 'dict'"
          :model="dictQuery"
          :label-col="{ span: 5, md: 7 }"
        >
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      dictSearchFilter.isSearchFilterHidden(0),
                  }"
                >
                  <AFormItem label="字典名称" class="m-0">
                    <AInput
                      v-model:value="dictQuery.dictName"
                      allow-clear
                      placeholder="请输入字典名称"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      dictSearchFilter.isSearchFilterHidden(1),
                  }"
                >
                  <AFormItem label="字典类型" class="m-0">
                    <AInput
                      v-model:value="dictQuery.dictType"
                      allow-clear
                      placeholder="请输入字典类型"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      dictSearchFilter.isSearchFilterHidden(2),
                  }"
                >
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="dictQuery.status"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '启用', value: 1 },
                        { label: '停用', value: 0 },
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
              <PageSearchExpandButton
                v-if="dictSearchFilter.showSearchFilterExpandToggle"
                :expanded="dictSearchFilter.searchFilterExpanded"
                @click="dictSearchFilter.toggleSearchFilterExpand"
              />
            </div>
          </div>
        </AForm>

        <AForm
          v-if="activeKey === 'config'"
          :model="configQuery"
          :label-col="{ span: 5, md: 7 }"
        >
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      configSearchFilter.isSearchFilterHidden(0),
                  }"
                >
                  <AFormItem label="参数名称" class="m-0">
                    <AInput
                      v-model:value="configQuery.configName"
                      allow-clear
                      placeholder="请输入参数名称"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      configSearchFilter.isSearchFilterHidden(1),
                  }"
                >
                  <AFormItem label="参数键名" class="m-0">
                    <AInput
                      v-model:value="configQuery.configKey"
                      allow-clear
                      placeholder="请输入参数键名"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      configSearchFilter.isSearchFilterHidden(2),
                  }"
                >
                  <AFormItem label="是否内置" class="m-0">
                    <ASelect
                      v-model:value="configQuery.configType"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '是', value: 1 },
                        { label: '否', value: 0 },
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
              <PageSearchExpandButton
                v-if="configSearchFilter.showSearchFilterExpandToggle"
                :expanded="configSearchFilter.searchFilterExpanded"
                @click="configSearchFilter.toggleSearchFilterExpand"
              />
            </div>
          </div>
        </AForm>

        <AForm
          v-if="activeKey === 'notifyTemplate'"
          :model="notifyQuery"
          :label-col="{ span: 5, md: 7 }"
        >
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      notifySearchFilter.isSearchFilterHidden(0),
                  }"
                >
                  <AFormItem label="模板编码" class="m-0">
                    <AInput
                      v-model:value="notifyQuery.templateCode"
                      allow-clear
                      placeholder="请输入模板编码"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      notifySearchFilter.isSearchFilterHidden(1),
                  }"
                >
                  <AFormItem label="模板名称" class="m-0">
                    <AInput
                      v-model:value="notifyQuery.templateName"
                      allow-clear
                      placeholder="请输入模板名称"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      notifySearchFilter.isSearchFilterHidden(2),
                  }"
                >
                  <AFormItem label="模板来源" class="m-0">
                    <ASelect
                      v-model:value="notifyQuery.templateSource"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '内置', value: 'BUILT_IN' },
                        { label: '自定义', value: 'CUSTOM' },
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
              <PageSearchExpandButton
                v-if="notifySearchFilter.showSearchFilterExpandToggle"
                :expanded="notifySearchFilter.searchFilterExpanded"
                @click="notifySearchFilter.toggleSearchFilterExpand"
              />
            </div>
          </div>
        </AForm>

        <AForm
          v-if="activeKey === 'barcode'"
          :model="barcodeQuery"
          :label-col="{ span: 5, md: 7 }"
        >
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(0),
                  }"
                >
                  <AFormItem label="归属总部" class="m-0">
                    <ASelect
                      v-model:value="barcodeQuery.ownerHqId"
                      allow-clear
                      show-search
                      option-filter-prop="label"
                      placeholder="请选择总部"
                      class="w-full"
                      :options="
                        barcodeHqOptions.map((c: RowData) => ({
                          label: c.companyName ?? c.label,
                          value: Number(c.id ?? c.value),
                        }))
                      "
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(1),
                  }"
                >
                  <AFormItem label="条码" class="m-0">
                    <AInput
                      v-model:value="barcodeQuery.barcode"
                      allow-clear
                      placeholder="请输入条码"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(2),
                  }"
                >
                  <AFormItem label="发货单号" class="m-0">
                    <AInput
                      v-model:value="barcodeQuery.deliverNumber"
                      allow-clear
                      placeholder="请输入发货单号"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(3),
                  }"
                >
                  <AFormItem label="物料编码" class="m-0">
                    <AInput
                      v-model:value="barcodeQuery.productCode"
                      allow-clear
                      placeholder="请输入物料编码"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(4),
                  }"
                >
                  <AFormItem label="机器小号" class="m-0">
                    <AInput
                      v-model:value="barcodeQuery.machineNo"
                      allow-clear
                      placeholder="请输入机器小号"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(5),
                  }"
                >
                  <AFormItem label="产品型号" class="m-0">
                    <AInput
                      v-model:value="barcodeQuery.productModel"
                      allow-clear
                      placeholder="请输入产品型号"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      barcodeSearchFilter.isSearchFilterHidden(6),
                  }"
                >
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="barcodeQuery.status"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '启用', value: 1 },
                        { label: '停用', value: 0 },
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
              <PageSearchExpandButton
                v-if="barcodeSearchFilter.showSearchFilterExpandToggle"
                :expanded="barcodeSearchFilter.searchFilterExpanded"
                @click="barcodeSearchFilter.toggleSearchFilterExpand"
              />
            </div>
          </div>
        </AForm>

        <AForm
          v-if="activeKey === 'syncTask'"
          :model="syncTaskQuery"
          :label-col="{ span: 5, md: 7 }"
        >
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      syncTaskSearchFilter.isSearchFilterHidden(0),
                  }"
                >
                  <AFormItem label="任务编码" class="m-0">
                    <AInput
                      v-model:value="syncTaskQuery.taskCode"
                      allow-clear
                      placeholder="请输入任务编码"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      syncTaskSearchFilter.isSearchFilterHidden(1),
                  }"
                >
                  <AFormItem label="任务名称" class="m-0">
                    <AInput
                      v-model:value="syncTaskQuery.taskName"
                      allow-clear
                      placeholder="请输入任务名称"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      syncTaskSearchFilter.isSearchFilterHidden(2),
                  }"
                >
                  <AFormItem label="处理器" class="m-0">
                    <ASelect
                      v-model:value="syncTaskQuery.handlerCode"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="
                        handlerOptions.map((h: RowData) => ({
                          label: h.handlerName ?? h.label,
                          value: h.handlerCode ?? h.value,
                        }))
                      "
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      syncTaskSearchFilter.isSearchFilterHidden(3),
                  }"
                >
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="syncTaskQuery.status"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '启用', value: 1 },
                        { label: '停用', value: 0 },
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
              <PageSearchExpandButton
                v-if="syncTaskSearchFilter.showSearchFilterExpandToggle"
                :expanded="syncTaskSearchFilter.searchFilterExpanded"
                @click="syncTaskSearchFilter.toggleSearchFilterExpand"
              />
            </div>
          </div>
        </AForm>

        <AForm
          v-if="activeKey === 'fault'"
          :model="faultQuery"
          :label-col="{ span: 5, md: 7 }"
        >
          <div class="page-search-toolbar">
            <div class="page-search-toolbar__filters">
              <ARow :gutter="[16, 16]" wrap>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      faultSearchFilter.isSearchFilterHidden(0),
                  }"
                >
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
                          value: c.id ?? c.value,
                        }))
                      "
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      faultSearchFilter.isSearchFilterHidden(1),
                  }"
                >
                  <AFormItem label="物料编码" class="m-0">
                    <AInput
                      v-model:value="faultQuery.productCode"
                      allow-clear
                      placeholder="请输入物料编码"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      faultSearchFilter.isSearchFilterHidden(2),
                  }"
                >
                  <AFormItem label="产品型号" class="m-0">
                    <AInput
                      v-model:value="faultQuery.productModel"
                      allow-clear
                      placeholder="请输入产品型号"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      faultSearchFilter.isSearchFilterHidden(3),
                  }"
                >
                  <AFormItem label="故障描述" class="m-0">
                    <AInput
                      v-model:value="faultQuery.faultDesc"
                      allow-clear
                      placeholder="请输入故障描述"
                    />
                  </AFormItem>
                </ACol>
                <ACol
                  :span="24"
                  :md="12"
                  :lg="6"
                  :class="{
                    'page-search-toolbar__filter-col--collapsed':
                      faultSearchFilter.isSearchFilterHidden(4),
                  }"
                >
                  <AFormItem label="状态" class="m-0">
                    <ASelect
                      v-model:value="faultQuery.status"
                      allow-clear
                      placeholder="全部"
                      class="w-full"
                      :options="[
                        { label: '启用', value: 1 },
                        { label: '停用', value: 0 },
                      ]"
                    />
                  </AFormItem>
                </ACol>
              </ARow>
            </div>
            <div class="page-search-toolbar__actions">
              <AButton type="primary" @click="handleSearch">查询</AButton>
              <AButton @click="resetSearch">重置</AButton>
              <PageSearchExpandButton
                v-if="faultSearchFilter.showSearchFilterExpandToggle"
                :expanded="faultSearchFilter.searchFilterExpanded"
                @click="faultSearchFilter.toggleSearchFilterExpand"
              />
            </div>
          </div>
        </AForm>

        <AForm
          v-if="activeKey === 'roleTemplate'"
          :label-col="{ span: 5, md: 7 }"
        >
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
                      :options="
                        Object.entries(typeCodeLabelMap).map(
                          ([value, label]) => ({ value, label }),
                        )
                      "
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
                      :options="
                        hqCompanyOptions.map((c: RowData) => ({
                          label: c.companyName,
                          value: c.id,
                        }))
                      "
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
      :title="pageMenuTitle"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <ASpace wrap>
          <AButton v-if="activeKey === 'dict'" @click="onRefreshCache"
            >刷新字典缓存</AButton
          >
          <AButton v-if="activeKey === 'config'" @click="onRefreshCache"
            >刷新参数缓存</AButton
          >
          <AButton v-if="activeKey === 'notifyTemplate'" @click="onRefreshCache"
            >刷新模板缓存</AButton
          >
          <AButton
            v-if="activeKey === 'barcode'"
            type="primary"
            ghost
            :loading="loading"
            @click="onFullSyncBarcode"
          >
            执行同步任务
          </AButton>
          <AButton
            v-if="activeKey === 'syncTask'"
            type="primary"
            ghost
            @click="openSyncForm()"
            >新增任务</AButton
          >
          <AButton
            v-if="
              hasDictConfigRowActions ||
              activeKey === 'fault' ||
              activeKey === 'roleTemplate'
            "
            type="primary"
            @click="openForm(undefined, '新增')"
          >
            新增
          </AButton>
          <AButton
            v-if="activeKey === 'region'"
            type="primary"
            :disabled="!regionHqId"
            @click="openRegionForm()"
          >
            新增大区
          </AButton>
        </ASpace>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="columns"
        :data-source="rows"
        :locale="tableListLocale"
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
                onChange: onPageChange,
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
              {{ record.barcode || "-" }}
            </span>
          </template>
          <template
            v-else-if="
              column.key === 'templateCode' && activeKey === 'notifyTemplate'
            "
          >
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openNotifyView(record)"
              @keydown.enter.prevent="openNotifyView(record)"
            >
              {{ record.templateCode || "-" }}
            </span>
          </template>
          <template
            v-else-if="column.key === 'companyName' && activeKey === 'fault'"
          >
            <span
              class="table-list-entry-link"
              role="button"
              tabindex="0"
              @click="openFaultDetail(record)"
              @keydown.enter.prevent="openFaultDetail(record)"
            >
              {{ record.companyName || "-" }}
            </span>
          </template>
          <template
            v-else-if="
              column.key === 'status' && activeKey !== 'notifyTemplate'
            "
          >
            <ATag :color="tagColorEnabled(Number(record.status) === 1)">
              {{ Number(record.status) === 1 ? "正常" : "停用" }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'templateSource'">
            <ATag
              :color="
                record.templateSource === 'BUILT_IN' ? 'warning' : 'success'
              "
            >
              {{
                record.templateSource === "BUILT_IN" ? "内置模板" : "自定义模板"
              }}
            </ATag>
          </template>
          <template
            v-else-if="
              column.key === 'notifyEnabled' || column.key === 'overrideEnabled'
            "
          >
            <ATag
              :color="tagColorPositiveNeutral(Number(record[column.key]) === 1)"
            >
              <template v-if="column.key === 'overrideEnabled'">
                {{ Number(record[column.key]) === 1 ? "覆盖" : "回退" }}
              </template>
              <template v-else>
                {{ Number(record[column.key]) === 1 ? "开启" : "关闭" }}
              </template>
            </ATag>
          </template>
          <template
            v-else-if="
              column.key === 'typeCode' && activeKey === 'roleTemplate'
            "
          >
            {{ typeCodeLabel(record.typeCode) }}
          </template>
          <template
            v-else-if="
              column.key === 'dataScope' && activeKey === 'roleTemplate'
            "
          >
            {{ getRoleTemplateDataScopeLabel(record) }}
          </template>
          <template
            v-else-if="column.key === 'isAdmin' && activeKey === 'roleTemplate'"
          >
            <ATag v-if="record.isAdmin === 1" color="error">是</ATag>
            <span v-else>-</span>
          </template>
          <template
            v-else-if="
              column.key === 'actions' && activeKey === 'notifyTemplate'
            "
          >
            <ASpace :wrap="false">
              <AButton
                type="link"
                size="small"
                class="table-action-link--info"
                @click="onPreviewNotify(record)"
              >
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
                v-if="
                  record.templateSource === 'BUILT_IN' &&
                  !hasCustomNotifyTemplate(record.templateCode)
                "
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
          <template
            v-else-if="column.key === 'actions' && activeKey === 'syncTask'"
          >
            <ASpace :wrap="false">
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openSyncFormEdit(record)"
              >
                编辑
              </AButton>
              <APopconfirm
                :title="`确认立即执行任务“${record.taskName || record.taskCode || '-'}”吗？`"
                @confirm="onRunSyncTask(record)"
              >
                <AButton
                  type="link"
                  size="small"
                  class="table-action-link--warning"
                  :loading="loading"
                  >执行</AButton
                >
              </APopconfirm>
              <AButton
                type="link"
                size="small"
                class="table-action-link--info"
                @click="openLogDialog(record)"
              >
                日志
              </AButton>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeKey === 'fault'"
          >
            <ASpace :wrap="false">
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openForm(record, '编辑')"
              >
                编辑
              </AButton>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeKey === 'roleTemplate'"
          >
            <ASpace :size="4" :wrap="false">
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openForm(record, '编辑')"
              >
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
              <APopconfirm
                :title="`确认将角色模板「${record.roleName || record.roleKey || '-'}」全量同步到公司吗？`"
                @confirm="onSyncRoleTemplateRow(record)"
              >
                <AButton
                  type="link"
                  size="small"
                  class="table-action-link--warning"
                  >全量同步到公司</AButton
                >
              </APopconfirm>
              <APopconfirm title="确认删除？" @confirm="removeRow(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeKey === 'region'"
          >
            <ASpace :wrap="false">
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openRegionForm(record)"
              >
                编辑
              </AButton>
              <APopconfirm title="确认删除？" @confirm="removeRow(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && hasDictConfigRowActions"
          >
            <ASpace :wrap="false">
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openForm(record, '编辑')"
              >
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
      :title="`${tabOptions.find((t) => t.key === activeKey)?.label} — ${formTitle}`"
      :width="unifiedFormDrawerWidth"
    >
      <AForm
        ref="unifiedFormRef"
        layout="vertical"
        class="mt-8px"
        :model="formModel"
        :rules="unifiedFormRules as any"
      >
        <template v-if="activeKey === 'roleTemplate'">
          <div :class="unifiedFormGridClass">
            <AFormItem label="角色名称" name="roleName" required>
              <AInput
                v-model:value="formModel.roleName"
                placeholder="如：管理员"
              />
            </AFormItem>
            <AFormItem label="角色标识" name="roleKey" required>
              <AInput
                v-model:value="formModel.roleKey"
                placeholder="如：admin"
              />
            </AFormItem>
            <AFormItem label="类型编码" name="typeCode" required>
              <ASelect
                v-model:value="formModel.typeCode"
                show-search
                option-filter-prop="label"
                :options="
                  Object.entries(typeCodeLabelMap).map(([value, label]) => ({
                    value,
                    label,
                  }))
                "
                @change="() => onRoleTemplateDataScopeInit()"
              />
            </AFormItem>
            <AFormItem label="数据范围" name="dataScope" required>
              <ASelect
                v-model:value="formModel.dataScope"
                :options="roleTemplateScopeOptions"
                placeholder="请选择数据范围"
              />
              <div class="mt-4px text-12px text-gray-500">
                说明：{{
                  getRoleTemplateDataScopeLabel({
                    typeCode: formModel.typeCode,
                    dataScope: formModel.dataScope,
                  })
                }}
              </div>
            </AFormItem>
            <AFormItem label="管理员模板">
              <ASwitch
                v-model:checked="formModel.isAdmin"
                :checked-value="1"
                :un-checked-value="0"
              />
              <div class="mt-4px text-12px text-gray-500">
                每种公司类型最多保留一个管理员模板，用于初始化公司管理员角色。
              </div>
            </AFormItem>
            <AFormItem label="备注" class="adaptive-modal-form-grid__full">
              <ATextarea v-model:value="formModel.remark" :rows="3" />
            </AFormItem>
          </div>
        </template>
        <template v-else-if="activeKey === 'fault'">
          <ARow :gutter="16">
            <ACol :span="12">
              <AFormItem label="归属总部" name="companyId" required>
                <ASelect
                  v-model:value="formModel.companyId"
                  show-search
                  option-filter-prop="label"
                  placeholder="请选择归属总部"
                  :options="
                    faultCompanyOptions.map((c: RowData) => ({
                      label: c.companyName ?? c.label,
                      value: c.id ?? c.value,
                    }))
                  "
                />
              </AFormItem>
            </ACol>
            <ACol :span="12">
              <AFormItem label="状态" name="status" required>
                <ARadioGroup v-model:value="formModel.status">
                  <ARadio :value="1">启用</ARadio>
                  <ARadio :value="0">停用</ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
          </ARow>

          <ARow :gutter="16">
            <ACol :span="12">
              <AFormItem label="物料编码" name="productCode" required>
                <AInput
                  v-model:value="formModel.productCode"
                  placeholder="请输入物料编码"
                />
              </AFormItem>
            </ACol>
            <ACol :span="12">
              <AFormItem label="产品型号" name="productModel" required>
                <AInput
                  v-model:value="formModel.productModel"
                  placeholder="请输入产品型号（与物料编码至少填一项）"
                />
              </AFormItem>
            </ACol>
          </ARow>

          <ARow :gutter="16">
            <ACol :span="24">
              <AFormItem label="备注">
                <ATextarea
                  v-model:value="formModel.remark"
                  :rows="2"
                  placeholder="请输入备注"
                />
              </AFormItem>
            </ACol>
          </ARow>

          <div class="mb-8px mt-4px flex items-center justify-between font-600">
            <span>故障信息</span>
            <AButton type="link" size="small" @click="addFaultItem"
              >新增故障信息</AButton
            >
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
            <AFormItem
              label="故障描述"
              required
              :name="['faults', Number(index), 'faultDesc']"
              :rules="[
                {
                  required: true,
                  message: '故障描述不能为空',
                  trigger: 'blur',
                },
              ]"
            >
              <AInput
                v-model:value="item.faultDesc"
                placeholder="请输入故障描述"
              />
            </AFormItem>
            <div class="mb-8px flex items-center justify-between font-500">
              <span>维修说明</span>
              <AButton type="link" size="small" @click="addRepairOption(item)"
                >新增维修说明</AButton
              >
            </div>
            <div
              v-for="(repairDesc, repairIndex) in item.repairOptions"
              :key="`repair-${index}-${repairIndex}`"
              class="mb-8px flex"
            >
              <AFormItem
                required
                class="mb-0 min-w-0 flex-1"
                :name="[
                  'faults',
                  Number(index),
                  'repairOptions',
                  Number(repairIndex),
                ]"
                :rules="[
                  {
                    required: true,
                    message: '维修说明不能为空',
                    trigger: 'blur',
                  },
                ]"
              >
                <AInput
                  v-model:value="item.repairOptions[repairIndex]"
                  placeholder="请输入维修说明"
                />
              </AFormItem>
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
          <AFormItem
            name="faults"
            class="!mb-0 max-h-0 overflow-hidden p-0 opacity-0"
          >
            <span aria-hidden="true">.</span>
          </AFormItem>
        </template>
        <div v-else :class="unifiedFormGridClass">
          <template v-for="field in formFields()" :key="field.key">
            <AFormItem
              :label="field.label"
              :name="field.key"
              :required="isFormFieldRequired(field.key)"
              :class="{
                'adaptive-modal-form-grid__full': field.type === 'textarea',
              }"
            >
              <ARadioGroup
                v-if="field.type === 'radio'"
                v-model:value="formModel[field.key]"
              >
                <ARadio
                  v-for="option in getFormRadioOptions(field.key)"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </ARadio>
              </ARadioGroup>
              <AInputNumber
                v-else-if="field.type === 'number'"
                v-model:value="formModel[field.key]"
                class="w-full"
              />
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
        </div>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="formOpen = false">取消</AButton>
          <AButton type="primary" @click="submitForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="barcodeDetailOpen"
      title="条码档案详情"
      placement="right"
      :width="760"
      destroy-on-close
    >
      <ADescriptions bordered size="small" :column="1">
        <ADescriptionsItem
          v-for="item in barcodeDetailRows"
          :key="item.key"
          :label="item.label"
        >
          <div class="max-h-120px overflow-y-auto break-all">
            {{ item.value }}
          </div>
        </ADescriptionsItem>
      </ADescriptions>
      <template #footer>
        <ASpace>
          <AButton type="primary" @click="barcodeDetailOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="notifyFormOpen"
      :title="notifyFormTitle"
      :width="notifyFormDrawerWidth"
    >
      <AForm
        ref="notifyFormRef"
        layout="vertical"
        class="mt-8px"
        :model="notifyForm"
        :rules="notifyFormRules as any"
      >
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="模板编码" name="templateCode" required>
              <AInput v-model:value="notifyForm.templateCode" disabled />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="模板名称" name="templateName" required>
              <AInput
                v-model:value="notifyForm.templateName"
                :disabled="notifyFormReadonly"
              />
            </AFormItem>
          </ACol>
        </ARow>
        <ARow :gutter="16">
          <ACol :span="12">
            <AFormItem label="通知开关" name="notifyEnabled" required>
              <ARadioGroup
                v-model:value="notifyForm.notifyEnabled"
                :disabled="notifyFormReadonly"
              >
                <ARadio :value="1">开启</ARadio>
                <ARadio :value="0">关闭</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="覆盖开关" name="overrideEnabled" required>
              <ARadioGroup
                v-model:value="notifyForm.overrideEnabled"
                :disabled="notifyFormReadonly"
              >
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
              <ATextarea
                :value="String(notifyForm.variablesJson || '')"
                :rows="4"
                disabled
              />
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem label="备注">
              <ATextarea
                v-model:value="notifyForm.remark"
                :disabled="notifyFormReadonly"
                :rows="2"
              />
            </AFormItem>
          </ACol>
        </ARow>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="notifyFormOpen = false">取消</AButton>
          <AButton v-if="!notifyFormReadonly" @click="onPreviewNotifyForm"
            >预览</AButton
          >
          <AButton
            v-if="!notifyFormReadonly"
            type="primary"
            :loading="notifyFormSubmitting"
            @click="submitNotifyForm"
          >
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
          @click="
            notifyPreviewPayload && runNotifyPreview(notifyPreviewPayload)
          "
        >
          执行预览
        </AButton>
      </div>
      <ADescriptions
        v-if="notifyPreviewResult"
        :column="1"
        bordered
        size="small"
      >
        <ADescriptionsItem label="是否发送">
          {{ Number(notifyPreviewResult.notifyEnabled) === 1 ? "是" : "否" }}
        </ADescriptionsItem>
        <ADescriptionsItem label="实际来源">{{
          notifyPreviewResult.templateSource || "-"
        }}</ADescriptionsItem>
        <ADescriptionsItem label="标题">{{
          notifyPreviewResult.title || "-"
        }}</ADescriptionsItem>
        <ADescriptionsItem label="摘要">{{
          notifyPreviewResult.summary || "-"
        }}</ADescriptionsItem>
        <ADescriptionsItem label="路由类型">{{
          notifyPreviewResult.routeType || "-"
        }}</ADescriptionsItem>
        <ADescriptionsItem label="路由值">{{
          notifyPreviewResult.routeValue || "-"
        }}</ADescriptionsItem>
      </ADescriptions>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="notifyPreviewOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="channelsOpen"
      :title="`渠道配置 - ${channelsTemplateCode || ''}`"
      :width="channelsDrawerWidth"
    >
      <ASpin :spinning="channelsLoading">
        <div class="mb-12px">
          <AButton
            v-if="!channelsReadonly"
            type="primary"
            ghost
            size="small"
            @click="addChannelRow"
            >新增渠道</AButton
          >
        </div>
        <div
          v-for="(item, index) in channelsRows"
          :key="`channel-${index}`"
          class="mb-12px border border-gray-200 rounded-6px border-solid bg-gray-50 p-12px dark:bg-dark-900"
        >
          <div class="mb-8px flex items-center justify-between font-600">
            <span>渠道 {{ Number(index) + 1 }}</span>
            <APopconfirm
              v-if="!channelsReadonly"
              title="确认删除该渠道配置？"
              @confirm="removeChannelRow(Number(index))"
            >
              <AButton type="link" size="small" danger>删除</AButton>
            </APopconfirm>
          </div>
          <ARow :gutter="16">
            <ACol :span="8">
              <AFormItem
                label="渠道类型"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
                class="mb-8px"
              >
                <ASelect
                  v-model:value="item.channelType"
                  :disabled="channelsReadonly"
                  :options="[
                    { label: '小程序订阅消息', value: 'MP_SUBSCRIBE' },
                    { label: '短信', value: 'SMS' },
                    { label: '邮件', value: 'EMAIL' },
                  ]"
                />
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem
                label="渠道开关"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
                class="mb-8px"
              >
                <ARadioGroup
                  v-model:value="item.channelEnabled"
                  :disabled="channelsReadonly"
                >
                  <ARadio :value="1">开启</ARadio>
                  <ARadio :value="0">关闭</ARadio>
                </ARadioGroup>
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem
                label="小程序场景"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
                class="mb-8px"
              >
                <ASelect
                  v-model:value="item.channelScene"
                  :disabled="
                    channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'
                  "
                  :options="[
                    { label: 'B 端小程序', value: 'B' },
                    { label: 'C 端小程序', value: 'C' },
                  ]"
                />
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem
                label="模板 ID"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
                class="mb-8px"
              >
                <AInput
                  v-model:value="item.templateId"
                  :disabled="
                    channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'
                  "
                />
              </AFormItem>
            </ACol>
            <ACol :span="16">
              <AFormItem
                label="页面路径模板"
                :label-col="{ span: 8 }"
                :wrapper-col="{ span: 16 }"
                class="mb-8px"
              >
                <AInput
                  v-model:value="item.pagePathTemplate"
                  :disabled="
                    channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'
                  "
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
              {
                title: '操作',
                key: 'actions',
                width: NOTIFY_FIELD_MAPPING_ACTION_COL_W,
                fixed: 'right' as const,
              },
            ]"
            :data-source="item.fieldMapping || []"
            :pagination="false"
            :scroll="{ x: 'max-content' }"
            size="small"
            row-key="field"
          >
            <template
              #bodyCell="{ column, record: mapping, index: mappingIndex }"
            >
              <template v-if="column.key === 'field'">
                <AInput
                  v-model:value="mapping.field"
                  :disabled="
                    channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'
                  "
                  placeholder="例如 thing1"
                />
              </template>
              <template v-else-if="column.key === 'value'">
                <AInput
                  v-model:value="mapping.value"
                  :disabled="
                    channelsReadonly || item.channelType !== 'MP_SUBSCRIBE'
                  "
                  placeholder="例如 ${orderNo}"
                />
              </template>
              <template v-else-if="column.key === 'actions'">
                <AButton
                  v-if="
                    !channelsReadonly && item.channelType === 'MP_SUBSCRIBE'
                  "
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
          <ARow :gutter="16" class="mt-8px">
            <ACol :span="24">
              <AFormItem label="备注" layout="vertical" class="mb-0">
                <ATextarea
                  v-model:value="item.remark"
                  :disabled="channelsReadonly"
                  :rows="2"
                  class="w-full"
                />
              </AFormItem>
            </ACol>
          </ARow>
        </div>
      </ASpin>
      <template #footer>
        <ASpace>
          <AButton @click="channelsOpen = false">取消</AButton>
          <AButton
            v-if="!channelsReadonly"
            type="primary"
            :loading="channelsLoading"
            @click="saveChannels"
          >
            保存
          </AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="syncFormOpen" :title="syncFormTitle" :width="420">
      <AForm
        ref="syncTaskFormRef"
        layout="vertical"
        class="mt-8px"
        :model="syncFormModel"
        :rules="syncTaskFormRules as any"
      >
        <AFormItem label="任务编码" name="taskCode" required>
          <AInput
            v-model:value="syncFormModel.taskCode"
            :disabled="!!syncFormModel.id"
            placeholder="请输入任务编码"
          />
        </AFormItem>
        <AFormItem label="任务名称" name="taskName" required>
          <AInput
            v-model:value="syncFormModel.taskName"
            placeholder="请输入任务名称"
          />
        </AFormItem>
        <AFormItem label="处理器" name="handlerCode" required>
          <ASelect
            v-model:value="syncFormModel.handlerCode"
            placeholder="请选择处理器"
            class="w-full"
            :options="
              handlerOptions.map((h: RowData) => ({
                label: h.handlerName,
                value: h.handlerCode,
              }))
            "
          />
        </AFormItem>
        <AFormItem label="Cron 表达式" name="cronExpression" required>
          <AInput
            v-model:value="syncFormModel.cronExpression"
            placeholder="请输入Cron表达式，例如 0 0 2 * * ?"
          />
        </AFormItem>
        <AFormItem label="状态">
          <ARadioGroup v-model:value="syncFormModel.status">
            <ARadio :value="1">启用</ARadio>
            <ARadio :value="0">停用</ARadio>
          </ARadioGroup>
        </AFormItem>
        <AFormItem label="备注">
          <ATextarea
            v-model:value="syncFormModel.remark"
            :rows="2"
            placeholder="请输入备注"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="syncFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitSyncForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="logOpen" :title="logDialogTitle" :width="1280">
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
              { label: 'FAILED', value: 'FAILED' },
            ]"
            @change="handleLogStatusChange"
          />
        </AFormItem>
      </AForm>
      <ATable
        :columns="[
          { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
          {
            title: '触发类型',
            dataIndex: 'triggerType',
            key: 'triggerType',
            width: 120,
          },
          {
            title: '触发人',
            dataIndex: 'triggerUserId',
            key: 'triggerUserId',
            width: 140,
          },
          {
            title: '开始时间',
            dataIndex: 'startTime',
            key: 'startTime',
            width: 176,
          },
          {
            title: '结束时间',
            dataIndex: 'endTime',
            key: 'endTime',
            width: 176,
          },
          {
            title: '数据开始时间',
            dataIndex: 'dataStartTime',
            key: 'dataStartTime',
            width: 176,
          },
          {
            title: '数据结束时间',
            dataIndex: 'dataEndTime',
            key: 'dataEndTime',
            width: 176,
          },
          {
            title: '执行信息',
            dataIndex: 'message',
            key: 'message',
            width: 420,
            ellipsis: { showTitle: true },
          },
        ]"
        :data-source="logRows"
        :loading="logLoading"
        size="small"
        row-key="id"
        :scroll="{ x: 1484, y: 'calc(100vh - 280px)' }"
        :pagination="{
          current: logPage.pageNum,
          pageSize: logPage.pageSize,
          total: logTotal,
          showSizeChanger: true,
          onChange: onLogPageChange,
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <ATag :color="syncLogStatusTagColor(record.status)">{{
              record.status || "-"
            }}</ATag>
          </template>
          <template v-else-if="column.key === 'triggerType'">
            <ATag
              :color="record.triggerType === 'MANUAL' ? 'warning' : 'blue'"
              >{{ record.triggerType || "-" }}</ATag
            >
          </template>
          <template v-else-if="column.key === 'triggerUserId'">
            {{ syncLogTriggerUserLabel(record) }}
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
        v-model:expanded-keys="menuExpandedKeys"
        checkable
        :tree-data="menuTreeData"
        :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
        class="overflow-auto"
      />
      <template #footer>
        <ASpace :size="16">
          <AButton @click="menuAssignOpen = false">取消</AButton>
          <AButton
            type="primary"
            :loading="menuAssignSubmitting"
            @click="submitRoleTemplateMenuAssign"
            >确定</AButton
          >
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="regionFormOpen"
      :title="`大区 — ${regionFormTitle}`"
      :width="420"
    >
      <AForm
        ref="advRegionFormRef"
        layout="vertical"
        class="mt-8px"
        :model="regionForm"
        :rules="advRegionFormRules as any"
      >
        <AFormItem label="大区编码" name="regionCode" required>
          <AInput v-model:value="regionForm.regionCode" />
        </AFormItem>
        <AFormItem label="大区名称" name="regionName" required>
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

    <ADrawer
      v-model:open="faultDetailOpen"
      title="查看故障与维修配置"
      placement="right"
      :width="720"
    >
      <template v-if="faultDetail">
        <ADescriptions :column="2" bordered size="small">
          <ADescriptionsItem label="归属总部">{{
            faultDetail.companyName || "-"
          }}</ADescriptionsItem>
          <ADescriptionsItem label="状态">{{
            Number(faultDetail.status) === 1 ? "启用" : "停用"
          }}</ADescriptionsItem>
          <ADescriptionsItem label="物料编码">{{
            faultDetail.productCode || "-"
          }}</ADescriptionsItem>
          <ADescriptionsItem label="产品型号">{{
            faultDetail.productModel || "-"
          }}</ADescriptionsItem>
          <ADescriptionsItem label="备注" :span="2">{{
            faultDetail.remark || "-"
          }}</ADescriptionsItem>
        </ADescriptions>
        <div class="mb-8px mt-16px font-600">故障信息</div>
        <div
          v-for="(item, index) in faultDetail.faults || []"
          :key="`detail-fault-${index}`"
          class="mb-12px border border-gray-200 rounded-6px border-solid p-12px"
        >
          <div class="mb-8px font-600">{{ item.faultDesc || "-" }}</div>
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
    </ADrawer>
  </div>
</template>
