<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { tagColorEnabled, tagColorPositiveNeutral } from '@/constants/list-status-tag';
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
  updateFirstSecondRelation,
  updateHqFirstContract,
  updateRegion
} from '@/service/api';
import type { SysCompanyDTO, SysCompanyQuery } from '@/service/api';
import { useTableScroll } from '@/hooks/common/table';
import { getResponseMsg } from '@/service/request/shared';

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
type TabKey = 'companyType' | 'company' | 'hqFirst' | 'firstSecond' | 'external' | 'area';

const loading = ref(false);
const activeTab = ref<TabKey>('companyType');
const rows = ref<RowData[]>([]);
const total = ref(0);
const route = useRoute();

const ROUTE_NAME_TO_TAB_KEY: Record<string, TabKey> = {
  'org_company-type': 'companyType',
  org_company: 'company',
  org_contract: 'hqFirst',
  org_region: 'area'
};

const companyQuery = reactive<SysCompanyQuery>({
  pageNum: 1,
  pageSize: 10,
  companyName: undefined,
  typeCode: undefined,
  category: undefined,
  status: undefined
});

const hqFirstQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  hqCompanyId: undefined as number | undefined
});
const firstSecondQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  firstCompanyId: undefined as number | undefined
});
const externalQuery = reactive({ pageNum: 1, pageSize: 10, companyName: '' });

/** 与 jasic-ui `views/org/contract/index.vue` CRM 总部一级导入筛选一致 */
const crmHqQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  hqCompanyId: undefined as number | undefined,
  firstCompanyId: undefined as number | undefined,
  regionId: undefined as number | undefined,
  kunnr: '',
  showAbnormal: false
});

/** 与 jasic-ui 一级二级 CRM 导入筛选一致 */
const crmFsQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  firstCompanyId: undefined as number | undefined,
  secondCompanyId: undefined as number | undefined,
  firstCompanyCode: '',
  secondCompanyCode: '',
  showAbnormal: false
});

const hqCompanyOptions = ref<RowData[]>([]);
const firstCompanyOptions = ref<RowData[]>([]);
const secondCompanyOptions = ref<RowData[]>([]);
const crmImportRegionOptions = ref<RowData[]>([]);
const hqFormRegionOptions = ref<RowData[]>([]);

const hqFormOpen = ref(false);
const hqForm = reactive<HqFormModel>({
  id: undefined,
  hqCompanyId: undefined,
  firstCompanyId: undefined,
  regionId: undefined,
  contractTime: '',
  status: 1
});

const fsFormOpen = ref(false);
const fsForm = reactive<FsFormModel>({
  id: undefined,
  firstCompanyId: undefined,
  secondCompanyId: undefined
});

const companyFormOpen = ref(false);
const companyForm = reactive<Partial<SysCompanyDTO> & { id?: number }>({
  id: undefined,
  companyName: '',
  companyShortName: '',
  companyCode: '',
  typeCode: '',
  contactName: '',
  contactPhone: '',
  servicePhone: '',
  sourceType: 'MANUAL',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  detailAddress: '',
  adminUsername: '',
  salesOrg: '',
  status: 1,
  remark: ''
});

const previewOpen = ref(false);
const previewCustId = ref<string | number>('');
const previewLoading = ref(false);
const previewData = ref<unknown>(null);
const companyCrmImportOpen = ref(false);
const companyCrmImportLoading = ref(false);
const companyCrmImportRows = ref<RowData[]>([]);
const companyCrmImportTotal = ref(0);
const companyCrmImportQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  companyCode: '',
  companyName: '',
  custState: undefined as number | undefined
});
const externalStateOptions = [
  { value: 0, label: '待审核' },
  { value: 1, label: '审核通过' },
  { value: 2, label: '审核不通过' },
  { value: 3, label: '注销' },
  { value: 4, label: '资料已保存' },
  { value: 5, label: '申请注销' },
  { value: 6, label: '资料未填写' },
  { value: 9, label: '删除' }
];

const regionQueryCompanyId = ref<number | undefined>(undefined);
const regionFormOpen = ref(false);
const regionForm = reactive({
  id: undefined as number | undefined,
  companyId: undefined as number | undefined,
  regionCode: '',
  regionName: '',
  remark: ''
});

const selectedHqSnapshotIds = ref<Array<string | number>>([]);
const selectedFsSnapshotIds = ref<Array<string | number>>([]);
const hqCrmImportOpen = ref(false);
const companyTypeOptions = ref<RowData[]>([]);
const companyTypeFormOpen = ref(false);
const companyTypeForm = reactive({
  id: undefined as number | undefined,
  typeName: '',
  typeCode: '',
  subjectType: 'SERVICE',
  orderNum: 0,
  remark: ''
});
const companyTypeMenuOpen = ref(false);
const companyTypeMenuLoading = ref(false);
const companyTypeMenuSubmitting = ref(false);
const companyTypeMenuTitle = ref('');
const companyTypeMenuTreeData = ref<any[]>([]);
const companyTypeMenuCheckedKeys = ref<Array<string | number>>([]);
const companyTypeMenuTypeCode = ref('');

const provinceOptions = ref<RowData[]>([]);
const cityOptions = ref<RowData[]>([]);
const districtOptions = ref<RowData[]>([]);

function getCompanySubjectType(typeCode?: string) {
  const target = String(typeCode || '');
  if (!target) return '';
  const matched = companyTypeOptions.value.find(item => String(item.typeCode || '') === target);
  return String(matched?.subjectType || '');
}

function isCompanyHqType(typeCode?: string) {
  return getCompanySubjectType(typeCode) === 'HQ';
}

function getCompanyTypeLabel(typeCode?: string) {
  const target = String(typeCode || '');
  if (!target) return '';
  const matched = companyTypeOptions.value.find(item => String(item.typeCode || '') === target);
  return String(matched?.typeName || '');
}

function formatCompanyRegion(record: RowData) {
  const parts = [record.provinceName, record.cityName, record.districtName]
    .map(item => String(item || '').trim())
    .filter(Boolean);
  return parts.length ? parts.join('/') : '-';
}

const isContractPageTab = computed(() => activeTab.value === 'hqFirst' || activeTab.value === 'firstSecond');

const columns = computed(() => {
  switch (activeTab.value) {
    case 'company':
      return [
        { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
        { title: '公司名称', dataIndex: 'companyName', key: 'companyName', width: 200 },
        { title: '公司简称', dataIndex: 'companyShortName', key: 'companyShortName', width: 140 },
        { title: '公司编码', dataIndex: 'companyCode', key: 'companyCode', width: 140 },
        { title: '公司类型', dataIndex: 'typeCode', key: 'typeCode', width: 120 },
        { title: '主体类型', dataIndex: 'subjectType', key: 'subjectType', width: 100 },
        { title: '来源', dataIndex: 'sourceType', key: 'sourceType', width: 90 },
        { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
        { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
        { title: '客服电话', dataIndex: 'servicePhone', key: 'servicePhone', width: 140 },
        { title: '销售组织', dataIndex: 'salesOrg', key: 'salesOrg', width: 120 },
        { title: '地区', dataIndex: 'region', key: 'region', width: 180 },
        { title: '详细地址', dataIndex: 'detailAddress', key: 'detailAddress', ellipsis: true, width: 220 },
        { title: '地理解析', dataIndex: 'geocodeStatus', key: 'geocodeStatus', width: 100 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
        { title: '操作', dataIndex: 'actions', key: 'actions', width: 160, fixed: 'right' as const }
      ];
    case 'hqFirst':
      return [
        { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
        { title: '总部', dataIndex: 'hqCompanyName', key: 'hqCompanyName', width: 180 },
        { title: '一级网点', dataIndex: 'firstCompanyName', key: 'firstCompanyName', width: 180 },
        { title: '大区', dataIndex: 'regionName', key: 'regionName', width: 140 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
        { title: '签约时间', dataIndex: 'contractTime', key: 'contractTime', width: 170 },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
        { title: '操作', dataIndex: 'actions', key: 'actions', width: 160, fixed: 'right' as const }
      ];
    case 'firstSecond':
      return [
        { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
        { title: '一级', dataIndex: 'firstCompanyName', key: 'firstCompanyName', width: 180 },
        { title: '二级', dataIndex: 'secondCompanyName', key: 'secondCompanyName', width: 180 },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
        { title: '操作', dataIndex: 'actions', key: 'actions', width: 120, fixed: 'right' as const }
      ];
    case 'external':
      return [
        { title: '名称', dataIndex: 'companyName', key: 'companyName', width: 200 },
        { title: '编码', dataIndex: 'companyCode', key: 'companyCode', width: 140 },
        { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
        { title: '电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
        { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
        { title: '操作', dataIndex: 'actions', key: 'actions', width: 100, fixed: 'right' as const }
      ];
    case 'area':
      return [
        { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
        { title: '大区编码', dataIndex: 'regionCode', key: 'regionCode', width: 120 },
        { title: '大区名称', dataIndex: 'regionName', key: 'regionName', width: 200 },
        { title: '备注', dataIndex: 'remark', key: 'remark' },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
        { title: '操作', dataIndex: 'actions', key: 'actions', width: 160, fixed: 'right' as const }
      ];
    case 'companyType':
      return [
        { title: '类型名称', dataIndex: 'typeName', key: 'typeName', width: 200 },
        { title: '类型编码', dataIndex: 'typeCode', key: 'typeCode', width: 160 },
        { title: '主体类型', dataIndex: 'subjectType', key: 'subjectType', width: 120 },
        { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
        { title: '操作', dataIndex: 'actions', key: 'actions', width: 220, fixed: 'right' as const }
      ];
    default:
      return [];
  }
});

/** 与 jasic-ui `contract/index.vue` 总部一级 CRM 导入表列一致 */
const crmHqImportColumns = [
  { title: '客户编码', dataIndex: 'kunnr', key: 'kunnr', width: 120 },
  { title: 'CRM企业名称', dataIndex: 'crmCompanyName', key: 'crmCompanyName', ellipsis: true, width: 180 },
  { title: '销售组织', dataIndex: 'salesOrg', key: 'salesOrg', width: 120 },
  { title: 'CRM大区', key: 'hqCrmRegion', ellipsis: true, width: 180 },
  { title: '一级公司', dataIndex: 'firstCompanyName', key: 'firstCompanyName', ellipsis: true, width: 160 },
  { title: '本地大区', dataIndex: 'localRegionName', key: 'localRegionName', ellipsis: true, width: 150 },
  { title: 'CRM状态', key: 'hqCrmAlive', width: 100 },
  { title: '导入状态', key: 'hqCrmImportSts', width: 110 },
  { title: '说明', dataIndex: 'matchRemark', key: 'matchRemark', ellipsis: true, width: 200 }
];

/** 与 jasic-ui 一级二级 CRM 导入表列一致 */
const crmFsImportColumns = [
  { title: '一级CRM ID', dataIndex: 'firstCustId', key: 'firstCustId', width: 110 },
  { title: '一级编码', dataIndex: 'firstCompanyCode', key: 'firstCompanyCode', width: 120 },
  { title: '一级名称', dataIndex: 'firstCompanyName', key: 'firstCompanyName', ellipsis: true, width: 160 },
  { title: '二级CRM ID', dataIndex: 'secondCustId', key: 'secondCustId', width: 110 },
  { title: '二级编码', dataIndex: 'secondCompanyCode', key: 'secondCompanyCode', width: 120 },
  { title: '二级名称', dataIndex: 'secondCompanyName', key: 'secondCompanyName', ellipsis: true, width: 160 },
  { title: '本地一级', dataIndex: 'localFirstCompanyName', key: 'localFirstCompanyName', ellipsis: true, width: 140 },
  { title: '本地二级', dataIndex: 'localSecondCompanyName', key: 'localSecondCompanyName', ellipsis: true, width: 140 },
  { title: '来源更新时间', dataIndex: 'crmOperTime', key: 'crmOperTime', width: 160 },
  { title: '导入状态', key: 'fsCrmImportSts', width: 110 },
  { title: '说明', dataIndex: 'matchRemark', key: 'matchRemark', ellipsis: true, width: 220 }
];

const displayColumns = computed(() => columns.value);

const crmTableScrollX = computed(() => {
  if (activeTab.value === 'company') return 900;
  return 1100;
});

const { tableWrapperRef, scrollConfig } = useTableScroll(crmTableScrollX);

function pickRows(data: any) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.records)) return data.records;
  return [];
}

function pickTotal(data: any) {
  return Number(data?.total) || 0;
}

async function loadCompanyOptionsForCrm() {
  if (hqCompanyOptions.value.length && firstCompanyOptions.value.length && secondCompanyOptions.value.length) {
    return;
  }
  const base = { pageNum: 1, pageSize: 999 };
  const [hqRes, firstRes, secondRes] = await Promise.all([
    listCompany({ ...base, category: 'HQ' }),
    listCompany({ ...base, category: 'FIRST_LEVEL' }),
    listCompany({ ...base, category: 'SECOND_LEVEL' })
  ]);
  hqCompanyOptions.value = pickRows(hqRes.data);
  firstCompanyOptions.value = pickRows(firstRes.data);
  secondCompanyOptions.value = pickRows(secondRes.data);
}

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

async function ensureCompanyTypeOptions() {
  const { data } = await listCompanyType();
  const list = pickRows(data);
  companyTypeOptions.value = list;
}

async function loadList() {
  loading.value = true;
  try {
    switch (activeTab.value) {
      case 'company': {
        const { data } = await listCompany({
          ...companyQuery,
          companyName: companyQuery.companyName || undefined,
          typeCode: companyQuery.typeCode || undefined,
          status: companyQuery.status
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case 'hqFirst': {
        const { data } = await listHqFirstContract({
          pageNum: hqFirstQuery.pageNum,
          pageSize: hqFirstQuery.pageSize,
          hqCompanyId: hqFirstQuery.hqCompanyId
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case 'firstSecond': {
        const { data } = await listFirstSecondRelation({
          pageNum: firstSecondQuery.pageNum,
          pageSize: firstSecondQuery.pageSize,
          firstCompanyId: firstSecondQuery.firstCompanyId
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case 'external': {
        const { data } = await listExternalCompany({
          pageNum: externalQuery.pageNum,
          pageSize: externalQuery.pageSize,
          companyName: externalQuery.companyName.trim() || undefined
        });
        rows.value = pickRows(data);
        total.value = pickTotal(data);
        break;
      }
      case 'area': {
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
      case 'companyType': {
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

function handleRegionSearch() {
  loadList();
}

function openRegionForm(record?: RowData) {
  if (!regionQueryCompanyId.value && !record?.companyId) {
    window.$message?.warning?.('请先选择总部公司');
    return;
  }
  regionForm.id = record?.id;
  regionForm.companyId = Number(record?.companyId ?? regionQueryCompanyId.value);
  regionForm.regionCode = record?.regionCode ?? '';
  regionForm.regionName = record?.regionName ?? '';
  regionForm.remark = record?.remark ?? '';
  regionFormOpen.value = true;
}

async function submitRegionForm() {
  if (!regionForm.regionName.trim()) {
    window.$message?.warning?.('请输入大区名称');
    return;
  }
  const payload = {
    id: regionForm.id,
    companyId: regionForm.companyId,
    regionCode: regionForm.regionCode.trim() || undefined,
    regionName: regionForm.regionName.trim(),
    remark: regionForm.remark
  };
  if (payload.id) await updateRegion(payload);
  else await addRegion(payload);
  window.$message?.success?.('操作成功');
  regionFormOpen.value = false;
  await loadList();
}

async function removeRegion(record: RowData) {
  await deleteRegion(record.id);
  window.$message?.success?.('删除成功');
  await loadList();
}

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
      hqCompanyId: crmHqQuery.hqCompanyId,
      firstCompanyId: crmHqQuery.firstCompanyId,
      regionId: crmHqQuery.regionId,
      kunnr: crmHqQuery.kunnr.trim() || undefined,
      showAbnormal: crmHqQuery.showAbnormal
    });
    rows.value = pickRows(data);
    total.value = pickTotal(data);
    selectedHqSnapshotIds.value = [];
  } finally {
    loading.value = false;
  }
}

async function loadCrmFsRows() {
  loading.value = true;
  try {
    const { data } = await listCrmFirstSecondRelationImport({
      pageNum: crmFsQuery.pageNum,
      pageSize: crmFsQuery.pageSize,
      firstCompanyId: crmFsQuery.firstCompanyId,
      secondCompanyId: crmFsQuery.secondCompanyId,
      firstCompanyCode: crmFsQuery.firstCompanyCode.trim() || undefined,
      secondCompanyCode: crmFsQuery.secondCompanyCode.trim() || undefined,
      showAbnormal: crmFsQuery.showAbnormal
    });
    rows.value = pickRows(data);
    total.value = pickTotal(data);
    selectedFsSnapshotIds.value = [];
  } finally {
    loading.value = false;
  }
}

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

function handleCrmHqSearch() {
  if (!crmHqQuery.hqCompanyId) {
    window.$message?.warning?.('请选择总部公司');
    return;
  }
  crmHqQuery.pageNum = 1;
  loadCrmHqRows();
}

function resetHqCrmQuery() {
  const hqCompanyId = crmHqQuery.hqCompanyId;
  crmHqQuery.firstCompanyId = undefined;
  crmHqQuery.regionId = undefined;
  crmHqQuery.kunnr = '';
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

function handleCrmFsSearch() {
  crmFsQuery.pageNum = 1;
  loadCrmFsRows();
}

function resetFsCrmQuery() {
  crmFsQuery.firstCompanyId = undefined;
  crmFsQuery.secondCompanyId = undefined;
  crmFsQuery.firstCompanyCode = '';
  crmFsQuery.secondCompanyCode = '';
  crmFsQuery.showAbnormal = false;
  crmFsQuery.pageNum = 1;
  crmFsQuery.pageSize = 10;
  loadCrmFsRows();
}

function tableRowKey(record: RowData) {
  return record.id ?? record.batchId ?? record.custId ?? `${record.areaCode ?? ''}-${record.typeCode ?? ''}`;
}

function applyActiveTabByRoute(tab: TabKey) {
  activeTab.value = tab;
  if (tab === 'company') {
    companyQuery.pageNum = 1;
  }
  if (tab === 'hqFirst' || tab === 'firstSecond') {
    hqFirstQuery.pageNum = 1;
    firstSecondQuery.pageNum = 1;
    loadCompanyOptionsForCrm();
  }
  if (tab === 'external') {
    externalQuery.pageNum = 1;
  }
  if (tab === 'area') {
    loadCompanyOptionsForCrm().then(() => {
      if (!regionQueryCompanyId.value && hqCompanyOptions.value.length) {
        regionQueryCompanyId.value = Number(hqCompanyOptions.value[0].id);
        loadList();
      }
    });
  }
  loadList();
}

function handleCompanySearch() {
  companyQuery.pageNum = 1;
  loadList();
}

function resetCompanyQuery() {
  companyQuery.companyName = undefined;
  companyQuery.typeCode = undefined;
  companyQuery.category = undefined;
  companyQuery.status = undefined;
  handleCompanySearch();
}

function handleExternalSearch() {
  externalQuery.pageNum = 1;
  loadList();
}

function resetExternalQuery() {
  externalQuery.companyName = '';
  handleExternalSearch();
}

function handleHqFirstSearch() {
  hqFirstQuery.pageNum = 1;
  loadList();
}

function handleFirstSecondSearch() {
  firstSecondQuery.pageNum = 1;
  loadList();
}

function handleContractTabChange(tab: string | number) {
  const next = String(tab);
  if (next === 'hqFirst' || next === 'firstSecond') {
    applyActiveTabByRoute(next);
  }
}

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

function syncActiveTabByRouteName(routeName: unknown) {
  const routeKey = String(routeName || '');
  const tab = ROUTE_NAME_TO_TAB_KEY[routeKey];
  if (!tab || tab === activeTab.value) return false;
  applyActiveTabByRoute(tab);
  return true;
}

async function openHqForm(record?: RowData) {
  await loadCompanyOptionsForCrm();
  if (record) {
    hqForm.id = record.id;
    hqForm.hqCompanyId = record.hqCompanyId;
    hqForm.firstCompanyId = record.firstCompanyId;
    hqForm.regionId = record.regionId;
    hqForm.contractTime = record.contractTime ?? '';
    hqForm.status = record.status ?? 1;
  } else {
    hqForm.id = undefined;
    hqForm.hqCompanyId = undefined;
    hqForm.firstCompanyId = undefined;
    hqForm.regionId = undefined;
    hqForm.contractTime = '';
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

async function submitHqForm() {
  if (!hqForm.hqCompanyId) {
    window.$message?.warning?.('请选择总部公司');
    return;
  }
  if (!hqForm.firstCompanyId) {
    window.$message?.warning?.('请选择一级网点');
    return;
  }
  const body = { ...hqForm };
  if (hqForm.id) {
    await updateHqFirstContract(body);
    window.$message?.success?.('操作成功');
  } else {
    await addHqFirstContract(body);
    window.$message?.success?.('操作成功');
  }
  hqFormOpen.value = false;
  loadList();
}

async function removeHq(id: number) {
  await deleteHqFirstContract(id);
  window.$message?.success?.('删除成功');
  loadList();
}

async function openFsForm(record?: RowData) {
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

async function submitFsForm() {
  if (!fsForm.firstCompanyId) {
    window.$message?.warning?.('请选择一级网点');
    return;
  }
  if (!fsForm.secondCompanyId) {
    window.$message?.warning?.('请选择二级网点');
    return;
  }
  const body = { ...fsForm };
  if (fsForm.id) {
    await updateFirstSecondRelation(body);
    window.$message?.success?.('操作成功');
  } else {
    await addFirstSecondRelation(body);
    window.$message?.success?.('操作成功');
  }
  fsFormOpen.value = false;
  loadList();
}

async function removeFs(id: number) {
  await deleteFirstSecondRelation(id);
  window.$message?.success?.('删除成功');
  loadList();
}

async function triggerCrmHqImport() {
  if (!crmHqQuery.hqCompanyId) {
    window.$message?.warning?.('请选择总部公司');
    return;
  }
  if (!selectedHqSnapshotIds.value.length) {
    window.$message?.warning?.('请选择要导入的CRM签约关系');
    return;
  }
  const { data } = await importCrmHqFirstContract({
    hqCompanyId: crmHqQuery.hqCompanyId,
    snapshotIds: selectedHqSnapshotIds.value
  });
  const d = (data || {}) as RowData;
  window.$message?.success?.(
    `选中 ${d.selectedCount ?? 0} 条，成功 ${d.successCount ?? 0} 条，已存在 ${d.existedCount ?? 0} 条，失败 ${d.failedCount ?? 0} 条`
  );
  selectedHqSnapshotIds.value = [];
  hqCrmImportOpen.value = false;
  await loadList();
}

async function triggerCrmFsImport() {
  if (!selectedFsSnapshotIds.value.length) {
    window.$message?.warning?.('请选择要导入的一级二级关系');
    return;
  }
  const { data } = await importCrmFirstSecondRelation({ snapshotIds: selectedFsSnapshotIds.value });
  const d = (data || {}) as RowData;
  window.$message?.success?.(
    `选中 ${d.selectedCount ?? 0} 条，成功 ${d.successCount ?? 0} 条，已存在 ${d.existedCount ?? 0} 条，冲突 ${d.conflictCount ?? 0} 条，失败 ${d.failedCount ?? 0} 条`
  );
  selectedFsSnapshotIds.value = [];
  await loadList();
}

function openPreview(custId: string | number) {
  previewCustId.value = custId;
  previewOpen.value = true;
  previewData.value = null;
  loadPreview();
}

async function loadPreview() {
  if (previewCustId.value === '' || previewCustId.value === undefined) {
    window.$message?.warning?.('请填写客户/外部主键 ID');
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

function extractSnapshotId(record: RowData) {
  return record.snapshotId ?? record.snapshotID ?? record.id;
}

function onSelectHqCrmRows(_keys: Array<string | number>, selectedRows: RowData[]) {
  selectedHqSnapshotIds.value = (selectedRows || []).map(extractSnapshotId).filter(id => id != null && id !== '');
}

function onSelectFsCrmRows(_keys: Array<string | number>, selectedRows: RowData[]) {
  selectedFsSnapshotIds.value = (selectedRows || []).map(extractSnapshotId).filter(id => id != null && id !== '');
}

function crmRowSelection() {
  return undefined;
}

function openCompanyForm(record?: RowData) {
  loadProvinceOptions();
  ensureCompanyTypeOptions();
  if (record) {
    Object.assign(companyForm, {
      id: record.id,
      companyName: record.companyName ?? '',
      companyShortName: record.companyShortName ?? '',
      companyCode: record.companyCode ?? '',
      typeCode: record.typeCode ?? '',
      contactName: record.contactName ?? '',
      contactPhone: record.contactPhone ?? '',
      servicePhone: record.servicePhone ?? '',
      sourceType: record.sourceType ?? 'MANUAL',
      provinceCode: record.provinceCode ?? '',
      cityCode: record.cityCode ?? '',
      districtCode: record.districtCode ?? '',
      detailAddress: record.detailAddress ?? '',
      adminUsername: record.adminUsername ?? '',
      salesOrg: record.salesOrg ?? '',
      status: record.status ?? 1,
      remark: record.remark ?? ''
    });
    loadCompanyAreaOptionsForEdit(record.provinceCode, record.cityCode);
  } else {
    Object.assign(companyForm, {
      id: undefined,
      companyName: '',
      companyShortName: '',
      companyCode: '',
      typeCode: '',
      contactName: '',
      contactPhone: '',
      servicePhone: '',
      sourceType: 'MANUAL',
      provinceCode: '',
      cityCode: '',
      districtCode: '',
      detailAddress: '',
      adminUsername: '',
      salesOrg: '',
      status: 1,
      remark: ''
    });
  }
  companyFormOpen.value = true;
}

async function submitCompanyForm() {
  if (!String(companyForm.companyName || '').trim()) {
    window.$message?.warning?.('请输入公司名称');
    return;
  }
  if (!String(companyForm.typeCode || '').trim()) {
    window.$message?.warning?.('请选择公司类型');
    return;
  }
  if (!isCompanyHqType(companyForm.typeCode) && !String(companyForm.companyCode || '').trim()) {
    window.$message?.warning?.('请输入公司编码');
    return;
  }
  if (isCompanyHqType(companyForm.typeCode) && !String(companyForm.salesOrg || '').trim()) {
    window.$message?.warning?.('请输入销售组织');
    return;
  }
  if (!String(companyForm.contactName || '').trim()) {
    window.$message?.warning?.('请输入联系人');
    return;
  }
  if (!String(companyForm.contactPhone || '').trim()) {
    window.$message?.warning?.('请输入联系电话');
    return;
  }
  if (!String(companyForm.provinceCode || '').trim()) {
    window.$message?.warning?.('请选择省份');
    return;
  }
  if (!String(companyForm.cityCode || '').trim()) {
    window.$message?.warning?.('请选择城市');
    return;
  }
  if (!String(companyForm.districtCode || '').trim()) {
    window.$message?.warning?.('请选择区县');
    return;
  }
  if (!String(companyForm.detailAddress || '').trim()) {
    window.$message?.warning?.('请输入详细地址');
    return;
  }
  if (!companyForm.id && !String(companyForm.adminUsername || '').trim()) {
    window.$message?.warning?.('请输入管理员用户名');
    return;
  }

  const province = provinceOptions.value.find(item => String(item.areaCode) === String(companyForm.provinceCode));
  const city = cityOptions.value.find(item => String(item.areaCode) === String(companyForm.cityCode));
  const district = districtOptions.value.find(item => String(item.areaCode) === String(companyForm.districtCode));
  const body = { ...companyForm } as SysCompanyDTO;
  body.provinceName = province?.areaName;
  body.cityName = city?.areaName;
  body.districtName = district?.areaName;

  // 接口失败时 request 侧会统一弹出错误信息，这里只处理成功时的接口返回 msg。
  const result = companyForm.id ? await updateCompany(body) : await addCompany(body);
  if (!result) return;

  const response = (result as unknown as { response?: unknown }).response;
  window.$message?.success?.(getResponseMsg(response, '操作成功'));
  companyFormOpen.value = false;
  await loadList();
}

async function removeCompany(id: number) {
  await deleteCompany(id);
  window.$message?.success?.('删除成功');
  loadList();
}

function openCompanyTypeForm(record?: RowData) {
  companyTypeForm.id = record?.id;
  companyTypeForm.typeName = record?.typeName ?? '';
  companyTypeForm.typeCode = record?.typeCode ?? '';
  companyTypeForm.subjectType = record?.subjectType ?? 'SERVICE';
  companyTypeForm.orderNum = Number(record?.orderNum ?? 0);
  companyTypeForm.remark = record?.remark ?? '';
  companyTypeFormOpen.value = true;
}

async function submitCompanyTypeForm() {
  const payload = {
    id: companyTypeForm.id,
    typeName: companyTypeForm.typeName,
    typeCode: companyTypeForm.typeCode,
    subjectType: companyTypeForm.subjectType as 'PLATFORM' | 'HQ' | 'SERVICE',
    orderNum: Number(companyTypeForm.orderNum ?? 0),
    remark: companyTypeForm.remark
  };
  if (payload.id) await updateCompanyType(payload);
  else await addCompanyType(payload);
  companyTypeFormOpen.value = false;
  window.$message?.success?.('操作成功');
  await loadList();
}

async function removeCompanyType(id: number) {
  await deleteCompanyType(id);
  window.$message?.success?.('删除成功');
  await loadList();
}

async function openCompanyTypeMenuAssign(record: RowData) {
  const typeCode = String(record.typeCode || '');
  if (!typeCode) {
    window.$message?.warning?.('当前类型缺少 typeCode');
    return;
  }
  companyTypeMenuTypeCode.value = typeCode;
  companyTypeMenuTitle.value = `分配菜单 - ${record.typeName || ''}（${typeCode}）`;
  companyTypeMenuCheckedKeys.value = [];
  companyTypeMenuTreeData.value = [];
  companyTypeMenuOpen.value = true;
  companyTypeMenuLoading.value = true;
  try {
    const [treeRes, idsRes] = await Promise.all([typeCodeMenuTree(typeCode), typeCodeMenuIds(typeCode)]);
    companyTypeMenuTreeData.value = pickRows(treeRes.data);
    companyTypeMenuCheckedKeys.value = Array.isArray(idsRes.data) ? idsRes.data : [];
  } finally {
    companyTypeMenuLoading.value = false;
  }
}

async function submitCompanyTypeMenuAssign() {
  if (!companyTypeMenuTypeCode.value) return;
  companyTypeMenuSubmitting.value = true;
  try {
    await assignTypeCodeMenus(companyTypeMenuTypeCode.value, companyTypeMenuCheckedKeys.value);
    companyTypeMenuOpen.value = false;
    window.$message?.success?.('菜单分配保存成功');
  } finally {
    companyTypeMenuSubmitting.value = false;
  }
}

async function loadProvinceOptions() {
  const { data } = await listAreaOptions();
  provinceOptions.value = Array.isArray(data) ? data : pickRows(data);
}

async function loadCompanyAreaOptionsForEdit(provinceCode?: string | number, cityCode?: string | number) {
  cityOptions.value = [];
  districtOptions.value = [];

  const province = String(provinceCode ?? '').trim();
  if (!province) return;

  const cityRes = await listAreaOptions(province);
  cityOptions.value = Array.isArray(cityRes.data) ? cityRes.data : pickRows(cityRes.data);

  const city = String(cityCode ?? '').trim();
  if (!city) return;

  const districtRes = await listAreaOptions(city);
  districtOptions.value = Array.isArray(districtRes.data) ? districtRes.data : pickRows(districtRes.data);
}

async function onCompanyProvinceChange(code?: any) {
  companyForm.cityCode = '';
  companyForm.districtCode = '';
  cityOptions.value = [];
  districtOptions.value = [];
  if (!code) return;
  const { data } = await listAreaOptions(code);
  cityOptions.value = Array.isArray(data) ? data : pickRows(data);
}

async function onCompanyCityChange(code?: any) {
  companyForm.districtCode = '';
  districtOptions.value = [];
  if (!code) return;
  const { data } = await listAreaOptions(code);
  districtOptions.value = Array.isArray(data) ? data : pickRows(data);
}

async function prefillCompanyByCrm() {
  companyCrmImportOpen.value = true;
  companyCrmImportQuery.pageNum = 1;
  await loadCompanyCrmImportList();
}

async function loadCompanyCrmImportList() {
  companyCrmImportLoading.value = true;
  try {
    const { data } = await listExternalCompany({
      pageNum: companyCrmImportQuery.pageNum,
      pageSize: companyCrmImportQuery.pageSize,
      companyCode: companyCrmImportQuery.companyCode.trim() || undefined,
      companyName: companyCrmImportQuery.companyName.trim() || undefined,
      custState: companyCrmImportQuery.custState
    });
    companyCrmImportRows.value = pickRows(data);
    companyCrmImportTotal.value = pickTotal(data);
  } finally {
    companyCrmImportLoading.value = false;
  }
}

function handleCompanyCrmImportSearch() {
  companyCrmImportQuery.pageNum = 1;
  loadCompanyCrmImportList();
}

function resetCompanyCrmImportSearch() {
  companyCrmImportQuery.pageNum = 1;
  companyCrmImportQuery.pageSize = 10;
  companyCrmImportQuery.companyCode = '';
  companyCrmImportQuery.companyName = '';
  companyCrmImportQuery.custState = undefined;
  loadCompanyCrmImportList();
}

async function useCompanyCrmImportRow(row: RowData) {
  const custId = row.custId ?? row.id;
  if (!custId) return;
  const { data } = await getExternalCompanyImportPreview(custId);
  const preview = ((data as RowData) || {}) as RowData;
  if (preview.existingCompanyId) {
    companyCrmImportOpen.value = false;
    const existing = rows.value.find(item => Number(item.id) === Number(preview.existingCompanyId));
    if (existing) {
      openCompanyForm(existing);
    } else {
      window.$message?.warning?.('该公司已存在，请在列表中编辑');
    }
    return;
  }
  if (preview.canImport === false) {
    window.$message?.warning?.(String(preview.importDisabledReason || '当前记录不可导入'));
    return;
  }
  companyCrmImportOpen.value = false;
  openCompanyForm();
  Object.assign(companyForm, {
    companyName: preview.companyName ?? '',
    companyShortName: preview.companyShortName ?? '',
    companyCode: preview.companyCode ?? '',
    typeCode: preview.typeCode ?? '',
    contactName: preview.contactName ?? '',
    contactPhone: preview.contactPhone ?? '',
    servicePhone: preview.servicePhone ?? '',
    sourceType: preview.sourceType ?? 'CRM',
    provinceCode: preview.provinceCode ?? '',
    cityCode: preview.cityCode ?? '',
    districtCode: preview.districtCode ?? '',
    detailAddress: preview.detailAddress ?? '',
    salesOrg: preview.salesOrg ?? '',
    adminUsername: preview.adminUsername ?? preview.companyCode ?? '',
    status: preview.status == null ? 1 : preview.status,
    remark: preview.remark ?? ''
  });
  loadCompanyAreaOptionsForEdit(companyForm.provinceCode as string, companyForm.cityCode as string);
}

function companyPagination() {
  if (activeTab.value !== 'company') return false;
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
    }
  };
}

function hqPagination() {
  if (activeTab.value !== 'hqFirst') return false;
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
    }
  };
}

function fsPagination() {
  if (activeTab.value !== 'firstSecond') return false;
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
    }
  };
}

function externalPagination() {
  if (activeTab.value !== 'external') return false;
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
    }
  };
}

function mergedPagination() {
  const p = companyPagination() || hqPagination() || fsPagination() || externalPagination() || false;
  return p;
}

function tablePagination() {
  const p = mergedPagination();
  if (activeTab.value === 'companyType' || activeTab.value === 'area') return false;
  return p || false;
}

watch(
  () => route.name,
  name => {
    syncActiveTabByRouteName(name);
  }
);

onMounted(() => {
  const changedByRoute = syncActiveTabByRouteName(route.name);
  if (!changedByRoute) {
    loadList();
  }
});
</script>

<template>
  <div class="min-h-500px flex-col-stretch gap-16px overflow-hidden lt-sm:overflow-auto">
    <ACard v-if="activeTab !== 'companyType'" :bordered="false" class="card-wrapper">
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
      <AForm v-if="activeTab === 'company'" :model="companyQuery" :label-col="{ span: 5, md: 7 }" class="mb-12px">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="公司名称" class="m-0">
                  <AInput v-model:value="companyQuery.companyName" allow-clear placeholder="请输入公司名称" />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="公司类型" class="m-0">
                  <ASelect
                    v-model:value="companyQuery.typeCode"
                    allow-clear
                    placeholder="全部"
                    class="w-full"
                    :options="companyTypeOptions.map(item => ({ label: item.typeName, value: item.typeCode }))"
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
                      { label: '停用', value: 0 }
                    ]"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleCompanySearch">查询</AButton>
            <AButton :loading="loading" @click="resetCompanyQuery">重置</AButton>
          </div>
        </div>
      </AForm>

      <AForm v-if="activeTab === 'hqFirst'" :model="hqFirstQuery" :label-col="{ span: 5, md: 7 }" class="mb-12px">
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
                    :options="hqCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
                <AFormItem label="一级网点" class="m-0">
                  <ASelect
                    v-model:value="firstSecondQuery.firstCompanyId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    placeholder="全部"
                    class="w-full"
                    :options="firstCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
                    @change="handleFirstSecondSearch"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
        </div>
      </AForm>

      <AForm v-if="activeTab === 'external'" :model="externalQuery" :label-col="{ span: 5, md: 7 }" class="mb-12px">
        <div class="page-search-toolbar">
          <div class="page-search-toolbar__filters">
            <ARow :gutter="[16, 16]" wrap>
              <ACol :span="24" :md="12" :lg="6">
                <AFormItem label="名称" class="m-0">
                  <AInput v-model:value="externalQuery.companyName" allow-clear placeholder="名称筛选" />
                </AFormItem>
              </ACol>
            </ARow>
          </div>
          <div class="page-search-toolbar__actions">
            <AButton type="primary" :loading="loading" @click="handleExternalSearch">查询</AButton>
            <AButton :loading="loading" @click="resetExternalQuery">重置</AButton>
          </div>
        </div>
      </AForm>

      <AForm v-if="activeTab === 'area'" :label-col="{ span: 5, md: 7 }" class="mb-12px">
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
                    :options="hqCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
          <AButton v-if="activeTab === 'companyType'" type="primary" @click="openCompanyTypeForm()">
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
          <AButton v-if="activeTab === 'company'" type="primary" ghost @click="openCompanyForm()">新增公司</AButton>
          <AButton v-if="activeTab === 'company'" type="primary" @click="prefillCompanyByCrm">从 CRM 导入</AButton>
          <AButton v-if="activeTab === 'hqFirst'" type="primary" ghost @click="openHqForm()">新增签约</AButton>
          <AButton v-if="activeTab === 'hqFirst'" type="primary" @click="openHqCrmImport">从CRM导入</AButton>
          <AButton v-if="activeTab === 'firstSecond'" type="primary" ghost @click="openFsForm()">新增关系</AButton>
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
            <span>{{ record.regionName || '-' }}</span>
            <span v-if="record.regionCode" class="text-gray-500">（{{ record.regionCode }}）</span>
          </template>
          <template v-else-if="column.key === 'hqCrmAlive'">
            <ATag :color="tagColorPositiveNeutral(record.aliveFlag === 1)">
              {{ record.aliveFlag === 1 ? '有效' : '失效' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'hqCrmImportSts'">
            <ATag v-if="record.canImport" color="success">可导入</ATag>
            <ATag v-else-if="record.existingContract" color="processing">已存在</ATag>
            <ATag v-else color="warning">异常</ATag>
          </template>
          <template v-else-if="column.key === 'fsCrmImportSts'">
            <ATag v-if="record.canImport" color="success">可导入</ATag>
            <ATag v-else-if="record.existingRelation" color="processing">已存在</ATag>
            <ATag v-else-if="record.conflictingRelation" color="error">冲突</ATag>
            <ATag v-else color="warning">异常</ATag>
          </template>
          <template v-else-if="column.key === 'typeCode' && activeTab === 'company'">
            {{ getCompanyTypeLabel(record.typeCode) || record.typeCode || '-' }}
          </template>
          <template v-else-if="column.key === 'subjectType' && activeTab === 'company'">
            <ATag v-if="getCompanySubjectType(record.typeCode) === 'HQ'" color="warning">总部</ATag>
            <ATag v-else-if="getCompanySubjectType(record.typeCode) === 'SERVICE'" color="success">网点</ATag>
            <ATag v-else>平台</ATag>
          </template>
          <template v-else-if="column.key === 'sourceType' && activeTab === 'company'">
            <ATag :color="record.sourceType === 'CRM' ? 'default' : 'success'">{{ record.sourceType || '-' }}</ATag>
          </template>
          <template v-else-if="column.key === 'salesOrg' && activeTab === 'company'">
            {{ getCompanySubjectType(record.typeCode) === 'HQ' ? record.salesOrg || '-' : '-' }}
          </template>
          <template v-else-if="column.key === 'region' && activeTab === 'company'">
            {{ formatCompanyRegion(record) }}
          </template>
          <template v-else-if="column.key === 'geocodeStatus' && activeTab === 'company'">
            <ATag :color="record.geocodeStatus === 'SUCCESS' ? 'success' : 'error'">
              {{ record.geocodeStatus === 'SUCCESS' ? 'SUCCESS' : 'FAILED' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'status'">
            <ATag :color="tagColorEnabled(record.status === 1)">
              {{
                activeTab === 'hqFirst'
                  ? record.status === 1
                    ? '有效'
                    : '失效'
                  : record.status === 1
                    ? '启用'
                    : '停用'
              }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'actions' && activeTab === 'company'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openCompanyForm(record)">
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
          <template v-else-if="column.key === 'actions' && activeTab === 'hqFirst'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openHqForm(record)">
                编辑
              </AButton>
              <APopconfirm title="确认删除该签约关系？" @confirm="removeHq(record.id)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeTab === 'firstSecond'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openFsForm(record)">
                编辑
              </AButton>
              <APopconfirm title="确认删除该关系？" @confirm="removeFs(record.id)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeTab === 'external'">
            <AButton
              type="link"
              size="small"
              class="table-action-link--info"
              @click="openPreview(record.id ?? record.custId)"
            >
              导入预览
            </AButton>
          </template>
          <template v-else-if="column.key === 'actions' && activeTab === 'area'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openRegionForm(record)">
                编辑
              </AButton>
              <APopconfirm :title="`确认删除大区“${record.regionName || '-'}”？`" @confirm="removeRegion(record)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
          <template v-else-if="column.key === 'actions' && activeTab === 'companyType'">
            <ASpace>
              <AButton type="link" size="small" class="table-action-link--primary" @click="openCompanyTypeForm(record)">
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
              <APopconfirm :title="`确认删除类型“${record.typeName || '-'}”？`" @confirm="removeCompanyType(record.id)">
                <AButton type="link" size="small" danger>删除</AButton>
              </APopconfirm>
            </ASpace>
          </template>
        </template>
      </ATable>
    </ACard>

    <ADrawer v-model:open="hqCrmImportOpen" title="从CRM导入签约" :width="1180">
      <AForm :model="crmHqQuery" layout="inline" class="page-search-toolbar--inline mb-12px">
        <AFormItem label="总部公司">
          <ASelect
            v-model:value="crmHqQuery.hqCompanyId"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="请选择总部公司"
            class="min-w-180px"
            :options="hqCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
            :options="firstCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
              crmImportRegionOptions.map(r => ({
                label: `${r.regionName || '-'}${r.regionCode ? `（${r.regionCode}）` : ''}`,
                value: r.id
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
          <ACheckbox v-model:checked="crmHqQuery.showAbnormal">查看异常数据</ACheckbox>
        </AFormItem>
        <AFormItem>
          <AButton type="primary" :loading="loading" @click="handleCrmHqSearch">搜索</AButton>
          <AButton class="ml-8px" :loading="loading" @click="resetHqCrmQuery">重置</AButton>
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
          getCheckboxProps: (record: RowData) => ({ disabled: !record.canImport })
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
          }
        }"
        :scroll="{ x: 1480 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'hqCrmRegion'">
            <span>{{ record.regionName || '-' }}</span>
            <span v-if="record.regionCode" class="text-gray-500">（{{ record.regionCode }}）</span>
          </template>
          <template v-else-if="column.key === 'hqCrmAlive'">
            <ATag :color="tagColorPositiveNeutral(record.aliveFlag === 1)">
              {{ record.aliveFlag === 1 ? '有效' : '失效' }}
            </ATag>
          </template>
          <template v-else-if="column.key === 'hqCrmImportSts'">
            <ATag v-if="record.canImport" color="success">可导入</ATag>
            <ATag v-else-if="record.existingContract" color="processing">已存在</ATag>
            <ATag v-else color="warning">异常</ATag>
          </template>
        </template>
      </ATable>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="hqCrmImportOpen = false">取消</AButton>
          <AButton type="primary" :loading="loading" @click="triggerCrmHqImport">确定</AButton>
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
            :options="hqCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
            :options="firstCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
            :options="hqFormRegionOptions.map(r => ({ label: r.regionName, value: r.id }))"
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
            :options="firstCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
          />
        </AFormItem>
        <AFormItem label="二级网点" required>
          <ASelect
            v-model:value="fsForm.secondCompanyId"
            show-search
            option-filter-prop="label"
            placeholder="请选择"
            class="w-full"
            :options="secondCompanyOptions.map(c => ({ label: c.companyName, value: c.id }))"
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
              <AInput v-model:value="companyForm.companyName" placeholder="请输入公司名称" />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="公司简称">
              <AInput v-model:value="companyForm.companyShortName" placeholder="请输入公司简称" />
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
                  companyTypeOptions.map(item => ({
                    label: `${item.typeName} (${item.typeCode})`,
                    value: item.typeCode
                  }))
                "
              />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="联系人" required>
              <AInput v-model:value="companyForm.contactName" placeholder="请输入联系人" />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="联系电话" required>
              <AInput v-model:value="companyForm.contactPhone" placeholder="请输入联系电话" />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem label="客服电话">
              <AInput v-model:value="companyForm.servicePhone" placeholder="请输入客服电话" />
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
                  :options="provinceOptions.map(item => ({ label: item.areaName, value: item.areaCode }))"
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
                  :options="cityOptions.map(item => ({ label: item.areaName, value: item.areaCode }))"
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
                  :options="districtOptions.map(item => ({ label: item.areaName, value: item.areaCode }))"
                />
              </AFormItem>
            </ACol>
          </ARow>
          <AFormItem label="详细地址" required class="company-address-block__detail">
            <AInput v-model:value="companyForm.detailAddress" placeholder="请输入详细地址" />
          </AFormItem>
        </div>
        <ARow :gutter="[16, 0]">
          <ACol :span="24" :md="12">
            <AFormItem v-if="!companyForm.id" label="管理员用户名">
              <AInput v-model:value="companyForm.adminUsername" placeholder="新增公司时必填，用于创建默认管理员账号" />
            </AFormItem>
          </ACol>
          <ACol :span="24" :md="12">
            <AFormItem v-if="isCompanyHqType(companyForm.typeCode)" label="销售组织">
              <AInput v-model:value="companyForm.salesOrg" placeholder="请输入销售组织" />
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
          <AInput v-model:value="companyTypeForm.typeName" placeholder="如 总部A" />
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
              { label: '网点', value: 'SERVICE' }
            ]"
          />
        </AFormItem>
        <AFormItem label="排序">
          <AInputNumber v-model:value="companyTypeForm.orderNum" :min="0" class="w-full" />
        </AFormItem>
        <AFormItem label="备注">
          <ATextarea v-model:value="companyTypeForm.remark" :rows="2" placeholder="请输入备注" />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="companyTypeFormOpen = false">取消</AButton>
          <AButton type="primary" @click="submitCompanyTypeForm">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="companyTypeMenuOpen" :title="companyTypeMenuTitle" :width="680">
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
          <AButton type="primary" :loading="companyTypeMenuSubmitting" @click="submitCompanyTypeMenuAssign">
            确定
          </AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="regionFormOpen" :title="regionForm.id ? '编辑大区' : '新增大区'" :width="460">
      <AForm layout="vertical" class="mt-12px">
        <AFormItem label="大区编码">
          <AInput v-model:value="regionForm.regionCode" placeholder="如：HD" :maxlength="32" />
        </AFormItem>
        <AFormItem label="大区名称" required>
          <AInput v-model:value="regionForm.regionName" placeholder="如：华东大区" />
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

    <ADrawer v-model:open="previewOpen" title="外部公司 CRM 导入预览" :width="720">
      <div class="mb-8px">
        <AInput v-model:value="previewCustId" placeholder="custId / 主键" class="max-w-240px" />
        <AButton class="ml-8px" type="primary" :loading="previewLoading" @click="loadPreview">加载</AButton>
      </div>
      <pre class="max-h-400px overflow-auto rounded-4px bg-gray-50 p-12px text-12px dark:bg-dark-800">{{
        JSON.stringify(previewData, null, 2)
      }}</pre>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="previewOpen = false">关闭</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="companyCrmImportOpen" title="选择 CRM 公司" :width="1100">
      <AForm layout="inline" class="page-search-toolbar--inline mb-12px">
        <AFormItem label="SAP 公司编码">
          <AInput v-model:value="companyCrmImportQuery.companyCode" allow-clear placeholder="请输入 SAP 公司编码" />
        </AFormItem>
        <AFormItem label="公司名称">
          <AInput v-model:value="companyCrmImportQuery.companyName" allow-clear placeholder="请输入公司名称" />
        </AFormItem>
        <AFormItem label="CRM 状态">
          <ASelect
            v-model:value="companyCrmImportQuery.custState"
            allow-clear
            placeholder="全部"
            class="min-w-160px"
            :options="externalStateOptions.map(item => ({ label: item.label, value: item.value }))"
          />
        </AFormItem>
        <AFormItem>
          <AButton type="primary" @click="handleCompanyCrmImportSearch">搜索</AButton>
          <AButton class="ml-8px" @click="resetCompanyCrmImportSearch">重置</AButton>
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
          }
        }"
        :columns="[
          { title: 'SAP 公司编码', dataIndex: 'companyCode', key: 'companyCode', width: 140 },
          { title: '公司名称', dataIndex: 'companyName', key: 'companyName', width: 180 },
          { title: '公司简称', dataIndex: 'companyShortName', key: 'companyShortName', width: 140 },
          { title: '建议类型', dataIndex: 'typeCode', key: 'typeCode', width: 120 },
          { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100 },
          { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 130 },
          { title: '地址', dataIndex: 'address', key: 'address', ellipsis: true },
          { title: '操作', key: 'actions', width: 90, fixed: 'right' }
        ]"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'typeCode'">
            {{ record.typeCode || '-' }}
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
