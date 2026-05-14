<script setup lang="ts">
/**
 * 组织与客商：公司/类型、区域、合同、客户导入等多 Tab 业务聚合页（对接 org 域接口）。
 */
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  tagColorEnabled,
  tagColorPositiveNeutral,
} from "@/constants/list-status-tag";
import {
  addCompany,
  addCompanyType,
  addFirstSecondRelation,
  addHqFirstContract,
  addRegion,
  assignTypeCodeMenus,
  deleteCompany,
  deleteCompanyType,
  deleteFirstSecondRelation,
  deleteHqFirstContract,
  deleteRegion,
  getExternalCompanyImportPreview,
  importCrmFirstSecondRelation,
  importCrmHqFirstContract,
  listAreaOptions,
  listCompany,
  listCompanyType,
  listCrmFirstSecondRelationImport,
  listCrmHqFirstContractImport,
  listExternalCompany,
  listFirstSecondRelation,
  listHqFirstContract,
  listRegion,
  typeCodeMenuIds,
  typeCodeMenuTree,
  updateCompany,
  updateCompanyType,
  updateHqFirstContract,
  updateRegion,
} from "@/service/api";
import type { SysCompanyDTO, SysCompanyQuery } from "@/service/api";
import { getResponseMsg } from "@/service/request/shared";
import { useTableScroll } from "@/hooks/common/table";

type RowData = Record<string, any>;
type HqFormModel = {
  id?: number;
  hqCompanyId?: number;
  firstCompanyId?: number;
  regionId?: number;
  contractTime?: string;
  status: number;
};
type FsFormModel = {
  id?: number;
  firstCompanyId?: number;
  secondCompanyId?: number;
};
type TabKey =
  | "companyType"
  | "company"
  | "hqFirst"
  | "firstSecond"
  | "external"
  | "area";

// 表格与列表通用加载态
const loading = ref(false);
// 当前组织管理子 Tab
const activeTab = ref<TabKey>("companyType");
// 当前 Tab 表格数据
const rows = ref<RowData[]>([]);
// 当前 Tab 分页总数（部分 Tab 为前端全长）
const total = ref(0);
const route = useRoute();

// 路由 name 与子 Tab 映射
const ROUTE_NAME_TO_TAB_KEY: Record<string, TabKey> = {
  "org_company-type": "companyType",
  org_company: "company",
  org_contract: "hqFirst",
  org_region: "area",
};

// 公司列表查询参数
const companyQuery = reactive<SysCompanyQuery>({
  pageNum: 1,
  pageSize: 10,
  companyName: undefined,
  typeCode: undefined,
  category: undefined,
  status: undefined,
});

// 总部-一级签约列表查询
const hqFirstQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  hqCompanyId: undefined as number | undefined,
});
// 一级-二级关系列表查询
const firstSecondQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  targetCompanyId: undefined as number | undefined,
  firstCompanyId: undefined as number | undefined,
});
// 外部公司列表查询
const externalQuery = reactive({ pageNum: 1, pageSize: 10, companyName: "" });

/** 与 jasic-ui `views/org/contract/index.vue` CRM 总部一级导入筛选一致 */
const crmHqQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  hqCompanyId: undefined as number | undefined,
  firstCompanyId: undefined as number | undefined,
  regionId: undefined as number | undefined,
  kunnr: "",
  showAbnormal: false,
});

/** 与 jasic-ui 一级二级 CRM 导入筛选一致 */
const crmFsQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  targetCompanyId: undefined as number | undefined,
  firstCompanyId: undefined as number | undefined,
  secondCompanyId: undefined as number | undefined,
  firstCompanyCode: "",
  secondCompanyCode: "",
  showAbnormal: false,
});

// 总部公司下拉选项
const hqCompanyOptions = ref<RowData[]>([]);
// 一级公司下拉选项
const firstCompanyOptions = ref<RowData[]>([]);
// 二级公司下拉选项
const secondCompanyOptions = ref<RowData[]>([]);
// CRM 导入大区下拉选项
const crmImportRegionOptions = ref<RowData[]>([]);
// 总部表单大区下拉选项
const hqFormRegionOptions = ref<RowData[]>([]);

// 总部表单打开状态
const hqFormOpen = ref(false);
// 总部表单数据
const hqForm = reactive<HqFormModel>({
  id: undefined,
  hqCompanyId: undefined,
  firstCompanyId: undefined,
  regionId: undefined,
  contractTime: "",
  status: 1,
});

// 一级表单打开状态
const fsFormOpen = ref(false);
// 一级表单数据
const fsForm = reactive<FsFormModel>({
  id: undefined,
  firstCompanyId: undefined,
  secondCompanyId: undefined,
});

// 公司表单打开状态
const companyFormOpen = ref(false);
// 公司表单数据
const companyForm = reactive<Partial<SysCompanyDTO> & { id?: number }>({
  id: undefined,
  companyName: "",
  companyShortName: "",
  companyCode: "",
  typeCode: "",
  contactName: "",
  contactPhone: "",
  servicePhone: "",
  sourceType: "MANUAL",
  provinceCode: "",
  cityCode: "",
  districtCode: "",
  detailAddress: "",
  adminUsername: "",
  salesOrg: "",
  status: 1,
  remark: "",
});

// 预览打开状态
const previewOpen = ref(false);
// 预览客户ID
const previewCustId = ref<string | number>("");
// 预览加载状态
const previewLoading = ref(false);
// 预览数据
const previewData = ref<unknown>(null);
// 公司CRM导入打开状态
const companyCrmImportOpen = ref(false);
// 公司CRM导入加载状态
const companyCrmImportLoading = ref(false);
// 公司CRM导入行数据
const companyCrmImportRows = ref<RowData[]>([]);
// 公司CRM导入总条数
const companyCrmImportTotal = ref(0);
// 公司CRM导入查询参数
const companyCrmImportQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  companyCode: "",
  companyName: "",
  custState: undefined as number | undefined,
});
// 外部公司状态选项
const externalStateOptions = [
  { value: 0, label: "待审核" },
  { value: 1, label: "审核通过" },
  { value: 2, label: "审核不通过" },
  { value: 3, label: "注销" },
  { value: 4, label: "资料已保存" },
  { value: 5, label: "申请注销" },
  { value: 6, label: "资料未填写" },
  { value: 9, label: "删除" },
];

// 区域查询公司ID
const regionQueryCompanyId = ref<number | undefined>(undefined);
// 区域表单打开状态
const regionFormOpen = ref(false);
// 区域表单数据
const regionForm = reactive({
  id: undefined as number | undefined,
  targetCompanyId: undefined as number | undefined,
  regionCode: "",
  regionName: "",
  remark: "",
});

// 总部快照ID列表
const selectedHqSnapshotIds = ref<Array<string | number>>([]);
// 一级快照ID列表
const selectedFsSnapshotIds = ref<Array<string | number>>([]);
// 总部CRM导入打开状态
const hqCrmImportOpen = ref(false);
// 公司类型选项
const companyTypeOptions = ref<RowData[]>([]);
// 公司类型表单打开状态
const companyTypeFormOpen = ref(false);
// 公司类型表单数据
const companyTypeForm = reactive({
  id: undefined as number | undefined,
  typeName: "",
  typeCode: "",
  subjectType: "SERVICE",
  orderNum: 0,
  remark: "",
});
// 公司类型菜单打开状态
const companyTypeMenuOpen = ref(false);
// 公司类型菜单加载状态
const companyTypeMenuLoading = ref(false);
// 公司类型菜单提交状态
const companyTypeMenuSubmitting = ref(false);
// 公司类型菜单标题
const companyTypeMenuTitle = ref("");
// 公司类型菜单树数据
const companyTypeMenuTreeData = ref<any[]>([]);
// 公司类型菜单选中键
const companyTypeMenuCheckedKeys = ref<Array<string | number>>([]);
// 公司类型菜单类型编码
const companyTypeMenuTypeCode = ref("");

// 省市区下拉选项（公司表单）
const provinceOptions = ref<RowData[]>([]);
// 市下拉选项
const cityOptions = ref<RowData[]>([]);
// 区下拉选项
const districtOptions = ref<RowData[]>([]);

/**
 * 作用：根据类型编码取公司类型的主体类型（HQ/SERVICE 等）。
 * @param typeCode - 类型编码
 * @returns 主体类型字符串，未匹配返回空串
 */
function getCompanySubjectType(typeCode?: string) {
  const target = String(typeCode || "");
  if (!target) return "";
  // 匹配公司类型选项
  const matched = companyTypeOptions.value.find(
    (item) => String(item.typeCode || "") === target,
  );
  return String(matched?.subjectType || "");
}

/**
 * 作用：判断是否为总部类型公司。
 * @param typeCode - 类型编码
 * @returns 是否总部
 */
function isCompanyHqType(typeCode?: string) {
  return getCompanySubjectType(typeCode) === "HQ";
}

/**
 * 作用：根据类型编码解析类型显示名称。
 * @param typeCode - 类型编码
 * @returns 类型名称或空串
 */
function getCompanyTypeLabel(typeCode?: string) {
  const target = String(typeCode || "");
  if (!target) return "";
  const matched = companyTypeOptions.value.find(
    (item) => String(item.typeCode || "") === target,
  );
  return String(matched?.typeName || "");
}

/**
 * 作用：拼接公司省/市/名称为展示用地区字符串。
 * @param record - 表格行
 * @returns 展示文案或 -
 */
function formatCompanyRegion(record: RowData) {
  const parts = [record.provinceName, record.cityName, record.districtName]
    .map((item) => String(item || "").trim())
    .filter(Boolean);
  return parts.length ? parts.join("/") : "-";
}

// 是否为签约相关 Tab（总部一级 / 一级二级）
const isContractPageTab = computed(
  () => activeTab.value === "hqFirst" || activeTab.value === "firstSecond",
);

// 按当前 Tab 切换表格列定义
const columns = computed(() => {
  switch (activeTab.value) {
    case "company":
      return [
        { title: "ID", dataIndex: "id", key: "id", width: 70 },
        {
          title: "公司名称",
          dataIndex: "companyName",
          key: "companyName",
          width: 200,
        },
        {
          title: "公司简称",
          dataIndex: "companyShortName",
          key: "companyShortName",
          width: 140,
        },
        {
          title: "公司编码",
          dataIndex: "companyCode",
          key: "companyCode",
          width: 140,
        },
        {
          title: "公司类型",
          dataIndex: "typeCode",
          key: "typeCode",
          width: 120,
        },
        {
          title: "主体类型",
          dataIndex: "subjectType",
          key: "subjectType",
          width: 100,
        },
        {
          title: "来源",
          dataIndex: "sourceType",
          key: "sourceType",
          width: 90,
        },
        {
          title: "联系人",
          dataIndex: "contactName",
          key: "contactName",
          width: 120,
        },
        {
          title: "联系电话",
          dataIndex: "contactPhone",
          key: "contactPhone",
          width: 140,
        },
        {
          title: "客服电话",
          dataIndex: "servicePhone",
          key: "servicePhone",
          width: 140,
        },
        {
          title: "销售组织",
          dataIndex: "salesOrg",
          key: "salesOrg",
          width: 120,
        },
        { title: "地区", dataIndex: "region", key: "region", width: 180 },
        {
          title: "详细地址",
          dataIndex: "detailAddress",
          key: "detailAddress",
          ellipsis: true,
          width: 220,
        },
        {
          title: "地理解析",
          dataIndex: "geocodeStatus",
          key: "geocodeStatus",
          width: 100,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 80 },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 160,
        },
        {
          title: "操作",
          dataIndex: "actions",
          key: "actions",
          width: 160,
          fixed: "right" as const,
        },
      ];
    case "hqFirst":
      return [
        { title: "ID", dataIndex: "id", key: "id", width: 70 },
        {
          title: "总部",
          dataIndex: "hqCompanyName",
          key: "hqCompanyName",
          width: 180,
        },
        {
          title: "一级网点",
          dataIndex: "firstCompanyName",
          key: "firstCompanyName",
          width: 180,
        },
        {
          title: "大区",
          dataIndex: "regionName",
          key: "regionName",
          width: 140,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 80 },
        {
          title: "签约时间",
          dataIndex: "contractTime",
          key: "contractTime",
          width: 170,
        },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 170,
        },
        {
          title: "操作",
          dataIndex: "actions",
          key: "actions",
          width: 160,
          fixed: "right" as const,
        },
      ];
    case "firstSecond":
      return [
        { title: "ID", dataIndex: "id", key: "id", width: 70 },
        {
          title: "一级",
          dataIndex: "firstCompanyName",
          key: "firstCompanyName",
          width: 180,
        },
        {
          title: "二级",
          dataIndex: "secondCompanyName",
          key: "secondCompanyName",
          width: 180,
        },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 170,
        },
        {
          title: "操作",
          dataIndex: "actions",
          key: "actions",
          width: 120,
          fixed: "right" as const,
        },
      ];
    case "external":
      return [
        {
          title: "名称",
          dataIndex: "companyName",
          key: "companyName",
          width: 200,
        },
        {
          title: "编码",
          dataIndex: "companyCode",
          key: "companyCode",
          width: 140,
        },
        {
          title: "联系人",
          dataIndex: "contactName",
          key: "contactName",
          width: 120,
        },
        {
          title: "电话",
          dataIndex: "contactPhone",
          key: "contactPhone",
          width: 140,
        },
        { title: "状态", dataIndex: "status", key: "status", width: 80 },
        {
          title: "操作",
          dataIndex: "actions",
          key: "actions",
          width: 100,
          fixed: "right" as const,
        },
      ];
    case "area":
      return [
        { title: "ID", dataIndex: "id", key: "id", width: 70 },
        {
          title: "大区编码",
          dataIndex: "regionCode",
          key: "regionCode",
          width: 120,
        },
        {
          title: "大区名称",
          dataIndex: "regionName",
          key: "regionName",
          width: 200,
        },
        { title: "备注", dataIndex: "remark", key: "remark" },
        {
          title: "创建时间",
          dataIndex: "createTime",
          key: "createTime",
          width: 160,
        },
        {
          title: "操作",
          dataIndex: "actions",
          key: "actions",
          width: 160,
          fixed: "right" as const,
        },
      ];
    case "companyType":
      return [
        {
          title: "类型名称",
          dataIndex: "typeName",
          key: "typeName",
          width: 200,
        },
        {
          title: "类型编码",
          dataIndex: "typeCode",
          key: "typeCode",
          width: 160,
        },
        {
          title: "主体类型",
          dataIndex: "subjectType",
          key: "subjectType",
          width: 120,
        },
        { title: "备注", dataIndex: "remark", key: "remark", ellipsis: true },
        {
          title: "操作",
          dataIndex: "actions",
          key: "actions",
          width: 220,
          fixed: "right" as const,
        },
      ];
    default:
      return [];
  }
});

/** 与 jasic-ui `contract/index.vue` 总部一级 CRM 导入表列一致 */
const crmHqImportColumns = [
  { title: "客户编码", dataIndex: "kunnr", key: "kunnr", width: 120 },
  {
    title: "CRM企业名称",
    dataIndex: "crmCompanyName",
    key: "crmCompanyName",
    ellipsis: true,
    width: 180,
  },
  { title: "销售组织", dataIndex: "salesOrg", key: "salesOrg", width: 120 },
  { title: "CRM大区", key: "hqCrmRegion", ellipsis: true, width: 180 },
  {
    title: "一级公司",
    dataIndex: "firstCompanyName",
    key: "firstCompanyName",
    ellipsis: true,
    width: 160,
  },
  {
    title: "本地大区",
    dataIndex: "localRegionName",
    key: "localRegionName",
    ellipsis: true,
    width: 150,
  },
  { title: "CRM状态", key: "hqCrmAlive", width: 100 },
  { title: "导入状态", key: "hqCrmImportSts", width: 110 },
  {
    title: "说明",
    dataIndex: "matchRemark",
    key: "matchRemark",
    ellipsis: true,
    width: 200,
  },
];

/** 与 jasic-ui 一级二级 CRM 导入表列一致 */
const crmFsImportColumns = [
  {
    title: "一级CRM ID",
    dataIndex: "firstCustId",
    key: "firstCustId",
    width: 110,
  },
  {
    title: "一级编码",
    dataIndex: "firstCompanyCode",
    key: "firstCompanyCode",
    width: 120,
  },
  {
    title: "一级名称",
    dataIndex: "firstCompanyName",
    key: "firstCompanyName",
    ellipsis: true,
    width: 160,
  },
  {
    title: "二级CRM ID",
    dataIndex: "secondCustId",
    key: "secondCustId",
    width: 110,
  },
  {
    title: "二级编码",
    dataIndex: "secondCompanyCode",
    key: "secondCompanyCode",
    width: 120,
  },
  {
    title: "二级名称",
    dataIndex: "secondCompanyName",
    key: "secondCompanyName",
    ellipsis: true,
    width: 160,
  },
  {
    title: "本地一级",
    dataIndex: "localFirstCompanyName",
    key: "localFirstCompanyName",
    ellipsis: true,
    width: 140,
  },
  {
    title: "本地二级",
    dataIndex: "localSecondCompanyName",
    key: "localSecondCompanyName",
    ellipsis: true,
    width: 140,
  },
  {
    title: "来源更新时间",
    dataIndex: "crmOperTime",
    key: "crmOperTime",
    width: 160,
  },
  { title: "导入状态", key: "fsCrmImportSts", width: 110 },
  {
    title: "说明",
    dataIndex: "matchRemark",
    key: "matchRemark",
    ellipsis: true,
    width: 220,
  },
];

// 主表格当前展示的列（与 columns 同步）
const displayColumns = computed(() => columns.value);

// 主表格横向滚动宽度估计值
const crmTableScrollX = computed(() => {
  if (activeTab.value === "company") return 900;
  return 1100;
});

// 表格容器与滚动配置（依赖横向宽度 computed）
const { tableWrapperRef, scrollConfig } = useTableScroll(crmTableScrollX);

/**
 * 作用：解析列表接口返回的行数组。
 * @param data - 分页或数组
 * @returns 行数据
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
 * 作用：懒加载总部/一级/二级公司下拉数据（CRM 相关共用）。
 * @param 无
 * @returns 返回 Promise，选项就绪后结束；已缓存则无请求
 */
async function loadCompanyOptionsForCrm() {
  if (
    hqCompanyOptions.value.length &&
    firstCompanyOptions.value.length &&
    secondCompanyOptions.value.length
  ) {
    return;
  }
  const base = { pageNum: 1, pageSize: 999 };
  const [hqRes, firstRes, secondRes] = await Promise.all([
    listCompany({ ...base, category: "HQ" }),
    listCompany({ ...base, category: "FIRST_LEVEL" }),
    listCompany({ ...base, category: "SECOND_LEVEL" }),
  ]);
  hqCompanyOptions.value = pickRows(hqRes.data);
  firstCompanyOptions.value = pickRows(firstRes.data);
  secondCompanyOptions.value = pickRows(secondRes.data);
}

/**
 * 作用：按总部 ID 加载 CRM 导入用大区下拉选项。
 * @param hqCompanyId - 总部公司 ID，空则清空选项
 * @returns 返回 Promise，大区列表写入后结束（失败时清空选项）
 */
async function loadCrmImportRegions(hqCompanyId: number | undefined) {
  if (!hqCompanyId) {
    crmImportRegionOptions.value = [];
    return;
  }
  try {
    const { data } = await listRegion(hqCompanyId);
    crmImportRegionOptions.value = Array.isArray(data) ? data : pickRows(data);
  } catch {
    crmImportRegionOptions.value = [];
  }
}

/**
 * 作用：拉取并缓存公司类型列表到公司类型选项。
 * @param 无
 * @returns 返回 Promise，请求写入 companyTypeOptions 后结束
 */
async function ensureCompanyTypeOptions() {
  const { data } = await listCompanyType();
  const list = pickRows(data);
  companyTypeOptions.value = list;
}

/**
 * 作用：按当前 activeTab 加载对应列表数据。
 * @param 无
 * @returns 返回 Promise，当前 Tab 列表更新后结束
 */
async function loadList() {
  loading.value = true;
  try {
    switch (activeTab.value) {
      case "company": {
        const { data } = await listCompany({
          ...companyQuery,
          companyName: companyQuery.companyName || undefined,
          typeCode: companyQuery.typeCode || undefined,
          status: companyQuery.status,
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case "hqFirst": {
        if (!hqFirstQuery.hqCompanyId) {
          rows.value = [];
          total.value = 0;
          break;
        }
        const { data } = await listHqFirstContract({
          pageNum: hqFirstQuery.pageNum,
          pageSize: hqFirstQuery.pageSize,
          targetCompanyId: hqFirstQuery.hqCompanyId,
          hqCompanyId: hqFirstQuery.hqCompanyId,
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case "firstSecond": {
        if (!firstSecondQuery.targetCompanyId) {
          rows.value = [];
          total.value = 0;
          break;
        }
        const { data } = await listFirstSecondRelation({
          pageNum: firstSecondQuery.pageNum,
          pageSize: firstSecondQuery.pageSize,
          targetCompanyId: firstSecondQuery.targetCompanyId,
          firstCompanyId: firstSecondQuery.firstCompanyId,
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case "external": {
        const { data } = await listExternalCompany({
          pageNum: externalQuery.pageNum,
          pageSize: externalQuery.pageSize,
          companyName: externalQuery.companyName.trim() || undefined,
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case "area": {
        if (!regionQueryCompanyId.value) {
          rows.value = [];
          total.value = 0;
          break;
        }
        const { data } = await listRegion(regionQueryCompanyId.value);
        const list = Array.isArray(data) ? data : pickRows(data);
        rows.value = list;
        total.value = list.length;
        break;
      }
      case "companyType": {
        await ensureCompanyTypeOptions();
        rows.value = companyTypeOptions.value;
        total.value = rows.value.length;
        break;
      }
      default:
        break;
    }
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：大区 Tab 下触发刷新列表。
 * @param 无
 * @returns {void} 无
 */
function handleRegionSearch() {
  loadList();
}

/**
 * 作用：打开大区新增/编辑抽屉并回填表单。
 * @param record - 编辑时传入行数据
 */
function openRegionForm(record?: RowData) {
  if (!regionQueryCompanyId.value && !record?.companyId) {
    window.$message?.warning?.("请先选择总部公司");
    return;
  }
  regionForm.id = record?.id;
  regionForm.targetCompanyId = Number(
    record?.companyId ?? regionQueryCompanyId.value,
  );
  regionForm.regionCode = record?.regionCode ?? "";
  regionForm.regionName = record?.regionName ?? "";
  regionForm.remark = record?.remark ?? "";
  regionFormOpen.value = true;
}

/**
 * 作用：提交大区表单（新增或更新）。
 */
async function submitRegionForm() {
  if (!regionForm.regionName.trim()) {
    window.$message?.warning?.("请输入大区名称");
    return;
  }
  const payload = {
    id: regionForm.id,
    targetCompanyId: regionForm.targetCompanyId,
    regionCode: regionForm.regionCode.trim() || undefined,
    regionName: regionForm.regionName.trim(),
    remark: regionForm.remark,
  };
  if (payload.id) await updateRegion(payload);
  else await addRegion(payload);
  window.$message?.success?.("操作成功");
  regionFormOpen.value = false;
  await loadList();
}

/**
 * 作用：删除大区记录。
 * @param record - 表格行
 */
async function removeRegion(record: RowData) {
  await deleteRegion(record.id, {
    targetCompanyId: Number(record?.companyId ?? regionQueryCompanyId.value),
  });
  window.$message?.success?.("删除成功");
  await loadList();
}

/**
 * 作用：加载总部一级 CRM 导入快照表格数据。
 */
async function loadCrmHqRows() {
  if (!crmHqQuery.hqCompanyId) {
    rows.value = [];
    total.value = 0;
    selectedHqSnapshotIds.value = [];
    return;
  }
  loading.value = true;
  try {
    const { data } = await listCrmHqFirstContractImport({
      pageNum: crmHqQuery.pageNum,
      pageSize: crmHqQuery.pageSize,
      targetCompanyId: crmHqQuery.hqCompanyId,
      hqCompanyId: crmHqQuery.hqCompanyId,
      firstCompanyId: crmHqQuery.firstCompanyId,
      regionId: crmHqQuery.regionId,
      kunnr: crmHqQuery.kunnr.trim() || undefined,
      showAbnormal: crmHqQuery.showAbnormal,
    });
    rows.value = pickRows(data);
    total.value = pickTotal(data);
    selectedHqSnapshotIds.value = [];
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：加载一级二级 CRM 导入快照表格数据。
 */
async function loadCrmFsRows() {
  if (!crmFsQuery.targetCompanyId) {
    rows.value = [];
    total.value = 0;
    selectedFsSnapshotIds.value = [];
    return;
  }
  loading.value = true;
  try {
    const { data } = await listCrmFirstSecondRelationImport({
      pageNum: crmFsQuery.pageNum,
      pageSize: crmFsQuery.pageSize,
      targetCompanyId: crmFsQuery.targetCompanyId,
      firstCompanyId: crmFsQuery.firstCompanyId,
      secondCompanyId: crmFsQuery.secondCompanyId,
      firstCompanyCode: crmFsQuery.firstCompanyCode.trim() || undefined,
      secondCompanyCode: crmFsQuery.secondCompanyCode.trim() || undefined,
      showAbnormal: crmFsQuery.showAbnormal,
    });
    rows.value = pickRows(data);
    total.value = pickTotal(data);
    selectedFsSnapshotIds.value = [];
  } finally {
    loading.value = false;
  }
}

/**
 * 作用：打开「从 CRM 导入签约」抽屉并初始化筛选与列表。
 */
async function openHqCrmImport() {
  hqCrmImportOpen.value = true;
  selectedHqSnapshotIds.value = [];
  selectedFsSnapshotIds.value = [];
  await loadCompanyOptionsForCrm();
  crmHqQuery.pageNum = 1;
  if (crmHqQuery.hqCompanyId) {
    await loadCrmImportRegions(crmHqQuery.hqCompanyId);
    await loadCrmHqRows();
  } else {
    rows.value = [];
    total.value = 0;
  }
}

/**
 * 作用：总部一级 CRM 导入筛选中总部变更时重置大区并刷新列表。
 */
function onCrmHqCompanyChange() {
  crmHqQuery.regionId = undefined;
  selectedHqSnapshotIds.value = [];
  crmHqQuery.pageNum = 1;
  const hid = crmHqQuery.hqCompanyId;
  if (hid) {
    loadCrmImportRegions(hid).then(() => loadCrmHqRows());
  } else {
    crmImportRegionOptions.value = [];
    rows.value = [];
    total.value = 0;
  }
}

/**
 * 作用：总部一级 CRM 导入表格查询。
 */
function handleCrmHqSearch() {
  if (!crmHqQuery.hqCompanyId) {
    window.$message?.warning?.("请选择总部公司");
    return;
  }
  crmHqQuery.pageNum = 1;
  loadCrmHqRows();
}

/**
 * 作用：重置总部一级 CRM 导入筛选条件（保留当前总部）。
 */
function resetHqCrmQuery() {
  const hqCompanyId = crmHqQuery.hqCompanyId;
  crmHqQuery.firstCompanyId = undefined;
  crmHqQuery.regionId = undefined;
  crmHqQuery.kunnr = "";
  crmHqQuery.showAbnormal = false;
  crmHqQuery.pageNum = 1;
  crmHqQuery.pageSize = 10;
  crmHqQuery.hqCompanyId = hqCompanyId;
  if (hqCompanyId) {
    loadCrmImportRegions(hqCompanyId).then(() => loadCrmHqRows());
  } else {
    crmImportRegionOptions.value = [];
    rows.value = [];
    total.value = 0;
  }
}

/**
 * 作用：一级二级 CRM 导入表格查询。
 */
function handleCrmFsSearch() {
  if (!crmFsQuery.targetCompanyId) {
    window.$message?.warning?.("请选择目标总部公司");
    return;
  }
  crmFsQuery.pageNum = 1;
  loadCrmFsRows();
}

/**
 * 作用：重置一级二级 CRM 导入筛选条件。
 */
function resetFsCrmQuery() {
  const targetCompanyId = crmFsQuery.targetCompanyId;
  crmFsQuery.firstCompanyId = undefined;
  crmFsQuery.secondCompanyId = undefined;
  crmFsQuery.firstCompanyCode = "";
  crmFsQuery.secondCompanyCode = "";
  crmFsQuery.showAbnormal = false;
  crmFsQuery.pageNum = 1;
  crmFsQuery.pageSize = 10;
  crmFsQuery.targetCompanyId = targetCompanyId;
  loadCrmFsRows();
}

/**
 * 作用：生成表格行唯一 key（兼容多种 ID 字段）。
 * @param record - 行数据
 * @returns row-key 值
 */
function tableRowKey(record: RowData) {
  return (
    record.id ??
    record.batchId ??
    record.custId ??
    `${record.areaCode ?? ""}-${record.typeCode ?? ""}`
  );
}

/**
 * 作用：切换子 Tab 并重置分页、按需预加载选项与列表。
 * @param tab - 目标 TabKey
 */
function applyActiveTabByRoute(tab: TabKey) {
  activeTab.value = tab;
  if (tab === "company") {
    companyQuery.pageNum = 1;
  }
  if (tab === "hqFirst" || tab === "firstSecond") {
    hqFirstQuery.pageNum = 1;
    firstSecondQuery.pageNum = 1;
    loadCompanyOptionsForCrm();
  }
  if (tab === "external") {
    externalQuery.pageNum = 1;
  }
  if (tab === "area") {
    loadCompanyOptionsForCrm().then(() => {
      if (!regionQueryCompanyId.value && hqCompanyOptions.value.length) {
        regionQueryCompanyId.value = Number(hqCompanyOptions.value[0].id);
        loadList();
      }
    });
  }
  loadList();
}

/**
 * 作用：公司列表查询（重置第一页）。
 */
function handleCompanySearch() {
  companyQuery.pageNum = 1;
  loadList();
}

/**
 * 作用：重置公司筛选条件并查询。
 */
function resetCompanyQuery() {
  companyQuery.companyName = undefined;
  companyQuery.typeCode = undefined;
  companyQuery.category = undefined;
  companyQuery.status = undefined;
  handleCompanySearch();
}

/**
 * 作用：外部公司列表查询。
 */
function handleExternalSearch() {
  externalQuery.pageNum = 1;
  loadList();
}

/**
 * 作用：重置外部公司名称筛选并查询。
 */
function resetExternalQuery() {
  externalQuery.companyName = "";
  handleExternalSearch();
}

/**
 * 作用：总部一级签约列表筛选变更触发刷新。
 */
function handleHqFirstSearch() {
  hqFirstQuery.pageNum = 1;
  loadList();
}

/**
 * 作用：一级二级关系列表筛选变更触发刷新。
 */
function handleFirstSecondSearch() {
  firstSecondQuery.pageNum = 1;
  loadList();
}

/**
 * 作用：签约页内部 Tabs（总部一级 / 一级二级）切换。
 * @param tab - Tab key
 */
function handleContractTabChange(tab: string | number) {
  const next = String(tab);
  if (next === "hqFirst" || next === "firstSecond") {
    applyActiveTabByRoute(next);
  }
}

/**
 * 作用：签约表单中总部变更时加载其下属大区选项。
 * @param value - 选中的总部 ID
 */
async function onHqFormHqCompanyChange(value: any) {
  hqForm.regionId = undefined;
  const hqId = Number(value || 0);
  if (!hqId) {
    hqFormRegionOptions.value = [];
    return;
  }
  const { data } = await listRegion(hqId);
  hqFormRegionOptions.value = Array.isArray(data) ? data : pickRows(data);
}

/**
 * 作用：根据路由 name 切换组织 Tab；若已是当前 Tab 则返回 false。
 * @param routeName - 路由 name
 * @returns 是否发生了 Tab 切换
 */
function syncActiveTabByRouteName(routeName: unknown) {
  const routeKey = String(routeName || "");
  const tab = ROUTE_NAME_TO_TAB_KEY[routeKey];
  if (!tab || tab === activeTab.value) return false;
  applyActiveTabByRoute(tab);
  return true;
}

/**
 * 作用：打开总部-一级签约抽屉并回填；加载大区选项。
 * @param record - 编辑行（可选）
 */
async function openHqForm(record?: RowData) {
  await loadCompanyOptionsForCrm();
  if (record) {
    hqForm.id = record.id;
    hqForm.hqCompanyId = record.hqCompanyId;
    hqForm.firstCompanyId = record.firstCompanyId;
    hqForm.regionId = record.regionId;
    hqForm.contractTime = record.contractTime ?? "";
    hqForm.status = record.status ?? 1;
  } else {
    hqForm.id = undefined;
    hqForm.hqCompanyId = undefined;
    hqForm.firstCompanyId = undefined;
    hqForm.regionId = undefined;
    hqForm.contractTime = "";
    hqForm.status = 1;
  }
  const hqId = Number(hqForm.hqCompanyId || 0);
  if (hqId) {
    const { data } = await listRegion(hqId);
    hqFormRegionOptions.value = Array.isArray(data) ? data : pickRows(data);
  } else {
    hqFormRegionOptions.value = [];
  }
  hqFormOpen.value = true;
}

/**
 * 作用：提交总部一级签约表单。
 */
async function submitHqForm() {
  if (!hqForm.hqCompanyId) {
    window.$message?.warning?.("请选择总部公司");
    return;
  }
  if (!hqForm.firstCompanyId) {
    window.$message?.warning?.("请选择一级网点");
    return;
  }
  const body = { ...hqForm, targetCompanyId: hqForm.hqCompanyId };
  if (hqForm.id) {
    await updateHqFirstContract(body);
    window.$message?.success?.("操作成功");
  } else {
    await addHqFirstContract(body);
    window.$message?.success?.("操作成功");
  }
  hqFormOpen.value = false;
  loadList();
}

/**
 * 作用：删除总部一级签约关系。
 * @param id - 记录 ID
 */
async function removeHq(id: number) {
  await deleteHqFirstContract(id, { targetCompanyId: hqFirstQuery.hqCompanyId });
  window.$message?.success?.("删除成功");
  loadList();
}

/**
 * 作用：打开一级二级关系抽屉并回填。
 * @param record - 编辑行（可选）
 */
async function openFsForm(record?: RowData) {
  if (!firstSecondQuery.targetCompanyId) {
    window.$message?.warning?.("请先选择目标总部公司");
    return;
  }
  await loadCompanyOptionsForCrm();
  if (record) {
    fsForm.id = record.id;
    fsForm.firstCompanyId = record.firstCompanyId;
    fsForm.secondCompanyId = record.secondCompanyId;
  } else {
    fsForm.id = undefined;
    fsForm.firstCompanyId = undefined;
    fsForm.secondCompanyId = undefined;
  }
  fsFormOpen.value = true;
}

/**
 * 作用：提交一级二级关系表单。
 */
async function submitFsForm() {
  if (!fsForm.firstCompanyId) {
    window.$message?.warning?.("请选择一级网点");
    return;
  }
  if (!fsForm.secondCompanyId) {
    window.$message?.warning?.("请选择二级网点");
    return;
  }
  const body = { ...fsForm, targetCompanyId: firstSecondQuery.targetCompanyId };
  await addFirstSecondRelation(body);
  window.$message?.success?.("操作成功");
  fsFormOpen.value = false;
  loadList();
}

/**
 * 作用：删除一级二级关系。
 * @param id - 记录 ID
 */
async function removeFs(id: number) {
  await deleteFirstSecondRelation(id, { targetCompanyId: firstSecondQuery.targetCompanyId });
  window.$message?.success?.("删除成功");
  loadList();
}

/**
 * 作用：执行总部一级 CRM 选中行导入。
 */
async function triggerCrmHqImport() {
  if (!crmHqQuery.hqCompanyId) {
    window.$message?.warning?.("请选择总部公司");
    return;
  }
  if (!selectedHqSnapshotIds.value.length) {
    window.$message?.warning?.("请选择要导入的CRM签约关系");
    return;
  }
  const { data } = await importCrmHqFirstContract({
    targetCompanyId: crmHqQuery.hqCompanyId,
    hqCompanyId: crmHqQuery.hqCompanyId,
    snapshotIds: selectedHqSnapshotIds.value,
  });
  const d = (data || {}) as RowData;
  window.$message?.success?.(
    `选中 ${d.selectedCount ?? 0} 条，成功 ${d.successCount ?? 0} 条，已存在 ${d.existedCount ?? 0} 条，失败 ${d.failedCount ?? 0} 条`,
  );
  selectedHqSnapshotIds.value = [];
  hqCrmImportOpen.value = false;
  await loadList();
}

/**
 * 作用：执行一级二级 CRM 选中行导入。
 */
async function triggerCrmFsImport() {
  if (!crmFsQuery.targetCompanyId) {
    window.$message?.warning?.("请选择目标总部公司");
    return;
  }
  if (!selectedFsSnapshotIds.value.length) {
    window.$message?.warning?.("请选择要导入的一级二级关系");
    return;
  }
  const { data } = await importCrmFirstSecondRelation({
    targetCompanyId: crmFsQuery.targetCompanyId,
    snapshotIds: selectedFsSnapshotIds.value,
  });
  const d = (data || {}) as RowData;
  window.$message?.success?.(
    `选中 ${d.selectedCount ?? 0} 条，成功 ${d.successCount ?? 0} 条，已存在 ${d.existedCount ?? 0} 条，冲突 ${d.conflictCount ?? 0} 条，失败 ${d.failedCount ?? 0} 条`,
  );
  selectedFsSnapshotIds.value = [];
  await loadList();
}

/**
 * 作用：打开外部公司 CRM 导入预览抽屉并触发加载。
 * @param custId - 客户主键 ID
 */
function openPreview(custId: string | number) {
  previewCustId.value = custId;
  previewOpen.value = true;
  previewData.value = null;
  loadPreview();
}

/**
 * 作用：按预览抽屉中的 custId 拉取 CRM 导入预览 JSON。
 */
async function loadPreview() {
  if (previewCustId.value === "" || previewCustId.value === undefined) {
    window.$message?.warning?.("请填写客户/外部主键 ID");
    return;
  }
  previewLoading.value = true;
  try {
    const { data } = await getExternalCompanyImportPreview(previewCustId.value);
    previewData.value = data;
  } finally {
    previewLoading.value = false;
  }
}

/**
 * 作用：从 CRM 快照行解析用于导入的 snapshotId。
 * @param record - 表格行
 * @returns 快照 ID
 */
function extractSnapshotId(record: RowData) {
  return record.snapshotId ?? record.snapshotID ?? record.id;
}

/**
 * 作用：总部一级 CRM 导入表格行选中变更，同步 snapshotIds。
 * @param _keys - 选中 key（未使用）
 * @param selectedRows - 选中行
 */
function onSelectHqCrmRows(
  _keys: Array<string | number>,
  selectedRows: RowData[],
) {
  selectedHqSnapshotIds.value = (selectedRows || [])
    .map(extractSnapshotId)
    .filter((id) => id != null && id !== "");
}

/**
 * 作用：一级二级 CRM 导入表格行选中变更，同步 snapshotIds。
 * @param _keys - 选中 key（未使用）
 * @param selectedRows - 选中行
 */
function onSelectFsCrmRows(
  _keys: Array<string | number>,
  selectedRows: RowData[],
) {
  selectedFsSnapshotIds.value = (selectedRows || [])
    .map(extractSnapshotId)
    .filter((id) => id != null && id !== "");
}

/**
 * 作用：主列表在非 CRM 抽屉场景关闭行选择（返回 undefined）。
 * @returns undefined 表示无 rowSelection
 */
function crmRowSelection() {
  return undefined;
}

/**
 * 作用：打开公司新增/编辑抽屉，加载省市区与类型选项并回填。
 * @param record - 编辑行（可选）
 */
function openCompanyForm(record?: RowData) {
  loadProvinceOptions();
  ensureCompanyTypeOptions();
  if (record) {
    Object.assign(companyForm, {
      id: record.id,
      companyName: record.companyName ?? "",
      companyShortName: record.companyShortName ?? "",
      companyCode: record.companyCode ?? "",
      typeCode: record.typeCode ?? "",
      contactName: record.contactName ?? "",
      contactPhone: record.contactPhone ?? "",
      servicePhone: record.servicePhone ?? "",
      sourceType: record.sourceType ?? "MANUAL",
      provinceCode: record.provinceCode ?? "",
      cityCode: record.cityCode ?? "",
      districtCode: record.districtCode ?? "",
      detailAddress: record.detailAddress ?? "",
      adminUsername: record.adminUsername ?? "",
      salesOrg: record.salesOrg ?? "",
      status: record.status ?? 1,
      remark: record.remark ?? "",
    });
    loadCompanyAreaOptionsForEdit(record.provinceCode, record.cityCode);
  } else {
    Object.assign(companyForm, {
      id: undefined,
      companyName: "",
      companyShortName: "",
      companyCode: "",
      typeCode: "",
      contactName: "",
      contactPhone: "",
      servicePhone: "",
      sourceType: "MANUAL",
      provinceCode: "",
      cityCode: "",
      districtCode: "",
      detailAddress: "",
      adminUsername: "",
      salesOrg: "",
      status: 1,
      remark: "",
    });
  }
  companyFormOpen.value = true;
}

/**
 * 作用：校验并提交公司表单（新增或更新）。
 */
async function submitCompanyForm() {
  if (!String(companyForm.companyName || "").trim()) {
    window.$message?.warning?.("请输入公司名称");
    return;
  }
  if (!String(companyForm.typeCode || "").trim()) {
    window.$message?.warning?.("请选择公司类型");
    return;
  }
  if (
    !isCompanyHqType(companyForm.typeCode) &&
    !String(companyForm.companyCode || "").trim()
  ) {
    window.$message?.warning?.("请输入公司编码");
    return;
  }
  if (
    isCompanyHqType(companyForm.typeCode) &&
    !String(companyForm.salesOrg || "").trim()
  ) {
    window.$message?.warning?.("请输入销售组织");
    return;
  }
  if (!String(companyForm.contactName || "").trim()) {
    window.$message?.warning?.("请输入联系人");
    return;
  }
  if (!String(companyForm.contactPhone || "").trim()) {
    window.$message?.warning?.("请输入联系电话");
    return;
  }
  if (!String(companyForm.provinceCode || "").trim()) {
    window.$message?.warning?.("请选择省份");
    return;
  }
  if (!String(companyForm.cityCode || "").trim()) {
    window.$message?.warning?.("请选择城市");
    return;
  }
  if (!String(companyForm.districtCode || "").trim()) {
    window.$message?.warning?.("请选择区县");
    return;
  }
  if (!String(companyForm.detailAddress || "").trim()) {
    window.$message?.warning?.("请输入详细地址");
    return;
  }
  if (!companyForm.id && !String(companyForm.adminUsername || "").trim()) {
    window.$message?.warning?.("请输入管理员用户名");
    return;
  }

  const province = provinceOptions.value.find(
    (item) => String(item.areaCode) === String(companyForm.provinceCode),
  );
  const city = cityOptions.value.find(
    (item) => String(item.areaCode) === String(companyForm.cityCode),
  );
  const district = districtOptions.value.find(
    (item) => String(item.areaCode) === String(companyForm.districtCode),
  );
  const body = { ...companyForm } as SysCompanyDTO;
  body.provinceName = province?.areaName;
  body.cityName = city?.areaName;
  body.districtName = district?.areaName;

  // 接口失败时 request 侧会统一弹出错误信息，这里只处理成功时的接口返回 msg。
  const result = companyForm.id
    ? await updateCompany(body)
    : await addCompany(body);
  if (!result) return;

  const response = (result as unknown as { response?: unknown }).response;
  window.$message?.success?.(getResponseMsg(response, "操作成功"));
  companyFormOpen.value = false;
  await loadList();
}

/**
 * 作用：删除公司。
 * @param id - 公司 ID
 */
async function removeCompany(id: number) {
  await deleteCompany(id);
  window.$message?.success?.("删除成功");
  loadList();
}

/**
 * 作用：打开公司类型新增/编辑表单。
 * @param record - 编辑行（可选）
 */
function openCompanyTypeForm(record?: RowData) {
  companyTypeForm.id = record?.id;
  companyTypeForm.typeName = record?.typeName ?? "";
  companyTypeForm.typeCode = record?.typeCode ?? "";
  companyTypeForm.subjectType = record?.subjectType ?? "SERVICE";
  companyTypeForm.orderNum = Number(record?.orderNum ?? 0);
  companyTypeForm.remark = record?.remark ?? "";
  companyTypeFormOpen.value = true;
}

/**
 * 作用：提交公司类型表单。
 */
async function submitCompanyTypeForm() {
  const payload = {
    id: companyTypeForm.id,
    typeName: companyTypeForm.typeName,
    typeCode: companyTypeForm.typeCode,
    subjectType: companyTypeForm.subjectType as "PLATFORM" | "HQ" | "SERVICE",
    orderNum: Number(companyTypeForm.orderNum ?? 0),
    remark: companyTypeForm.remark,
  };
  if (payload.id) await updateCompanyType(payload);
  else await addCompanyType(payload);
  companyTypeFormOpen.value = false;
  window.$message?.success?.("操作成功");
  await loadList();
}

/**
 * 作用：删除公司类型。
 * @param id - 类型 ID
 */
async function removeCompanyType(id: number) {
  await deleteCompanyType(id);
  window.$message?.success?.("删除成功");
  await loadList();
}

/**
 * 作用：打开「按类型编码分配菜单」抽屉并加载树与已选菜单 ID。
 * @param record - 公司类型行
 */
async function openCompanyTypeMenuAssign(record: RowData) {
  const typeCode = String(record.typeCode || "");
  if (!typeCode) {
    window.$message?.warning?.("当前类型缺少 typeCode");
    return;
  }
  companyTypeMenuTypeCode.value = typeCode;
  companyTypeMenuTitle.value = `分配菜单 - ${record.typeName || ""}（${typeCode}）`;
  companyTypeMenuCheckedKeys.value = [];
  companyTypeMenuTreeData.value = [];
  companyTypeMenuOpen.value = true;
  companyTypeMenuLoading.value = true;
  try {
    const [treeRes, idsRes] = await Promise.all([
      typeCodeMenuTree(typeCode),
      typeCodeMenuIds(typeCode),
    ]);
    companyTypeMenuTreeData.value = pickRows(treeRes.data);
    companyTypeMenuCheckedKeys.value = Array.isArray(idsRes.data)
      ? idsRes.data
      : [];
  } finally {
    companyTypeMenuLoading.value = false;
  }
}

/**
 * 作用：保存类型与菜单的绑定关系。
 */
async function submitCompanyTypeMenuAssign() {
  if (!companyTypeMenuTypeCode.value) return;
  companyTypeMenuSubmitting.value = true;
  try {
    await assignTypeCodeMenus(
      companyTypeMenuTypeCode.value,
      companyTypeMenuCheckedKeys.value,
    );
    companyTypeMenuOpen.value = false;
    window.$message?.success?.("菜单分配保存成功");
  } finally {
    companyTypeMenuSubmitting.value = false;
  }
}

/**
 * 作用：加载省级行政区划选项（公司表单）。
 */
async function loadProvinceOptions() {
  const { data } = await listAreaOptions();
  provinceOptions.value = Array.isArray(data) ? data : pickRows(data);
}

/**
 * 作用：编辑公司时根据已有省、市代码加载市、区下拉。
 * @param provinceCode - 省代码
 * @param cityCode - 市代码
 */
async function loadCompanyAreaOptionsForEdit(
  provinceCode?: string | number,
  cityCode?: string | number,
) {
  cityOptions.value = [];
  districtOptions.value = [];

  const province = String(provinceCode ?? "").trim();
  if (!province) return;

  const cityRes = await listAreaOptions(province);
  cityOptions.value = Array.isArray(cityRes.data)
    ? cityRes.data
    : pickRows(cityRes.data);

  const city = String(cityCode ?? "").trim();
  if (!city) return;

  const districtRes = await listAreaOptions(city);
  districtOptions.value = Array.isArray(districtRes.data)
    ? districtRes.data
    : pickRows(districtRes.data);
}

/**
 * 作用：公司表单省份变更时重置市、区并加载市列表。
 * @param code - 省 areaCode
 */
async function onCompanyProvinceChange(code?: any) {
  companyForm.cityCode = "";
  companyForm.districtCode = "";
  cityOptions.value = [];
  districtOptions.value = [];
  if (!code) return;
  const { data } = await listAreaOptions(code);
  cityOptions.value = Array.isArray(data) ? data : pickRows(data);
}

/**
 * 作用：公司表单城市变更时重置区并加载区列表。
 * @param code - 市 areaCode
 */
async function onCompanyCityChange(code?: any) {
  companyForm.districtCode = "";
  districtOptions.value = [];
  if (!code) return;
  const { data } = await listAreaOptions(code);
  districtOptions.value = Array.isArray(data) ? data : pickRows(data);
}

/**
 * 作用：打开「从 CRM 选择公司」抽屉并重置到第一页列表。
 */
async function prefillCompanyByCrm() {
  companyCrmImportOpen.value = true;
  companyCrmImportQuery.pageNum = 1;
  await loadCompanyCrmImportList();
}

/**
 * 作用：按 CRM 导入查询条件加载可选外部公司列表。
 */
async function loadCompanyCrmImportList() {
  companyCrmImportLoading.value = true;
  try {
    const { data } = await listExternalCompany({
      pageNum: companyCrmImportQuery.pageNum,
      pageSize: companyCrmImportQuery.pageSize,
      companyCode: companyCrmImportQuery.companyCode.trim() || undefined,
      companyName: companyCrmImportQuery.companyName.trim() || undefined,
      custState: companyCrmImportQuery.custState,
    });
    companyCrmImportRows.value = pickRows(data);
    companyCrmImportTotal.value = pickTotal(data);
  } finally {
    companyCrmImportLoading.value = false;
  }
}

/**
 * 作用：CRM 导入抽屉内搜索（回到第一页）。
 */
function handleCompanyCrmImportSearch() {
  companyCrmImportQuery.pageNum = 1;
  loadCompanyCrmImportList();
}

/**
 * 作用：重置 CRM 导入抽屉筛选并刷新列表。
 */
function resetCompanyCrmImportSearch() {
  companyCrmImportQuery.pageNum = 1;
  companyCrmImportQuery.pageSize = 10;
  companyCrmImportQuery.companyCode = "";
  companyCrmImportQuery.companyName = "";
  companyCrmImportQuery.custState = undefined;
  loadCompanyCrmImportList();
}

/**
 * 作用：选中 CRM 一行后预览并带入公司表单或提示不可导入。
 * @param row - 外部公司行
 */
async function useCompanyCrmImportRow(row: RowData) {
  const custId = row.custId ?? row.id;
  if (!custId) return;
  const { data } = await getExternalCompanyImportPreview(custId);
  const preview = ((data as RowData) || {}) as RowData;
  if (preview.existingCompanyId) {
    companyCrmImportOpen.value = false;
    const existing = rows.value.find(
      (item) => Number(item.id) === Number(preview.existingCompanyId),
    );
    if (existing) {
      openCompanyForm(existing);
    } else {
      window.$message?.warning?.("该公司已存在，请在列表中编辑");
    }
    return;
  }
  if (preview.canImport === false) {
    window.$message?.warning?.(
      String(preview.importDisabledReason || "当前记录不可导入"),
    );
    return;
  }
  companyCrmImportOpen.value = false;
  openCompanyForm();
  Object.assign(companyForm, {
    companyName: preview.companyName ?? "",
    companyShortName: preview.companyShortName ?? "",
    companyCode: preview.companyCode ?? "",
    typeCode: preview.typeCode ?? "",
    contactName: preview.contactName ?? "",
    contactPhone: preview.contactPhone ?? "",
    servicePhone: preview.servicePhone ?? "",
    sourceType: preview.sourceType ?? "CRM",
    provinceCode: preview.provinceCode ?? "",
    cityCode: preview.cityCode ?? "",
    districtCode: preview.districtCode ?? "",
    detailAddress: preview.detailAddress ?? "",
    salesOrg: preview.salesOrg ?? "",
    adminUsername: preview.adminUsername ?? preview.companyCode ?? "",
    status: preview.status == null ? 1 : preview.status,
    remark: preview.remark ?? "",
  });
  loadCompanyAreaOptionsForEdit(
    companyForm.provinceCode as string,
    companyForm.cityCode as string,
  );
}

/**
 * 作用：公司 Tab 下的分页配置对象；非公司 Tab 返回 false。
 * @returns Pagination 配置或 false
 */
function companyPagination() {
  if (activeTab.value !== "company") return false;
  return {
    current: companyQuery.pageNum,
    pageSize: companyQuery.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
    onChange: (page: number, pageSize?: number) => {
      companyQuery.pageNum = page;
      if (pageSize) companyQuery.pageSize = pageSize;
      loadList();
    },
  };
}

/**
 * 作用：总部一级签约 Tab 分页配置；非对应 Tab 返回 false。
 * @returns Pagination 配置或 false
 */
function hqPagination() {
  if (activeTab.value !== "hqFirst") return false;
  return {
    current: hqFirstQuery.pageNum,
    pageSize: hqFirstQuery.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
    onChange: (page: number, pageSize?: number) => {
      hqFirstQuery.pageNum = page;
      if (pageSize) hqFirstQuery.pageSize = pageSize;
      loadList();
    },
  };
}

/**
 * 作用：一级二级关系 Tab 分页配置；非对应 Tab 返回 false。
 * @returns Pagination 配置或 false
 */
function fsPagination() {
  if (activeTab.value !== "firstSecond") return false;
  return {
    current: firstSecondQuery.pageNum,
    pageSize: firstSecondQuery.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
    onChange: (page: number, pageSize?: number) => {
      firstSecondQuery.pageNum = page;
      if (pageSize) firstSecondQuery.pageSize = pageSize;
      loadList();
    },
  };
}

/**
 * 作用：外部公司 Tab 分页配置；非对应 Tab 返回 false。
 * @returns Pagination 配置或 false
 */
function externalPagination() {
  if (activeTab.value !== "external") return false;
  return {
    current: externalQuery.pageNum,
    pageSize: externalQuery.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
    onChange: (page: number, pageSize?: number) => {
      externalQuery.pageNum = page;
      if (pageSize) externalQuery.pageSize = pageSize;
      loadList();
    },
  };
}

/**
 * 作用：合并各业务 Tab 的分页配置（短路返回首个有效）。
 * @returns Pagination 配置或 false
 */
function mergedPagination() {
  const p =
    companyPagination() ||
    hqPagination() ||
    fsPagination() ||
    externalPagination() ||
    false;
  return p;
}

/**
 * 作用：主表格 pagination：公司类型/大区 Tab 无分页。
 * @returns Pagination 配置或 false
 */
function tablePagination() {
  const p = mergedPagination();
  if (activeTab.value === "companyType" || activeTab.value === "area")
    return false;
  return p || false;
}

// 路由切换时同步组织管理子 Tab
watch(
  () => route.name,
  (name) => {
    syncActiveTabByRouteName(name);
  },
);

onMounted(() => {
  const changedByRoute = syncActiveTabByRouteName(route.name);
  if (!changedByRoute) {
    loadList();
  }
});
</script>

<template>
  <div
    class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto"
  >
    <ACard
      v-if="activeTab !== 'companyType'"
      :bordered="false"
      class="card-wrapper"
    >
      <ATabs
        v-if="isContractPageTab"
        :active-key="activeTab"
        type="card"
        class="mb-12px"
        @change="handleContractTabChange"
      >
        <ATabPane key="hqFirst" tab="总部-一级签约" />
        <ATabPane key="firstSecond" tab="一级-二级关系" />
      </ATabs>
      <AForm
        v-if="activeTab === 'company'"
        :model="companyQuery"
        :label-col="{ span: 5, md: 7 }"
        class="mb-12px"
      >
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="公司名称" class="m-0">
                  <AInput
                    v-model:value="companyQuery.companyName"
                    allow-clear
                    placeholder="请输入公司名称"
                  />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="公司类型" class="m-0">
                  <ASelect
                    v-model:value="companyQuery.typeCode"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="
                      companyTypeOptions.map((item) => ({
                        label: item.typeName,
                        value: item.typeCode,
                      }))
                    "
                  />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="状态" class="m-0">
                  <ASelect
                    v-model:value="companyQuery.status"
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
            <AButton
              type="primary"
              :loading="loading"
              @click="handleCompanySearch"
              >查询</AButton
            >
            <AButton :loading="loading" @click="resetCompanyQuery"
              >重置</AButton
            >
          </div>
        </div>
      </AForm>

      <AForm
        v-if="activeTab === 'hqFirst'"
        :model="hqFirstQuery"
        :label-col="{ span: 5, md: 7 }"
        class="mb-12px"
      >
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="总部公司" class="m-0">
                  <ASelect
                    v-model:value="hqFirstQuery.hqCompanyId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    placeholder="全部"
                    class="w-full"
                    :options="
                      hqCompanyOptions.map((c) => ({
                        label: c.companyName,
                        value: c.id,
                      }))
                    "
                    @change="handleHqFirstSearch"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
        </div>
      </AForm>
      <AForm
        v-if="activeTab === 'firstSecond'"
        :model="firstSecondQuery"
        :label-col="{ span: 5, md: 7 }"
        class="mb-12px"
      >
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="目标总部" class="m-0" required>
                  <ASelect
                    v-model:value="firstSecondQuery.targetCompanyId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    placeholder="请选择总部"
                    class="w-full"
                    :options="
                      hqCompanyOptions.map((c) => ({
                        label: c.companyName,
                        value: c.id,
                      }))
                    "
                    @change="handleFirstSecondSearch"
                  />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="一级网点" class="m-0">
                  <ASelect
                    v-model:value="firstSecondQuery.firstCompanyId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    placeholder="全部"
                    class="w-full"
                    :options="
                      firstCompanyOptions.map((c) => ({
                        label: c.companyName,
                        value: c.id,
                      }))
                    "
                    @change="handleFirstSecondSearch"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
        </div>
      </AForm>

      <AForm
        v-if="activeTab === 'external'"
        :model="externalQuery"
        :label-col="{ span: 5, md: 7 }"
        class="mb-12px"
      >
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="名称" class="m-0">
                  <AInput
                    v-model:value="externalQuery.companyName"
                    allow-clear
                    placeholder="名称筛选"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton
              type="primary"
              :loading="loading"
              @click="handleExternalSearch"
              >查询</AButton
            >
            <AButton :loading="loading" @click="resetExternalQuery"
              >重置</AButton
            >
          </div>
        </div>
      </AForm>

      <AForm
        v-if="activeTab === 'area'"
        :label-col="{ span: 5, md: 7 }"
        class="mb-12px"
      >
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="总部公司" class="m-0">
                  <ASelect
                    v-model:value="regionQueryCompanyId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    placeholder="请选择"
                    class="w-full"
                    :options="
                      hqCompanyOptions.map((c) => ({
                        label: c.companyName,
                        value: c.id,
                      }))
                    "
                    @change="handleRegionSearch"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
        </div>
      </AForm>
    </ACard>
    <ACard
      title="组织管理"
      :bordered="false"
      :body-style="{ flex: 1, overflow: 'hidden' }"
      class="flex-col-stretch card-wrapper sm:flex-1-hidden"
    >
      <template #extra>
        <ASpace wrap>
          <AButton
            v-if="activeTab === 'companyType'"
            type="primary"
            @click="openCompanyTypeForm()"
          >
            新增公司类型
          </AButton>
          <AButton
            v-if="activeTab === 'area'"
            type="primary"
            :disabled="!regionQueryCompanyId"
            @click="openRegionForm()"
          >
            新增大区
          </AButton>
          <AButton
            v-if="activeTab === 'company'"
            type="primary"
            ghost
            @click="openCompanyForm()"
            >新增公司</AButton
          >
          <AButton
            v-if="activeTab === 'company'"
            type="primary"
            @click="prefillCompanyByCrm"
            >从 CRM 导入</AButton
          >
          <AButton
            v-if="activeTab === 'hqFirst'"
            type="primary"
            ghost
            @click="openHqForm()"
            >新增签约</AButton
          >
          <AButton
            v-if="activeTab === 'hqFirst'"
            type="primary"
            @click="openHqCrmImport"
            >从CRM导入</AButton
          >
          <AButton
            v-if="activeTab === 'firstSecond'"
            type="primary"
            ghost
            @click="openFsForm()"
            >新增关系</AButton
          >
        </ASpace>
      </template>
      <ATable
        ref="tableWrapperRef"
        :columns="displayColumns"
        :data-source="rows"
        :loading="loading"
        class="h-full"
        :pagination="tablePagination()"
        :row-key="tableRowKey"
        :row-selection="crmRowSelection()"
        size="small"
        :scroll="scrollConfig"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'hqCrmRegion'">
            <span>{{ record.regionName || "-" }}</span>
            <span v-if="record.regionCode" class="text-gray-500"
              >（{{ record.regionCode }}）</span
            >
          </template>
          <template v-else-if="column.key === 'hqCrmAlive'">
            <ATag :color="tagColorPositiveNeutral(record.aliveFlag === 1)">
              {{ record.aliveFlag === 1 ? "有效" : "失效" }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'hqCrmImportSts'">
            <ATag v-if="record.canImport" color="success">可导入</ATag>
            <ATag v-else-if="record.existingContract" color="processing"
              >已存在</ATag
            >
            <ATag v-else color="warning">异常</ATag>
          </template>
          <template v-else-if="column.key === 'fsCrmImportSts'">
            <ATag v-if="record.canImport" color="success">可导入</ATag>
            <ATag v-else-if="record.existingRelation" color="processing"
              >已存在</ATag
            >
            <ATag v-else-if="record.conflictingRelation" color="error"
              >冲突</ATag
            >
            <ATag v-else color="warning">异常</ATag>
          </template>
          <template
            v-else-if="column.key === 'typeCode' && activeTab === 'company'"
          >
            {{ getCompanyTypeLabel(record.typeCode) || record.typeCode || "-" }}
          </template>
          <template
            v-else-if="column.key === 'subjectType' && activeTab === 'company'"
          >
            <ATag
              v-if="getCompanySubjectType(record.typeCode) === 'HQ'"
              color="warning"
              >总部</ATag
            >
            <ATag
              v-else-if="getCompanySubjectType(record.typeCode) === 'SERVICE'"
              color="success"
              >网点</ATag
            >
            <ATag v-else>平台</ATag>
          </template>
          <template
            v-else-if="column.key === 'sourceType' && activeTab === 'company'"
          >
            <ATag
              :color="record.sourceType === 'CRM' ? 'default' : 'success'"
              >{{ record.sourceType || "-" }}</ATag
            >
          </template>
          <template
            v-else-if="column.key === 'salesOrg' && activeTab === 'company'"
          >
            {{
              getCompanySubjectType(record.typeCode) === "HQ"
                ? record.salesOrg || "-"
                : "-"
            }}
          </template>
          <template
            v-else-if="column.key === 'region' && activeTab === 'company'"
          >
            {{ formatCompanyRegion(record) }}
          </template>
          <template
            v-else-if="
              column.key === 'geocodeStatus' && activeTab === 'company'
            "
          >
            <ATag
              :color="record.geocodeStatus === 'SUCCESS' ? 'success' : 'error'"
            >
              {{ record.geocodeStatus === "SUCCESS" ? "SUCCESS" : "FAILED" }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'status'">
            <ATag :color="tagColorEnabled(record.status === 1)">
              {{
                activeTab === "hqFirst"
                  ? record.status === 1
                    ? "有效"
                    : "失效"
                  : record.status === 1
                    ? "启用"
                    : "停用"
              }}
            </ATag>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeTab === 'company'"
          >
            <ASpace>
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openCompanyForm(record)"
              >
                编辑
              </AButton>
              <APopconfirm
                :title="`确认删除公司“${record.companyName || '-'}”吗？`"
                @confirm="removeCompany(record.id)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeTab === 'hqFirst'"
          >
            <ASpace>
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openHqForm(record)"
              >
                编辑
              </AButton>
              <APopconfirm
                title="确认删除该签约关系？"
                @confirm="removeHq(record.id)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeTab === 'firstSecond'"
          >
            <ASpace>
              <APopconfirm
                title="确认删除该关系？"
                @confirm="removeFs(record.id)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeTab === 'external'"
          >
            <AButton
              type="link"
              size="small"
              class="table-action-link--info"
              @click="openPreview(record.id ?? record.custId)"
            >
              导入预览
            </AButton>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeTab === 'area'"
          >
            <ASpace>
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openRegionForm(record)"
              >
                编辑
              </AButton>
              <APopconfirm
                :title="`确认删除大区“${record.regionName || '-'}”？`"
                @confirm="removeRegion(record)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template
            v-else-if="column.key === 'actions' && activeTab === 'companyType'"
          >
            <ASpace>
              <AButton
                type="link"
                size="small"
                class="table-action-link--primary"
                @click="openCompanyTypeForm(record)"
              >
                编辑
              </AButton>
              <AButton
                type="link"
                size="small"
                class="table-action-link--processing"
                @click="openCompanyTypeMenuAssign(record)"
              >
                分配菜单
              </AButton>
              <APopconfirm
                :title="`确认删除类型“${record.typeName || '-'}”？`"
                @confirm="removeCompanyType(record.id)"
              >
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer v-model:open="hqCrmImportOpen" title="从CRM导入签约" :width="1180">
      <AForm
        :model="crmHqQuery"
        layout="inline"
        class="page-search-toolbar--inline mb-12px"
      >
        <AFormItem label="总部公司">
          <ASelect
            v-model:value="crmHqQuery.hqCompanyId"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="请选择总部公司"
            class="min-w-180px"
            :options="
              hqCompanyOptions.map((c) => ({
                label: c.companyName,
                value: c.id,
              }))
            "
            @change="onCrmHqCompanyChange"
          />
        </AFormItem>
        <AFormItem label="一级公司">
          <ASelect
            v-model:value="crmHqQuery.firstCompanyId"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="全部"
            class="min-w-160px"
            :options="
              firstCompanyOptions.map((c) => ({
                label: c.companyName,
                value: c.id,
              }))
            "
          />
        </AFormItem>
        <AFormItem label="大区">
          <ASelect
            v-model:value="crmHqQuery.regionId"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="全部"
            class="min-w-160px"
            :options="
              crmImportRegionOptions.map((r) => ({
                label: `${r.regionName || '-'}${r.regionCode ? `（${r.regionCode}）` : ''}`,
                value: r.id,
              }))
            "
          />
        </AFormItem>
        <AFormItem label="客户编码">
          <AInput
            v-model:value="crmHqQuery.kunnr"
            allow-clear
            placeholder="请输入客户编码"
            @press-enter="handleCrmHqSearch"
          />
        </AFormItem>
        <AFormItem>
          <ACheckbox v-model:checked="crmHqQuery.showAbnormal"
            >查看异常数据</ACheckbox
          >
        </AFormItem>
        <AFormItem>
          <AButton type="primary" :loading="loading" @click="handleCrmHqSearch"
            >搜索</AButton
          >
          <AButton class="ml-8px" :loading="loading" @click="resetHqCrmQuery"
            >重置</AButton
          >
        </AFormItem>
      </AForm>
      <ATable
        :loading="loading"
        :columns="crmHqImportColumns"
        :data-source="rows"
        row-key="id"
        size="small"
        :row-selection="{
          selectedRowKeys: selectedHqSnapshotIds,
          onChange: onSelectHqCrmRows,
          getCheckboxProps: (record: RowData) => ({
            disabled: !record.canImport,
          }),
        }"
        :pagination="{
          current: crmHqQuery.pageNum,
          pageSize: crmHqQuery.pageSize,
          total: total,
          showSizeChanger: true,
          onChange: (page: number, pageSize?: number) => {
            crmHqQuery.pageNum = page;
            if (pageSize) crmHqQuery.pageSize = pageSize;
            loadCrmHqRows();
          },
        }"
        :scroll="{ x: 1480 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'hqCrmRegion'">
            <span>{{ record.regionName || "-" }}</span>
            <span v-if="record.regionCode" class="text-gray-500"
              >（{{ record.regionCode }}）</span
            >
          </template>
          <template v-else-if="column.key === 'hqCrmAlive'">
            <ATag :color="tagColorPositiveNeutral(record.aliveFlag === 1)">
              {{ record.aliveFlag === 1 ? "有效" : "失效" }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'hqCrmImportSts'">
            <ATag v-if="record.canImport" color="success">可导入</ATag>
            <ATag v-else-if="record.existingContract" color="processing"
              >已存在</ATag
            >
            <ATag v-else color="warning">异常</ATag>
          </template>
        </template>
      </ATable>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="hqCrmImportOpen = false">取消</AButton>
          <AButton type="primary" :loading="loading" @click="triggerCrmHqImport"
            >确定</AButton
          >
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="hqFormOpen" title="总部-一级签约" :width="500">
      <AForm layout="vertical" class="mt-12px">
        <AFormItem label="总部公司" required>
          <ASelect
            v-model:value="hqForm.hqCompanyId"
            show-search
            option-filter-prop="label"
            placeholder="请选择"
            class="w-full"
            :options="
              hqCompanyOptions.map((c) => ({
                label: c.companyName,
                value: c.id,
              }))
            "
            @change="onHqFormHqCompanyChange"
          />
        </AFormItem>
        <AFormItem label="一级网点" required>
          <ASelect
            v-model:value="hqForm.firstCompanyId"
            show-search
            option-filter-prop="label"
            placeholder="请选择"
            class="w-full"
            :options="
              firstCompanyOptions.map((c) => ({
                label: c.companyName,
                value: c.id,
              }))
            "
          />
        </AFormItem>
        <AFormItem label="所属大区">
          <ASelect
            v-model:value="hqForm.regionId"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="请选择"
            class="w-full"
            :options="
              hqFormRegionOptions.map((r) => ({
                label: r.regionName,
                value: r.id,
              }))
            "
          />
        </AFormItem>
        <AFormItem label="签约时间">
          <ADatePicker
            v-model:value="hqForm.contractTime"
            value-format="YYYY-MM-DD"
            class="w-full"
            placeholder="请选择"
          />
        </AFormItem>
        <AFormItem label="状态">
          <ARadioGroup v-model:value="hqForm.status">
            <ARadio :value="1">有效</ARadio>
            <ARadio :value="0">失效</ARadio>
          </ARadioGroup>
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="hqFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitHqForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="fsFormOpen" title="一级-二级关系" :width="500">
      <AForm layout="vertical" class="mt-12px">
        <AFormItem label="一级网点" required>
          <ASelect
            v-model:value="fsForm.firstCompanyId"
            show-search
            option-filter-prop="label"
            placeholder="请选择"
            class="w-full"
            :options="
              firstCompanyOptions.map((c) => ({
                label: c.companyName,
                value: c.id,
              }))
            "
          />
        </AFormItem>
        <AFormItem label="二级网点" required>
          <ASelect
            v-model:value="fsForm.secondCompanyId"
            show-search
            option-filter-prop="label"
            placeholder="请选择"
            class="w-full"
            :options="
              secondCompanyOptions.map((c) => ({
                label: c.companyName,
                value: c.id,
              }))
            "
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="fsFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitFsForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="companyFormOpen" title="公司信息" :width="980">
      <AForm layout="vertical" class="mt-12px">
        <ARow :gutter="[16, 0]">
          <ACol :span="24" :md="12">
            <AFormItem label="公司名称" required>
              <AInput
                v-model:value="companyForm.companyName"
                placeholder="请输入公司名称"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="公司简称">
              <AInput
                v-model:value="companyForm.companyShortName"
                placeholder="请输入公司简称"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="公司编码">
              <AInput
                v-model:value="companyForm.companyCode"
                :disabled="!!companyForm.id"
                placeholder="请输入公司编码"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="类型编码" required>
              <ASelect
                v-model:value="companyForm.typeCode"
                show-search
                option-filter-prop="label"
                :disabled="!!companyForm.id"
                placeholder="请选择"
                :options="
                  companyTypeOptions.map((item) => ({
                    label: `${item.typeName} (${item.typeCode})`,
                    value: item.typeCode,
                  }))
                "
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="联系人" required>
              <AInput
                v-model:value="companyForm.contactName"
                placeholder="请输入联系人"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="联系电话" required>
              <AInput
                v-model:value="companyForm.contactPhone"
                placeholder="请输入联系电话"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="客服电话">
              <AInput
                v-model:value="companyForm.servicePhone"
                placeholder="请输入客服电话"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="来源类型">
              <AInput :value="companyForm.sourceType || 'MANUAL'" disabled />
            </AFormItem>
          </ACol>
        </ARow>
        <div class="company-address-block">
          <div class="company-address-block__title">地址信息</div>
          <ARow :gutter="16">
            <ACol :span="8">
              <AFormItem label="省份" required>
                <ASelect
                  v-model:value="companyForm.provinceCode"
                  show-search
                  option-filter-prop="label"
                  placeholder="请选择省份"
                  :options="
                    provinceOptions.map((item) => ({
                      label: item.areaName,
                      value: item.areaCode,
                    }))
                  "
                  @change="onCompanyProvinceChange"
                />
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem label="城市" required>
                <ASelect
                  v-model:value="companyForm.cityCode"
                  show-search
                  option-filter-prop="label"
                  :disabled="!companyForm.provinceCode"
                  placeholder="请选择城市"
                  :options="
                    cityOptions.map((item) => ({
                      label: item.areaName,
                      value: item.areaCode,
                    }))
                  "
                  @change="onCompanyCityChange"
                />
              </AFormItem>
            </ACol>
            <ACol :span="8">
              <AFormItem label="区县" required>
                <ASelect
                  v-model:value="companyForm.districtCode"
                  show-search
                  option-filter-prop="label"
                  :disabled="!companyForm.cityCode"
                  placeholder="请选择区县"
                  :options="
                    districtOptions.map((item) => ({
                      label: item.areaName,
                      value: item.areaCode,
                    }))
                  "
                />
              </AFormItem>
            </ACol>
          </ARow>
          <AFormItem
            label="详细地址"
            required
            class="company-address-block__detail"
          >
            <AInput
              v-model:value="companyForm.detailAddress"
              placeholder="请输入详细地址"
            />
          </AFormItem>
        </div>
        <ARow :gutter="[16, 0]">
          <ACol :span="24" :md="12">
            <AFormItem v-if="!companyForm.id" label="管理员用户名">
              <AInput
                v-model:value="companyForm.adminUsername"
                placeholder="新增公司时必填，用于创建默认管理员账号"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem
              v-if="isCompanyHqType(companyForm.typeCode)"
              label="销售组织"
            >
              <AInput
                v-model:value="companyForm.salesOrg"
                placeholder="请输入销售组织"
              />
            </AFormItem>
          </ACol>
          <ACol :span="24">
            <AFormItem label="状态">
              <ARadioGroup v-model:value="companyForm.status">
                <ARadio :value="1">启用</ARadio>
                <ARadio :value="0">停用</ARadio>
              </ARadioGroup>
            </AFormItem>
          </ACol>
        </ARow>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="companyFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitCompanyForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="companyTypeFormOpen" title="公司类型" :width="420">
      <AForm layout="vertical" class="mt-8px">
        <AFormItem label="类型名称" required>
          <AInput
            v-model:value="companyTypeForm.typeName"
            placeholder="如 总部A"
          />
        </AFormItem>
        <AFormItem label="类型编码" required>
          <AInput
            v-model:value="companyTypeForm.typeCode"
            placeholder="如 HQ_A、FIRST"
            :disabled="!!companyTypeForm.id"
          />
        </AFormItem>
        <AFormItem label="主体类型" required>
          <ASelect
            v-model:value="companyTypeForm.subjectType"
            placeholder="请选择"
            :options="[
              { label: '平台', value: 'PLATFORM' },
              { label: '总部', value: 'HQ' },
              { label: '网点', value: 'SERVICE' },
            ]"
          />
        </AFormItem>
        <AFormItem label="排序">
          <AInputNumber
            v-model:value="companyTypeForm.orderNum"
            :min="0"
            class="w-full"
          />
        </AFormItem>
        <AFormItem label="备注">
          <ATextarea
            v-model:value="companyTypeForm.remark"
            :rows="2"
            placeholder="请输入备注"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="companyTypeFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitCompanyTypeForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="companyTypeMenuOpen"
      :title="companyTypeMenuTitle"
      :width="680"
    >
      <ASpin :spinning="companyTypeMenuLoading">
        <ATree
          v-model:checked-keys="companyTypeMenuCheckedKeys"
          checkable
          :tree-data="companyTypeMenuTreeData"
          :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
          class="max-h-420px overflow-auto"
        />
      </ASpin>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="companyTypeMenuOpen = false">取消</AButton>
          <AButton
            type="primary"
            :loading="companyTypeMenuSubmitting"
            @click="submitCompanyTypeMenuAssign"
          >
            确定
          </AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="regionFormOpen"
      :title="regionForm.id ? '编辑大区' : '新增大区'"
      :width="460"
    >
      <AForm layout="vertical" class="mt-12px">
        <AFormItem label="大区编码">
          <AInput
            v-model:value="regionForm.regionCode"
            placeholder="如：HD"
            :maxlength="32"
          />
        </AFormItem>
        <AFormItem label="大区名称" required>
          <AInput
            v-model:value="regionForm.regionName"
            placeholder="如：华东大区"
          />
        </AFormItem>
        <AFormItem label="备注">
          <ATextarea v-model:value="regionForm.remark" :rows="3" />
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
      v-model:open="previewOpen"
      title="外部公司 CRM 导入预览"
      :width="720"
    >
      <div class="mb-8px">
        <AInput
          v-model:value="previewCustId"
          placeholder="custId / 主键"
          class="max-w-240px"
        />
        <AButton
          class="ml-8px"
          type="primary"
          :loading="previewLoading"
          @click="loadPreview"
          >加载</AButton
        >
      </div>
      <pre
        class="max-h-400px overflow-auto rounded-4px bg-gray-50 p-12px text-12px dark:bg-dark-800"
        >{{ JSON.stringify(previewData, null, 2) }}</pre
      >
      <template #footer>
        <ASpace :size="16">
          <AButton @click="previewOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer
      v-model:open="companyCrmImportOpen"
      title="选择 CRM 公司"
      :width="1100"
    >
      <AForm layout="inline" class="page-search-toolbar--inline mb-12px">
        <AFormItem label="SAP 公司编码">
          <AInput
            v-model:value="companyCrmImportQuery.companyCode"
            allow-clear
            placeholder="请输入 SAP 公司编码"
          />
        </AFormItem>
        <AFormItem label="公司名称">
          <AInput
            v-model:value="companyCrmImportQuery.companyName"
            allow-clear
            placeholder="请输入公司名称"
          />
        </AFormItem>
        <AFormItem label="CRM 状态">
          <ASelect
            v-model:value="companyCrmImportQuery.custState"
            allow-clear
            placeholder="全部"
            class="min-w-160px"
            :options="
              externalStateOptions.map((item) => ({
                label: item.label,
                value: item.value,
              }))
            "
          />
        </AFormItem>
        <AFormItem>
          <AButton type="primary" @click="handleCompanyCrmImportSearch"
            >搜索</AButton
          >
          <AButton class="ml-8px" @click="resetCompanyCrmImportSearch"
            >重置</AButton
          >
        </AFormItem>
      </AForm>
      <ATable
        :loading="companyCrmImportLoading"
        :data-source="companyCrmImportRows"
        row-key="id"
        size="small"
        :pagination="{
          current: companyCrmImportQuery.pageNum,
          pageSize: companyCrmImportQuery.pageSize,
          total: companyCrmImportTotal,
          showSizeChanger: true,
          onChange: (page: number, pageSize?: number) => {
            companyCrmImportQuery.pageNum = page;
            if (pageSize) companyCrmImportQuery.pageSize = pageSize;
            loadCompanyCrmImportList();
          },
        }"
        :columns="[
          {
            title: 'SAP 公司编码',
            dataIndex: 'companyCode',
            key: 'companyCode',
            width: 140,
          },
          {
            title: '公司名称',
            dataIndex: 'companyName',
            key: 'companyName',
            width: 180,
          },
          {
            title: '公司简称',
            dataIndex: 'companyShortName',
            key: 'companyShortName',
            width: 140,
          },
          {
            title: '建议类型',
            dataIndex: 'typeCode',
            key: 'typeCode',
            width: 120,
          },
          {
            title: '联系人',
            dataIndex: 'contactName',
            key: 'contactName',
            width: 100,
          },
          {
            title: '联系电话',
            dataIndex: 'contactPhone',
            key: 'contactPhone',
            width: 130,
          },
          {
            title: '地址',
            dataIndex: 'address',
            key: 'address',
            ellipsis: true,
          },
          { title: '操作', key: 'actions', width: 90, fixed: 'right' },
        ]"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'typeCode'">
            {{ record.typeCode || "-" }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <AButton
              type="link"
              size="small"
              class="table-action-link--processing"
              @click="useCompanyCrmImportRow(record)"
            >
              选择
            </AButton>
          </template>
        </template>
      </ATable>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="companyCrmImportOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>
  </div>
</template>

<style scoped>
.company-address-block {
  margin-bottom: 8px;
  padding: 12px 12px 4px;
  border: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 6px;
  background: var(--ant-color-fill-tertiary, #fafafa);
}

.company-address-block__title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ant-color-text, rgba(0, 0, 0, 0.88));
}

.company-address-block__detail {
  margin-bottom: 8px;
}
</style>
