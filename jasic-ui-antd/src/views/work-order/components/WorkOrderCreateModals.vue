<script setup lang="ts">
/* eslint-disable vue/component-name-in-template-casing -- Ant Design Vue Input.TextArea */
/**
 * 工单创建弹窗：分步表单、客户与设备信息、地址级联、附件上传；支持代填/上游报修等建单入口。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Cascader as ACascader, Modal, Upload } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import type { CheckboxChangeEvent } from 'ant-design-vue/es/checkbox/interface';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import type { UploadRequestOption } from 'ant-design-vue/es/vc-upload/interface';
import {
  type CompanyAddressVO,
  createCompanyAddress,
  deleteCompanyAddress,
  listCompanyAddress,
  setDefaultCompanyAddress,
  updateCompanyAddress
} from '@/service/api/company-address';
import { uploadSystemFile } from '@/service/api/file';
import type { WorkOrderProxyCreateDTO } from '@/service/api';
import {
  createProxyWorkOrder,
  createUpstreamFirstWorkOrder,
  createUpstreamHqWorkOrder,
  getProxyCreateBarcodeInfo,
  getUpstreamFirstCreateBarcodeInfo,
  getUpstreamHqCreateBarcodeInfo,
  listUpstreamFirstCreateTargetOptions
} from '@/service/api';
import {
  getFlatErrorMsg,
  getFlatResponseMsg,
  isFlatRequestFailed,
  notifyOnceSuccessFromFlatResult
} from '@/service/request/shared';
import { useAuthStore } from '@/store/modules/auth';
import { adaptiveModalWidth } from '@/hooks/common/modal-form-layout';
import { createAntTableActionColumn } from '@/utils/table-action-width';
import {
  type RegionCascaderOption,
  composeAddressWithRegion,
  fetchRegionCascaderOptions,
  isFullRegionSelection,
  loadRegionCascaderData,
  splitFullAddressToRegionAndDetail
} from '@/utils/china-region';
import {
  CREATE_ENTRY_PROXY,
  CREATE_ENTRY_UPSTREAM_FIRST,
  CREATE_ENTRY_UPSTREAM_HQ,
  type CreateEntryMode,
  getCreateEntryOptions
} from '../create-entry';
import {
  type CreateFaultMediaItem,
  type CreateFileItem,
  type CreateWorkOrderFormState,
  DEFAULT_OTHER_FAULT_LABEL,
  SERVICE_MODE_MAIL,
  SERVICE_MODE_OPTIONS,
  buildDefaultCreateForm,
  buildFileIdList,
  cloneFaultMediaItems,
  cloneFileItems,
  normalizeTargetCompanyOption,
  normalizeText
} from '../create-work-order-form';

/** 地址簿子表操作列：设为默认 / 编辑 / 删除 横排估算（选择与管理模式一致） */

const emit = defineEmits<{
  (e: 'created'): void;
}>();

// 登录用户信息（用于建单入口选项）
const authStore = useAuthStore();
// 建单抽屉是否打开
const createDrawerOpen = ref(false);
// 建单表单状态
const createForm = reactive<CreateWorkOrderFormState>(buildDefaultCreateForm());
// Ant Design 表单实例
const createFormRef = ref<FormInstance | null>(null);
// 建单提交中
const createSubmitting = ref(false);
// 条码查询加载中
const createBarcodeLoading = ref(false);
// 补充信息区块折叠状态
const createSupplementExpanded = ref(false);

/** 二级「报修一级」无条码：target-options 无可选一级时的默认提示 */
const DEFAULT_UPSTREAM_FIRST_TARGET_MSG = '当前网点未配置目标一级，无法填写报修一级，请联系管理员处理。';

/** 无可选目标一级时的弹窗文案（接口失败时优先使用后端 msg） */
const upstreamFirstTargetBlockMessage = ref(DEFAULT_UPSTREAM_FIRST_TARGET_MSG);

/** 二级报修一级无条码：目标一级候选为空时禁止填写整单表单 */
const upstreamFirstTargetBlocked = ref(false);

// 地址表单省市区级联选项
const companyAddressRegionOptions = ref<RegionCascaderOption[]>([]);

// 公司地址列表加载中
const companyAddressLoading = ref(false);
// 公司地址列表数据
const companyAddressList = ref<CompanyAddressVO[]>([]);
// 地址簿弹窗是否显示
const companyAddressDialogVisible = ref(false);
// 地址簿模式：选择寄件 / 管理
const companyAddressDialogMode = ref<'select' | 'manage'>('manage');
// 地址编辑子弹窗是否显示
const companyAddressFormVisible = ref(false);
// 地址簿编辑表单
const companyAddressFormRef = ref<FormInstance | null>(null);
const companyAddressFormTitle = ref('新增地址');
// 地址表单提交中
const companyAddressSubmitting = ref(false);
// 地址簿内编辑表单模型
const companyAddressForm = reactive({
  id: undefined as number | undefined,
  contactName: '',
  contactPhone: '',
  addressDetail: '',
  regionCodes: [] as string[],
  isDefault: 0 as 0 | 1
});

const companyAddressFormRules = computed(() => ({
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  regionCodes: [
    { required: true, message: '请选择完整的省、市、区', trigger: 'change' },
    {
      validator: async () => {
        if (!isFullRegionSelection(companyAddressForm.regionCodes)) {
          return Promise.reject(new Error('请选择完整的省、市、区'));
        }
        return Promise.resolve();
      },
      trigger: 'change'
    }
  ],
  addressDetail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}));

// 当前用户可用的建单入口选项
const createEntryOptions = computed(() => getCreateEntryOptions(authStore.userInfo.currentTypeCode));

// 是否为上游报修建单（非代填）
const isCreateUpstreamEntry = computed(() => createForm.entryMode !== CREATE_ENTRY_PROXY);
// 是否展示客户姓名/手机等代填字段
const showCreateCustomerFields = computed(() => !isCreateUpstreamEntry.value);
// 是否邮寄维修路径
const isCreateMailMode = computed(() => createForm.serviceMode === SERVICE_MODE_MAIL);
// 条码是否已成功解析出商品信息
const hasCreateResolvedBarcodeInfo = computed(() => Boolean(createForm.barcodeResolved));
// 上游建单且有待选受理网点时展示目标公司选择
const showCreateTargetCompany = computed(
  () => isCreateUpstreamEntry.value && (createForm.targetCompanyOptions || []).length > 0
);
// 目标公司字段标签（一级 / 总部）
const createTargetCompanyLabel = computed(() =>
  createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST ? '目标一级' : '目标总部'
);
// 目标公司下拉占位符（与小程序一致）
const createTargetCompanySelectPlaceholder = computed(() => `请选择${createTargetCompanyLabel.value}`);
// 仅有一个可选网点时自动带出
const isCreateTargetAutoFilled = computed(() => (createForm.targetCompanyOptions || []).length <= 1);

/** 是否处于「报修一级」且无条码场景（依赖 target-options 拉取默认一级） */
const isCreateUpstreamFirstNoBarcodeContext = computed(
  () => createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && !normalizeText(createForm.barcode)
);

/** 报修一级无条码且 target-options 无数据：整表禁填 */
const isCreateUpstreamFirstFormBlocked = computed(
  () => isCreateUpstreamFirstNoBarcodeContext.value && upstreamFirstTargetBlocked.value
);

/**
 * 作用：判断故障选项是否属于「其它故障」类（与小程序 isOtherFaultSelection 语义对齐）。
 * @param value - 选项值或文案
 * @returns 是否为其它故障项
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isCreateOtherFaultItem(value: string): boolean {
  const v = normalizeText(value);
  if (!v) return false;
  if (/^(other|others)$/i.test(v)) return true;
  const label = normalizeText(createForm.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL);
  if (label && v === label) return true;
  if (v === '其它' || v === '其他') return true;
  if (/(其它|其他)/.test(v) && /故障/.test(v)) return true;
  return false;
}

// 条码查询成功且存在故障选项时需展示故障多选（与 contractor jasicRepair 一致）
const createBarcodeQueryHasFaultDescription = computed(
  () =>
    createForm.barcodeQueried &&
    !createForm.barcodeQueryFailed &&
    (createForm.faultOptions || []).some(o => Boolean(normalizeText(o)))
);
// 当前生效的故障选项列表（无描述场景为空）
const effectiveCreateFaultOptions = computed(() => {
  if (!createBarcodeQueryHasFaultDescription.value) return [];
  return createForm.faultOptions || [];
});
// 已填条码但未完成查询时禁用故障选择
const isCreateFaultSelectDisabled = computed(
  () => Boolean(normalizeText(createForm.barcode)) && !createForm.barcodeQueried
);
// 是否展示故障备注输入（与小程序 syncShowFaultRemarkFromState 一致）
const showCreateFaultRemark = computed(() => {
  const code = normalizeText(createForm.barcode);
  if (!code) return true;
  if (createBarcodeQueryHasFaultDescription.value) {
    return (createForm.faultItems || []).some(item => isCreateOtherFaultItem(item));
  }
  if (hasCreateResolvedBarcodeInfo.value || createForm.barcodeQueryFailed) return true;
  return false;
});
// 根据字段多少动态设置抽屉基准宽度；多于 6 个可见表单项（≥7）时宽度与 720 取大（见 adaptiveModalWidth）
const createDrawerBaseWidth = computed(() => {
  if (hasCreateResolvedBarcodeInfo.value || isCreateMailMode.value || showCreateTargetCompany.value) {
    return 760;
  }
  return 560;
});

const createFormVisibleFieldCount = computed(() => {
  let n = 1; // 维修路径
  if (showCreateCustomerFields.value) n += 1;
  if (showCreateTargetCompany.value) n += 1;
  if (createBarcodeQueryHasFaultDescription.value) n += 1;
  if (showCreateFaultRemark.value) n += 1;
  if (isCreateMailMode.value) n += 1;
  if (createSupplementExpanded.value) {
    n += 2; // 故障媒体、语音
    if (isCreateMailMode.value) n += 1; // 寄件快递单号凭证
  }
  return n;
});

const createDrawerWidth = computed(() =>
  adaptiveModalWidth(createDrawerBaseWidth.value, createFormVisibleFieldCount.value)
);
// 故障选择框占位提示文案
const createFaultPlaceholder = computed(() => {
  if (isCreateFaultSelectDisabled.value) return '请先完成商品查询';
  if (!createBarcodeQueryHasFaultDescription.value) return '当前查询结果无故障选项，请填写下方故障说明备注';
  return '请选择';
});
// 邮寄场景寄件信息摘要（姓名/手机/地址）
const createShippingAddressSummary = computed(() => {
  const list = [
    normalizeText(createForm.senderName),
    normalizeText(createForm.senderMobile),
    normalizeText(createForm.senderAddress)
  ].filter(Boolean);
  if (!list.length) return '';
  if (list.length === 3) return `${list[0]} / ${list[1]}\n${list[2]}`;
  return list.join('\n');
});
// 地址簿弹窗标题（选择 / 管理）
const companyAddressDialogTitle = computed(() =>
  companyAddressDialogMode.value === 'select' ? '选择寄件信息' : '公司地址簿'
);

// 地址簿表格列（选择模式下多一列「选用」）
const companyAddressColumns = computed(() => {
  const rest = [
    { title: '默认', key: 'isDefault', width: 88, align: 'center' as const },
    { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
    { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
    { title: '详细地址', dataIndex: 'address', key: 'address', ellipsis: true },
    createAntTableActionColumn({ width: 200 })
  ];
  if (companyAddressDialogMode.value === 'select') {
    return [{ title: '', key: 'pick', width: 48, align: 'center' as const }, ...rest];
  }
  return rest;
});

// 建单表单校验规则（与 jasic-ui createRules 一致）
const createFormRules = computed(() => ({
  customerMobile:
    createForm.entryMode === CREATE_ENTRY_PROXY
      ? [
          { required: true, message: '请输入客户手机号码', trigger: 'blur' },
          {
            validator: async (_rule: unknown, value: string) => {
              const m = normalizeText(value);
              if (m && !/^1[3-9]\d{9}$/.test(m)) {
                return Promise.reject(new Error('请输入正确的手机号码'));
              }
              return Promise.resolve();
            },
            trigger: 'blur'
          }
        ]
      : [],
  serviceMode: [{ required: true, message: '请选择维修路径', trigger: 'change' }]
}));

/**
 * 作用：解析初始建单入口，非法或不允许时回落到首个可选值。
 * @param entry - 期望的入口模式（可选）
 * @returns 实际使用的入口模式
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveInitialEntry(entry?: CreateEntryMode): CreateEntryMode {
  const opts = createEntryOptions.value;
  const allowed = new Set(opts.map(o => o.value));
  if (entry && allowed.has(entry)) return entry;
  return opts[0]!.value;
}

/**
 * 作用：用新状态整体替换建单表单（浅合并字段）。
 * @param next - 新的表单状态对象
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function assignCreateForm(next: CreateWorkOrderFormState) {
  Object.assign(createForm, next);
}

/**
 * 作用：按指定入口重置为默认表单并写入 entryMode。
 * @param entry - 建单入口模式
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetCreateFormToEntry(entry: CreateEntryMode) {
  const d = buildDefaultCreateForm();
  d.entryMode = entry;
  assignCreateForm(d);
  upstreamFirstTargetBlocked.value = false;
  upstreamFirstTargetBlockMessage.value = DEFAULT_UPSTREAM_FIRST_TARGET_MSG;
}

/**
 * 作用：切换建单入口 Tab，保留寄件与附件等通用字段后重置业务字段。
 * @param entryMode - 目标入口模式
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleCreateEntryModeChange(entryMode: CreateEntryMode) {
  const nextForm = buildDefaultCreateForm();
  nextForm.entryMode = entryMode;
  nextForm.customerName = createForm.customerName;
  nextForm.customerMobile = createForm.customerMobile;
  nextForm.barcode = createForm.barcode;
  nextForm.serviceMode = createForm.serviceMode;
  nextForm.companyAddressId = createForm.companyAddressId;
  nextForm.senderName = createForm.senderName;
  nextForm.senderMobile = createForm.senderMobile;
  nextForm.senderAddress = createForm.senderAddress;
  nextForm.sendExpressNo = createForm.sendExpressNo;
  nextForm.senderVoucherFiles = cloneFileItems(createForm.senderVoucherFiles);
  nextForm.faultMediaFiles = cloneFaultMediaItems(createForm.faultMediaFiles);
  nextForm.faultVoiceFiles = cloneFileItems(createForm.faultVoiceFiles);
  assignCreateForm(nextForm);
  syncCreateEntryDefaults(entryMode === CREATE_ENTRY_UPSTREAM_FIRST);
}

/**
 * 作用：入口 Segmented 切换回调。
 * @param key - 选中的入口 key
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function onTabsChange(key: string | number) {
  handleCreateEntryModeChange(String(key) as CreateEntryMode);
}

/**
 * 作用：切换补充信息折叠面板展开状态。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function toggleCreateSupplementSection() {
  createSupplementExpanded.value = !createSupplementExpanded.value;
}

/**
 * 作用：弹出「无可选目标一级」提示（Modal 单条展示接口 msg，避免与全局 toast 重复）。
 * @param message - 可选覆盖文案
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-27
 */
function presentUpstreamFirstTargetBlockedModal(message?: string) {
  const content = String(message ?? upstreamFirstTargetBlockMessage.value).trim() || DEFAULT_UPSTREAM_FIRST_TARGET_MSG;
  Modal.info({
    title: '报修提示',
    content,
    okText: '我知道了',
    maskClosable: false
  });
}

/**
 * 作用：根据 target-options 结果更新「报修一级无条码」禁填态。
 * @param showModalWhenBlocked - 禁填时是否弹出提示
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-27
 */
function refreshUpstreamFirstTargetBlockState(showModalWhenBlocked = false) {
  if (!isCreateUpstreamFirstNoBarcodeContext.value) {
    upstreamFirstTargetBlocked.value = false;
    return;
  }
  const blocked = (createForm.targetCompanyOptions || []).length === 0;
  upstreamFirstTargetBlocked.value = blocked;
  if (blocked && showModalWhenBlocked) {
    presentUpstreamFirstTargetBlockedModal();
  }
}

/**
 * 作用：二级网点「报修一级」且无条码时拉取默认一级受理网点。
 * @param showModalWhenBlocked - 拉取结束且无可选一级时是否弹窗提示
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-27
 */
async function syncCreateEntryDefaults(showModalWhenBlocked = false) {
  if (createForm.entryMode !== CREATE_ENTRY_UPSTREAM_FIRST || normalizeText(createForm.barcode)) {
    refreshUpstreamFirstTargetBlockState(false);
    return;
  }
  const res = await listUpstreamFirstCreateTargetOptions({ skipErrorToast: true });
  if (isFlatRequestFailed(res)) {
    createForm.targetCompanyId = undefined;
    createForm.targetCompanyName = '';
    createForm.targetCompanyOptions = [];
    upstreamFirstTargetBlockMessage.value = getFlatErrorMsg(res, DEFAULT_UPSTREAM_FIRST_TARGET_MSG);
    refreshUpstreamFirstTargetBlockState(showModalWhenBlocked);
    return;
  }
  const data = res.data;
  const listRaw = Array.isArray(data) ? data : (data as { records?: unknown[] })?.records || [];
  const list = listRaw.map((r: Record<string, unknown>) => normalizeTargetCompanyOption(r)).filter(Boolean) as Array<{
    id: number;
    companyName: string;
  }>;
  createForm.targetCompanyOptions = list;
  if (list.length === 1) {
    const target = list[0]!;
    createForm.targetCompanyId = target.id;
    createForm.targetCompanyName = target.companyName || '';
    upstreamFirstTargetBlockMessage.value = DEFAULT_UPSTREAM_FIRST_TARGET_MSG;
  } else if (list.length > 1) {
    createForm.targetCompanyId = undefined;
    createForm.targetCompanyName = '';
    upstreamFirstTargetBlockMessage.value = DEFAULT_UPSTREAM_FIRST_TARGET_MSG;
  } else {
    createForm.targetCompanyId = undefined;
    createForm.targetCompanyName = '';
    upstreamFirstTargetBlockMessage.value = getFlatResponseMsg(res, DEFAULT_UPSTREAM_FIRST_TARGET_MSG);
  }
  refreshUpstreamFirstTargetBlockState(showModalWhenBlocked);
}

/**
 * 作用：清空条码解析产物；可选保留已选目标网点。
 * @param options.preserveTargetSelection - 是否保留 targetCompany 相关字段
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resetCreateQueryState(options: { preserveTargetSelection?: boolean } = {}) {
  const preserveTargetSelection = Boolean(options.preserveTargetSelection);
  Object.assign(createForm, {
    queriedBarcode: '',
    barcodeQueried: false,
    barcodeResolved: false,
    barcodeQueryFailed: false,
    productCode: '',
    productName: '',
    productModel: '',
    machineNo: '',
    brandCode: '',
    warrantyStatus: '',
    hqCompanyId: undefined,
    hqCompanyName: '',
    targetCompanyId: preserveTargetSelection ? createForm.targetCompanyId : undefined,
    targetCompanyName: preserveTargetSelection ? createForm.targetCompanyName : '',
    targetCompanyOptions: preserveTargetSelection ? [...(createForm.targetCompanyOptions || [])] : [],
    faultOptions: [],
    otherFaultLabel: DEFAULT_OTHER_FAULT_LABEL,
    faultItems: [],
    faultRemark: ''
  });
}

/**
 * 作用：将条码查询接口返回写入表单（商品、网点、故障选项等）。
 * @param data - 接口 body
 * @param queriedBarcode - 当前查询的条码字符串
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
// eslint-disable-next-line complexity
function applyCreateBarcodeInfo(data: Record<string, unknown>, queriedBarcode: string) {
  const rawOpts = Array.isArray(data.targetCompanyOptions) ? data.targetCompanyOptions : [];
  const targetCompanyOptions = rawOpts
    .map((item: unknown) => normalizeTargetCompanyOption((item || {}) as Record<string, unknown>))
    .filter(Boolean) as Array<{ id: number; companyName: string }>;

  const currentTargetCompanyId = createForm.targetCompanyId;
  const matchedCurrentTarget = targetCompanyOptions.some(item => String(item.id) === String(currentTargetCompanyId));
  let targetCompanyId: number | undefined = matchedCurrentTarget ? currentTargetCompanyId : undefined;
  if (
    targetCompanyId === undefined &&
    data.defaultTargetCompanyId !== null &&
    data.defaultTargetCompanyId !== undefined
  ) {
    targetCompanyId = Number(data.defaultTargetCompanyId);
  }
  if (targetCompanyId === undefined && targetCompanyOptions.length === 1) {
    targetCompanyId = targetCompanyOptions[0]!.id;
  }
  const targetCompany = targetCompanyOptions.find(item => String(item.id) === String(targetCompanyId)) || null;

  Object.assign(createForm, {
    barcode: String(data.barcode || queriedBarcode),
    queriedBarcode: String(data.barcode || queriedBarcode),
    barcodeQueried: true,
    barcodeResolved: true,
    barcodeQueryFailed: false,
    productCode: String(data.productCode || ''),
    productName: String(data.productName || ''),
    productModel: String(data.productModel || ''),
    machineNo: String(data.machineNo || ''),
    brandCode: String(data.brandCode || ''),
    warrantyStatus: String(data.warrantyStatus || ''),
    hqCompanyId: data.hqCompanyId !== undefined ? Number(data.hqCompanyId) : undefined,
    hqCompanyName: String(data.hqCompanyName || ''),
    targetCompanyId,
    targetCompanyName: targetCompany ? targetCompany.companyName || '' : '',
    targetCompanyOptions,
    faultOptions: Array.isArray(data.faultOptions) ? (data.faultOptions as string[]) : [],
    otherFaultLabel: String(data.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL),
    faultItems: [],
    faultRemark: ''
  });

  // 报修一级：接口必须返回唯一一级网点，否则禁止提交
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && targetCompanyOptions.length !== 1) {
    createForm.targetCompanyId = undefined;
    createForm.targetCompanyName = '';
    window.$message?.error('当前二级网点未带出唯一一级网点，请联系管理员排查');
  }
}

/**
 * 作用：按当前建单入口解析对应的条码查询请求。
 * @param barcode - 条码
 * @returns 条码查询 Promise 或 null（入口不支持时）
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveCreateBarcodeInfoRequest(barcode: string) {
  if (createForm.entryMode === CREATE_ENTRY_PROXY) {
    return getProxyCreateBarcodeInfo({ barcode });
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST) {
    return getUpstreamFirstCreateBarcodeInfo({ barcode });
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_HQ) {
    const params: Record<string, unknown> = { barcode };
    if (createForm.targetCompanyId) params.targetCompanyId = createForm.targetCompanyId;
    return getUpstreamHqCreateBarcodeInfo(params);
  }
  return null;
}

/**
 * 作用：请求条码信息并回填表单；失败时标记 barcodeQueryFailed。
 * @param options.preserveTargetSelection - 是否保留目标网点
 * @param options.silentSuccess - 成功时不弹 success 提示
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function queryCreateBarcodeInfo(options: { preserveTargetSelection?: boolean; silentSuccess?: boolean } = {}) {
  const barcode = normalizeText(createForm.barcode);
  if (!barcode) {
    window.$message?.warning('请输入条形码');
    return;
  }
  // 代填 / 报修一级 / 报修佳士 三套条码查询接口，由入口模式决定
  const req = resolveCreateBarcodeInfoRequest(barcode);
  if (!req) {
    window.$message?.error('当前建单入口不支持查条码');
    return;
  }
  createBarcodeLoading.value = true;
  // 重新查询前清空上次解析结果，避免商品/故障选项与当前条码不一致
  resetCreateQueryState({ preserveTargetSelection: Boolean(options.preserveTargetSelection) });
  try {
    const res = await req;
    if (isFlatRequestFailed(res)) {
      return;
    }
    const body = (res?.data || {}) as Record<string, unknown>;
    applyCreateBarcodeInfo(body, barcode);
    if (!options.silentSuccess) {
      notifyOnceSuccessFromFlatResult(res, '查询成功');
    }
  } catch {
    // 查询失败仍标记已查询，允许无码/过保路径提交；故障改走备注输入
    Object.assign(createForm, {
      queriedBarcode: barcode,
      barcodeQueried: true,
      barcodeResolved: false,
      barcodeQueryFailed: true,
      productCode: '',
      productName: '',
      productModel: '',
      machineNo: '',
      brandCode: '',
      warrantyStatus: '',
      hqCompanyId: undefined,
      hqCompanyName: '',
      faultOptions: [],
      otherFaultLabel: DEFAULT_OTHER_FAULT_LABEL,
      faultItems: [],
      faultRemark: ''
    });
    await syncCreateEntryDefaults(false);
  } finally {
    createBarcodeLoading.value = false;
  }
}

/**
 * 作用：目标网点变更后若已查过条码则静默重新查询。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleCreateTargetCompanyChange() {
  if (!createForm.barcodeQueried) return;
  // 报修佳士切换目标总部后需按新网点重查条码（受理方、故障选项可能变化）
  queryCreateBarcodeInfo({ preserveTargetSelection: true, silentSuccess: true });
}

/**
 * 作用：加载公司地址簿列表并同步当前选中寄件地址。
 * @param options.preserveSelection - 是否尽量保留当前 addressId 选中项
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function loadCompanyAddressList(options: { preserveSelection?: boolean } = {}) {
  companyAddressLoading.value = true;
  try {
    const { data } = await listCompanyAddress();
    companyAddressList.value = Array.isArray(data) ? data : [];
    syncCreateCompanyAddressSelection(options);
  } finally {
    companyAddressLoading.value = false;
  }
}

/**
 * 作用：将选中的公司地址写入建单寄件字段。
 * @param address - 地址 VO 或 null 表示清空
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function applySelectedCompanyAddress(address: CompanyAddressVO | null) {
  createForm.companyAddressId = address ? address.id : undefined;
  createForm.senderName = address ? address.contactName || '' : '';
  createForm.senderMobile = address ? address.contactPhone || '' : '';
  createForm.senderAddress = address ? address.address || '' : '';
}

/**
 * 作用：列表刷新后同步默认选中地址（默认项或第一项）。
 * @param options.preserveSelection - 是否优先保留当前 ID
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function syncCreateCompanyAddressSelection(options: { preserveSelection?: boolean } = {}) {
  const preserveSelection = Boolean(options.preserveSelection);
  const currentAddressId = createForm.companyAddressId;
  let selected: CompanyAddressVO | null = null;
  if (preserveSelection && currentAddressId !== null && currentAddressId !== undefined) {
    selected = companyAddressList.value.find(item => String(item.id) === String(currentAddressId)) || null;
  }
  if (!selected) {
    selected = companyAddressList.value.find(item => item.isDefault === 1) || companyAddressList.value[0] || null;
  }
  applySelectedCompanyAddress(selected);
}

/**
 * 作用：在地址簿中选择一行并关闭弹窗。
 * @param row - 地址行
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleSelectCompanyAddress(row: CompanyAddressVO) {
  applySelectedCompanyAddress(row);
  companyAddressDialogVisible.value = false;
}

/**
 * 作用：表格内勾选「选用」时选中该地址。
 * @param e - Checkbox 变更事件
 * @param row - 地址行
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function onCompanyAddressCheckboxChange(e: CheckboxChangeEvent, row: CompanyAddressVO) {
  if (e.target.checked) handleSelectCompanyAddress(row);
}

/**
 * 作用：打开公司地址簿弹窗并加载列表。
 * @param mode - 选择寄件信息或管理模式
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function openCompanyAddressDialog(mode: 'select' | 'manage' = 'manage') {
  companyAddressDialogMode.value = mode;
  companyAddressDialogVisible.value = true;
  loadCompanyAddressList({ preserveSelection: true });
}

/**
 * 作用：打开新增/编辑地址子弹窗并回填表单。
 * @param address - 编辑时传入；新增不传
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function openCompanyAddressForm(address?: CompanyAddressVO | null) {
  if (address) {
    companyAddressFormTitle.value = '编辑地址';
    companyAddressForm.id = address.id;
    companyAddressForm.contactName = address.contactName || '';
    companyAddressForm.contactPhone = address.contactPhone || '';
    // 将库内完整地址拆成省市区级联 + 详细地址，提交时再由 composeAddressWithRegion 拼回
    const parsed = await splitFullAddressToRegionAndDetail(address.address || '');
    companyAddressForm.regionCodes = [...parsed.regionCodes];
    companyAddressForm.addressDetail = parsed.addressDetail;
    if (normalizeText(address.address) && !parsed.regionCodes.length) {
      window.$message?.warning('未能从当前地址识别省市区，请手动选择省、市、区');
    }
    companyAddressForm.isDefault = address.isDefault === 1 ? 1 : 0;
  } else {
    companyAddressFormTitle.value = '新增地址';
    companyAddressForm.id = undefined;
    companyAddressForm.contactName = '';
    companyAddressForm.contactPhone = '';
    companyAddressForm.addressDetail = '';
    companyAddressForm.regionCodes = [];
    companyAddressForm.isDefault = 0;
  }
  companyAddressFormVisible.value = true;
}

/**
 * 作用：校验并提交地址簿表单（新增或更新）；成功后关闭子弹层并刷新地址列表。
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitCompanyAddress() {
  try {
    await companyAddressFormRef.value?.validate();
  } catch {
    return;
  }

  const isDef = (companyAddressForm.isDefault === 1 ? 1 : 0) as 0 | 1;
  const contactName = normalizeText(companyAddressForm.contactName);
  const contactPhone = normalizeText(companyAddressForm.contactPhone);
  const detail = normalizeText(companyAddressForm.addressDetail);

  // 省市区 + 详细地址合并为一条 address 字段入库（与小程序地址簿一致）
  const address = await composeAddressWithRegion(companyAddressForm.regionCodes, detail);

  const payload = {
    contactName,
    contactPhone,
    address,
    isDefault: isDef
  };
  companyAddressSubmitting.value = true;
  try {
    const res = companyAddressForm.id
      ? await updateCompanyAddress({ id: companyAddressForm.id, ...payload })
      : await createCompanyAddress(payload);
    if (!notifyOnceSuccessFromFlatResult(res, '已保存')) return;
    companyAddressFormVisible.value = false;
    // 刷新后 preserveSelection：邮寄建单已选地址尽量保持，新增默认项时 sync 会切到默认行
    await loadCompanyAddressList({ preserveSelection: true });
  } finally {
    companyAddressSubmitting.value = false;
  }
}

/**
 * 作用：删除地址簿中的一条记录。
 * @param row - 地址行
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleDeleteCompanyAddress(row: CompanyAddressVO) {
  if (!row?.id) return;
  Modal.confirm({
    title: '提示',
    content: '确认删除该地址吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteCompanyAddress(row.id);
      if (!notifyOnceSuccessFromFlatResult(res, '已删除')) return;
      await loadCompanyAddressList({ preserveSelection: true });
    }
  });
}

/**
 * 作用：将地址设为默认并刷新列表。
 * @param row - 地址行
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function handleSetDefaultCompanyAddress(row: CompanyAddressVO) {
  if (!row?.id || row.isDefault === 1) return;
  const res = await setDefaultCompanyAddress(row.id);
  if (!notifyOnceSuccessFromFlatResult(res, '已设为默认')) return;
  await loadCompanyAddressList({ preserveSelection: true });
}

/**
 * 作用：校验邮寄模式下寄件四要素是否齐全。
 * @returns 是否通过
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function validateSendInfo(): boolean {
  if (!createForm.companyAddressId) {
    window.$message?.error('请选择寄件信息');
    return false;
  }
  if (!createForm.senderName) {
    window.$message?.error('当前地址未配置寄件人');
    return false;
  }
  if (!createForm.senderMobile) {
    window.$message?.error('当前地址未配置寄件手机号');
    return false;
  }
  if (!createForm.senderAddress) {
    window.$message?.error('当前地址未配置寄件地址');
    return false;
  }
  return true;
}

/**
 * 作用：提交前业务校验（条码、网点、故障等）。
 * @returns 是否通过
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function validateCreateBeforeSubmit(): boolean {
  if (isCreateUpstreamFirstFormBlocked.value) {
    presentUpstreamFirstTargetBlockedModal();
    return false;
  }
  const barcode = normalizeText(createForm.barcode);
  if (barcode && (!createForm.barcodeQueried || createForm.queriedBarcode !== barcode)) {
    window.$message?.error('请先查询商品，再提交');
    return false;
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && !createForm.targetCompanyId) {
    presentUpstreamFirstTargetBlockedModal(upstreamFirstTargetBlockMessage.value || '请选择目标一级或联系管理员排查');
    return false;
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_HQ && !createForm.targetCompanyId) {
    window.$message?.error(`请选择${createTargetCompanyLabel.value}`);
    return false;
  }
  if (createBarcodeQueryHasFaultDescription.value && !(createForm.faultItems || []).length) {
    window.$message?.error('请填写故障描述');
    return false;
  }
  if (showCreateFaultRemark.value && !normalizeText(createForm.faultRemark)) {
    window.$message?.error('请填写故障说明备注');
    return false;
  }
  return true;
}

/**
 * 作用：组装提交用的故障项数组（无故障选项场景返回空）。
 * @returns 故障文案数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveCreateFaultItemsForSubmit(): string[] {
  if (createBarcodeQueryHasFaultDescription.value) {
    return (createForm.faultItems || []).map(item => normalizeText(item)).filter(Boolean);
  }
  return [];
}

/**
 * 作用：根据表单状态构造建单请求 body。
 * @returns 提交载荷对象
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function buildCreatePayload(): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    customerName: normalizeText(createForm.customerName),
    customerMobile: normalizeText(createForm.customerMobile),
    // 条码查询失败时按无码工单提交（与小程序、后端过保默认口径一致）
    barcode: createForm.barcodeQueryFailed ? '' : normalizeText(createForm.barcode),
    serviceMode: createForm.serviceMode,
    faultItems: resolveCreateFaultItemsForSubmit(),
    faultRemark: normalizeText(createForm.faultRemark) || '',
    faultImageFileIds: buildFileIdList(createForm.faultMediaFiles.filter(f => f.kind === 'image')),
    faultVideoFileIds: buildFileIdList(createForm.faultMediaFiles.filter(f => f.kind === 'video')),
    faultVoiceFileIds: buildFileIdList(createForm.faultVoiceFiles),
    senderName: '',
    senderMobile: '',
    senderAddress: '',
    sendExpressNo: '',
    senderVoucherFileIds: [] as number[]
  };
  // 仅邮寄维修写入寄件四要素与凭证 fileId
  if (isCreateMailMode.value) {
    payload.senderName = normalizeText(createForm.senderName);
    payload.senderMobile = normalizeText(createForm.senderMobile);
    payload.senderAddress = normalizeText(createForm.senderAddress);
    payload.sendExpressNo = normalizeText(createForm.sendExpressNo);
    payload.senderVoucherFileIds = buildFileIdList(createForm.senderVoucherFiles);
  }
  // 上游报修才传 targetCompanyId；代填由后端按当前主体解析
  if (createForm.entryMode !== CREATE_ENTRY_PROXY) {
    payload.targetCompanyId = createForm.targetCompanyId;
  }
  return payload;
}

/**
 * 作用：按入口模式选择对应创建工单接口。
 * @param payload - 请求体
 * @returns API 调用 Promise 或 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function resolveCreateRequest(payload: Record<string, unknown>) {
  if (createForm.entryMode === CREATE_ENTRY_PROXY) {
    return createProxyWorkOrder(payload as unknown as WorkOrderProxyCreateDTO);
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST) {
    return createUpstreamFirstWorkOrder(payload);
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_HQ) {
    return createUpstreamHqWorkOrder(payload);
  }
  return null;
}

/**
 * 作用：校验通过后调用创建接口；无条码时可二次确认。
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function submitCreate() {
  try {
    await createFormRef.value?.validate();
  } catch {
    return;
  }
  if (!validateCreateBeforeSubmit()) return;
  if (isCreateMailMode.value && !validateSendInfo()) return;

  const runSubmit = async () => {
    const payload = buildCreatePayload();
    const request = resolveCreateRequest(payload);
    if (!request) return;
    createSubmitting.value = true;
    try {
      const res = await request;
      if (!notifyOnceSuccessFromFlatResult(res, '创建成功')) return;
      createDrawerOpen.value = false;
      emit('created');
    } finally {
      createSubmitting.value = false;
    }
  };

  // 未填条码：二次确认后按无码工单提交（与查询失败路径一致）
  if (!normalizeText(createForm.barcode)) {
    Modal.confirm({
      title: '确认提交',
      content: '未填写产品条形码，将按无码工单直接提交，是否继续？',
      okText: '确定',
      cancelText: '取消',
      onOk: runSubmit
    });
    return;
  }
  await runSubmit();
}

/**
 * 作用：从上传接口响应解析为 CreateFileItem。
 * @param raw - 响应体
 * @returns 附件项或 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickUploadedFileItem(raw: unknown): CreateFileItem | null {
  if (!raw || typeof raw !== 'object') return null;
  const o = raw as Record<string, unknown>;
  const fileId = Number(o.fileId ?? o.id);
  if (!Number.isFinite(fileId)) return null;
  return {
    fileId,
    originalName: String(o.originalName ?? o.fileName ?? ''),
    fileSize: o.fileSize !== undefined ? Number(o.fileSize) : undefined
  };
}

/**
 * 作用：通用单文件上传并追加到指定列表。
 * @param opt - Upload 自定义请求参数
 * @param list - 目标附件列表
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function runFileUpload(opt: UploadRequestOption, list: CreateFileItem[]) {
  try {
    const raw = opt.file as File;
    const { data, error } = await uploadSystemFile(raw);
    if (error) {
      opt.onError?.(new Error('upload'));
      return;
    }
    const item = pickUploadedFileItem(data);
    if (item) list.push(item);
    opt.onSuccess?.(data as never, opt.file as never);
  } catch {
    opt.onError?.(new Error('upload'));
  }
}

// 字节换算基数（1MB）
const CREATE_UPLOAD_MB = 1024 * 1024;

/** 与 contractor `validateFaultMediaSelection` 一致：最多 1 个视频、3 张图片 */
// 故障图片最大数量
const FAULT_MEDIA_MAX_IMAGE = 3;
// 故障视频最大数量
const FAULT_MEDIA_MAX_VIDEO = 1;
// 故障媒体（图+视频）总数上限
const FAULT_MEDIA_MAX_TOTAL = FAULT_MEDIA_MAX_IMAGE + FAULT_MEDIA_MAX_VIDEO;
// 寄件凭证图片最大数量
const SENDER_VOUCHER_MAX_COUNT = 2;
// 故障语音最大条数
const FAULT_VOICE_MAX_COUNT = 6;
// 上传队列中待确认的故障媒体类型（按文件 key）
const pendingFaultMediaKinds = new Map<string, 'image' | 'video'>();
// 上传队列中待确认的寄件凭证文件 key
const pendingSenderVoucherKeys = new Set<string>();

/**
 * 作用：故障图片/视频上传请求处理。
 * @param opt - Upload 自定义请求参数
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function runFaultMediaUpload(opt: UploadRequestOption) {
  const raw = opt.file as File;
  const kind = inferFaultMediaKind(raw);
  if (!kind) {
    opt.onError?.(new Error('type'));
    return;
  }
  const uploadKey = getUploadFileKey(raw);
  if (!pendingFaultMediaKinds.has(uploadKey)) {
    opt.onError?.(new Error('blocked'));
    return;
  }
  try {
    const { data, error } = await uploadSystemFile(raw);
    if (error) {
      pendingFaultMediaKinds.delete(uploadKey);
      opt.onError?.(new Error('upload'));
      return;
    }
    const item = pickUploadedFileItem(data);
    if (item) {
      createForm.faultMediaFiles.push({ ...item, kind });
    }
    pendingFaultMediaKinds.delete(uploadKey);
    opt.onSuccess?.(data as never, opt.file as never);
  } catch {
    pendingFaultMediaKinds.delete(uploadKey);
    opt.onError?.(new Error('upload'));
  }
}

/**
 * 作用：按 fileId 从附件列表移除一项。
 * @param list - 附件列表
 * @param fileId - 文件 ID
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function removeFileItem(list: CreateFileItem[], fileId: number) {
  const i = list.findIndex(f => f.fileId === fileId);
  if (i >= 0) list.splice(i, 1);
}

/**
 * 作用：校验文件大小上限，超限则提示。
 * @param file - 文件对象
 * @param maxBytes - 最大字节
 * @param label - 提示中的类型名称
 * @returns 未超限时 true
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function rejectOversize(file: File, maxBytes: number, label: string): boolean {
  if (file.size > maxBytes) {
    window.$message?.error(`${label}单文件不能超过 ${Math.round(maxBytes / CREATE_UPLOAD_MB)}MB`);
    return false;
  }
  return true;
}

/**
 * 作用：为 Upload 组件生成稳定的文件标识 key（用于上传队列去重）。
 * @param file - 本地 File 或 UploadFile
 * @returns 稳定 key 字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function getUploadFileKey(file: File | UploadFile): string {
  const anyFile = file as unknown as Record<string, unknown>;
  const uid = normalizeText(anyFile.uid);
  if (uid) return uid;
  const name = normalizeText(anyFile.name);
  const size = Number(anyFile.size || 0);
  const lastModified = Number(anyFile.lastModified || 0);
  return `${name}-${size}-${lastModified}`;
}

/**
 * 作用：根据 MIME/扩展名推断故障媒体类型。
 * @param file - 本地文件
 * @returns image / video，无法识别时 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function inferFaultMediaKind(file: File): 'image' | 'video' | null {
  if (['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || /\.(jpe?g|png|webp)$/i.test(file.name)) {
    return 'image';
  }
  if (['video/mp4', 'video/quicktime'].includes(file.type) || /\.(mp4|mov)$/i.test(file.name)) {
    return 'video';
  }
  return null;
}

/**
 * 作用：故障媒体上传前校验类型与数量配额（1 视频 + 3 图）。
 * @param file - 本地文件
 * @returns 允许上传 true；拒绝时 Upload.LIST_IGNORE 或 false
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function beforeUploadFaultMedia(file: File) {
  const kind = inferFaultMediaKind(file);
  if (!kind) {
    window.$message?.error('仅支持上传图片或视频');
    return Upload.LIST_IGNORE;
  }
  const pendingKinds = Array.from(pendingFaultMediaKinds.values());
  const imgCount =
    createForm.faultMediaFiles.filter(f => f.kind === 'image').length + pendingKinds.filter(k => k === 'image').length;
  const vidCount =
    createForm.faultMediaFiles.filter(f => f.kind === 'video').length + pendingKinds.filter(k => k === 'video').length;
  const totalCount = imgCount + vidCount;
  if (kind === 'image') {
    if (imgCount >= FAULT_MEDIA_MAX_IMAGE) {
      window.$message?.error('最多只能上传3张图片');
      return Upload.LIST_IGNORE;
    }
    if (!rejectOversize(file, 10 * CREATE_UPLOAD_MB, '图片')) return Upload.LIST_IGNORE;
    pendingFaultMediaKinds.set(getUploadFileKey(file), 'image');
    return true;
  }
  if (vidCount >= FAULT_MEDIA_MAX_VIDEO) {
    window.$message?.error('最多只能上传1个视频');
    return Upload.LIST_IGNORE;
  }
  if (totalCount >= FAULT_MEDIA_MAX_TOTAL) {
    window.$message?.error('最多只能上传1个视频和3张图片');
    return Upload.LIST_IGNORE;
  }
  if (!rejectOversize(file, 50 * CREATE_UPLOAD_MB, '视频')) return Upload.LIST_IGNORE;
  pendingFaultMediaKinds.set(getUploadFileKey(file), 'video');
  return true;
}

/**
 * 作用：寄件凭证图片上传前校验（jpg/png/webp，最多 2 张）。
 * @param file - 本地文件
 * @returns 允许上传 true；拒绝时 Upload.LIST_IGNORE
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function beforeUploadFaultImage(file: File) {
  const okType =
    ['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || /\.(jpe?g|png|webp)$/i.test(file.name);
  if (!okType) {
    window.$message?.error('仅支持 jpg / png / webp');
    return Upload.LIST_IGNORE;
  }
  if (createForm.senderVoucherFiles.length + pendingSenderVoucherKeys.size >= SENDER_VOUCHER_MAX_COUNT) {
    window.$message?.error('最多只能上传2张图片');
    return Upload.LIST_IGNORE;
  }
  if (!rejectOversize(file, 10 * CREATE_UPLOAD_MB, '图片')) return Upload.LIST_IGNORE;
  pendingSenderVoucherKeys.add(getUploadFileKey(file));
  return true;
}

/**
 * 作用：故障语音上传前校验格式与条数。
 * @param file - 本地文件
 * @returns 允许上传 true；拒绝时 false 或 Upload.LIST_IGNORE
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function beforeUploadFaultVoice(file: File) {
  if (createForm.faultVoiceFiles.length >= FAULT_VOICE_MAX_COUNT) {
    return Upload.LIST_IGNORE;
  }
  const okType =
    ['audio/mpeg', 'audio/wav', 'audio/amr', 'audio/aac', 'audio/mp4'].includes(file.type) ||
    /\.(mp3|wav|amr|aac)$/i.test(file.name);
  if (!okType) {
    window.$message?.error('仅支持 mp3 / wav / amr / aac');
    return false;
  }
  return rejectOversize(file, 10 * CREATE_UPLOAD_MB, '语音');
}

/**
 * 作用：寄件凭证自定义上传实现。
 * @param opt - Upload 自定义请求参数
 * @returns void
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function runSenderVoucherUpload(opt: UploadRequestOption) {
  const raw = opt.file as File;
  const uploadKey = getUploadFileKey(raw);
  if (!pendingSenderVoucherKeys.has(uploadKey)) {
    opt.onError?.(new Error('blocked'));
    return;
  }
  if (createForm.senderVoucherFiles.length >= SENDER_VOUCHER_MAX_COUNT) {
    pendingSenderVoucherKeys.delete(uploadKey);
    opt.onError?.(new Error('limit'));
    return;
  }
  try {
    const { data, error } = await uploadSystemFile(raw);
    if (error) {
      pendingSenderVoucherKeys.delete(uploadKey);
      opt.onError?.(new Error('upload'));
      return;
    }
    const item = pickUploadedFileItem(data);
    if (item) createForm.senderVoucherFiles.push(item);
    pendingSenderVoucherKeys.delete(uploadKey);
    opt.onSuccess?.(data as never, opt.file as never);
  } catch {
    pendingSenderVoucherKeys.delete(uploadKey);
    opt.onError?.(new Error('upload'));
  }
}

/**
 * 作用：判断是否已达到故障媒体（图+视频）总数上限。
 * @returns 是否已达上限
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isFaultMediaAtLimit(): boolean {
  return createForm.faultMediaFiles.length + pendingFaultMediaKinds.size >= FAULT_MEDIA_MAX_TOTAL;
}

/**
 * 作用：判断是否已达到寄件凭证上传上限。
 * @returns 是否已达上限
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isSenderVoucherAtLimit(): boolean {
  return createForm.senderVoucherFiles.length + pendingSenderVoucherKeys.size >= SENDER_VOUCHER_MAX_COUNT;
}

/**
 * 作用：判断是否已达到故障语音条数上限。
 * @returns 是否已达上限
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function isFaultVoiceAtLimit(): boolean {
  return createForm.faultVoiceFiles.length >= FAULT_VOICE_MAX_COUNT;
}

/**
 * 作用：从 UploadFile.response 解析 fileId。
 * @param file - Upload 文件对象
 * @returns 文件 id 或 undefined
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function pickFileIdFromUploadFile(file: UploadFile): number | undefined {
  const item = pickUploadedFileItem(file.response as unknown);
  return item?.fileId;
}

/**
 * 作用：Upload 列表移除时同步删除表单中的 fileId（与提交列表一致）。
 * @param list - 故障媒体或凭证列表引用
 * @returns Ant Design Upload onRemove 回调
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function handleCreateUploadRemove(list: CreateFileItem[] | CreateFaultMediaItem[]) {
  return (file: UploadFile) => {
    if (list === createForm.faultMediaFiles) {
      pendingFaultMediaKinds.delete(getUploadFileKey(file));
    }
    if (list === createForm.senderVoucherFiles) {
      pendingSenderVoucherKeys.delete(getUploadFileKey(file));
    }
    const fid = pickFileIdFromUploadFile(file);
    if (fid !== null && fid !== undefined) removeFileItem(list, fid);
  };
}

onMounted(async () => {
  companyAddressRegionOptions.value = await fetchRegionCascaderOptions();
});

// 条码输入变化：清空则重置解析态；改字但未重查则清缓存，避免提交旧商品信息
watch(
  () => createForm.barcode,
  value => {
    const normalizedBarcode = normalizeText(value);
    if (!normalizedBarcode) {
      resetCreateQueryState();
      syncCreateEntryDefaults(true);
      return;
    }
    if (createForm.barcodeQueried && normalizedBarcode !== createForm.queriedBarcode) {
      resetCreateQueryState();
    }
  }
);

/**
 * 作用：对外打开建单抽屉并完成初始化（入口、地址簿、二级网点默认一级等）。
 * @param entry - 指定入口模式（可选）；非法时回落到当前用户首个可用入口
 * @returns Promise
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
async function open(entry?: CreateEntryMode) {
  const mode = resolveInitialEntry(entry);
  resetCreateFormToEntry(mode);
  createSupplementExpanded.value = false;
  companyAddressDialogMode.value = 'manage';
  createDrawerOpen.value = true;
  loadCompanyAddressList();
  await syncCreateEntryDefaults(createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST);
}

defineExpose({
  open,
  openProxy: () => open(CREATE_ENTRY_PROXY),
  openUpstreamFirst: () => open(CREATE_ENTRY_UPSTREAM_FIRST),
  openUpstreamHq: () => open(CREATE_ENTRY_UPSTREAM_HQ)
});
</script>

<template>
  <ADrawer
    v-model:open="createDrawerOpen"
    title="建维修订单"
    placement="right"
    :width="createDrawerWidth"
    destroy-on-close
    class="work-order-create-drawer"
    :styles="{
      body: { padding: '16px' },
      footer: { borderTop: '1px solid rgba(5, 5, 5, 0.06)', padding: '12px 16px' }
    }"
  >
    <!-- 建单主抽屉：入口 Segmented → 条码查询 → 必填/补充表单 -->
    <div class="flex flex-col gap-16px">
      <!-- 建单入口：代填 / 报修一级 / 报修佳士（选项随当前网点类型变化） -->
      <ASegmented
        v-model:value="createForm.entryMode"
        block
        size="small"
        :options="createEntryOptions"
        class="work-order-create-entry-segmented"
        @change="onTabsChange"
      />

      <AAlert
        type="info"
        show-icon
        class="rounded-8px !border-none !bg-primary/8"
        message="无条码或无法识别条码，系统默认该机器已过保；有条码时请先完成查询再提交。"
        :closable="false"
      />

      <div
        class="work-order-create-body"
        :class="{ 'work-order-create-body--blocked': isCreateUpstreamFirstFormBlocked }"
      >
        <div class="create-section">
          <div class="create-section__head">
            <span class="create-section__bar" />
            <span class="create-section__title">商品查询</span>
          </div>
          <AInputSearch
            v-model:value="createForm.barcode"
            placeholder="请输入产品条形码"
            :loading="createBarcodeLoading"
            :disabled="isCreateUpstreamFirstFormBlocked"
            enter-button="查询"
            @search="queryCreateBarcodeInfo()"
          />
        </div>

        <AForm
          ref="createFormRef"
          class="work-order-create-form"
          :model="createForm"
          :rules="createFormRules as any"
          layout="vertical"
        >
          <div class="create-section">
            <div class="create-section__head">
              <span class="create-section__bar" />
              <span class="create-section__title">必填信息</span>
            </div>
            <ARow :gutter="[12, 0]">
              <!-- 仅代客户填写展示客户手机；上游报修由后端带出发起方信息 -->
              <ACol v-if="showCreateCustomerFields" :span="24" :md="12">
                <AFormItem label="客户手机号码" name="customerMobile" class="mb-12px" required>
                  <AInput v-model:value="createForm.customerMobile" allow-clear placeholder="请输入客户手机号码" />
                </AFormItem>
              </ACol>
              <ACol v-if="showCreateTargetCompany" :span="24" :md="12">
                <AFormItem :label="createTargetCompanyLabel" name="targetCompanyId" class="mb-12px" required>
                  <ASelect
                    v-model:value="createForm.targetCompanyId"
                    :placeholder="createTargetCompanySelectPlaceholder"
                    show-search
                    option-filter-prop="label"
                    :disabled="isCreateTargetAutoFilled"
                    :options="createForm.targetCompanyOptions.map(o => ({ label: o.companyName, value: o.id }))"
                    @change="handleCreateTargetCompanyChange"
                  />
                </AFormItem>
              </ACol>
              <ACol :span="24" :md="12">
                <AFormItem label="选择维修路径" name="serviceMode" class="mb-12px" required>
                  <ASelect
                    v-model:value="createForm.serviceMode"
                    placeholder="请选择维修路径"
                    :options="[...SERVICE_MODE_OPTIONS]"
                  />
                </AFormItem>
              </ACol>
              <ACol v-if="createBarcodeQueryHasFaultDescription" :span="24">
                <AFormItem
                  label="故障描述"
                  name="faultItems"
                  class="mb-12px"
                  :required="createBarcodeQueryHasFaultDescription"
                >
                  <ASelect
                    v-model:value="createForm.faultItems"
                    mode="multiple"
                    max-tag-count="responsive"
                    :placeholder="createFaultPlaceholder"
                    :disabled="isCreateFaultSelectDisabled"
                    :options="effectiveCreateFaultOptions.map(t => ({ label: t, value: t }))"
                  />
                </AFormItem>
              </ACol>
              <ACol v-if="showCreateFaultRemark" :span="24">
                <AFormItem label="故障说明备注" name="faultRemark" class="mb-0" :required="showCreateFaultRemark">
                  <ATextarea
                    v-model:value="createForm.faultRemark"
                    :rows="3"
                    allow-clear
                    placeholder="请输入故障说明备注"
                  />
                </AFormItem>
              </ACol>
              <!-- 邮寄维修：寄件信息只读摘要 + 地址簿选择（提交前 validateSendInfo 校验） -->
              <ACol v-if="isCreateMailMode" :span="24">
                <AFormItem label="寄件信息" class="mb-0" :required="isCreateMailMode">
                  <ATextarea
                    :value="createShippingAddressSummary"
                    :rows="3"
                    disabled
                    placeholder="请选择寄件信息"
                    class="rounded-6px"
                  />
                  <div class="mt-10px flex flex-wrap gap-8px">
                    <AButton type="primary" size="small" @click="openCompanyAddressDialog('select')">
                      选择寄件信息
                    </AButton>
                    <AButton size="small" @click="loadCompanyAddressList({ preserveSelection: true })">刷新</AButton>
                  </div>
                  <AAlert
                    v-if="!companyAddressLoading && !companyAddressList.length"
                    type="warning"
                    show-icon
                    class="mt-10px rounded-8px"
                    message="当前公司还没有可用地址，请先维护公司地址簿后再提交邮寄维修工单。"
                    :closable="false"
                  />
                </AFormItem>
              </ACol>
            </ARow>
          </div>

          <!-- 补充说明默认折叠：故障媒体、语音、邮寄快递单号凭证等选填 -->
          <div class="create-section create-section--supplement">
            <div
              class="create-section__head create-section__head--row create-section__head--click"
              @click="toggleCreateSupplementSection"
            >
              <div class="min-w-0 flex flex-1 items-center gap-8px">
                <span class="create-section__bar" />
                <span class="create-section__title">补充说明</span>
                <span class="truncate text-12px text-gray-500 font-normal">选填</span>
              </div>
              <AButton type="link" size="small" class="shrink-0 !px-4px" @click.stop="toggleCreateSupplementSection">
                {{ createSupplementExpanded ? '收缩' : '展开' }}
              </AButton>
            </div>
            <div v-show="createSupplementExpanded" class="create-section__body">
              <!-- 补充区上传：故障媒体/语音、邮寄快递单号凭证（配额见 FAULT_MEDIA_* 常量） -->
              <AFormItem class="mb-12px" :colon="false">
                <template #label>
                  <div class="create-form-item-label-row">
                    <span class="create-form-item-label-title">故障视频/图片</span>
                    <span class="create-form-item-label-tip create-form-item-label-tip--gray">限1个视频、3张图</span>
                  </div>
                </template>
                <AUpload
                  class="create-upload-picture-card"
                  list-type="picture-card"
                  :max-count="FAULT_MEDIA_MAX_TOTAL"
                  multiple
                  accept=".jpg,.jpeg,.png,.webp,.mp4,.mov,image/jpeg,image/png,image/webp,video/mp4,video/quicktime"
                  :open-file-dialog-on-click="!isFaultMediaAtLimit()"
                  :before-upload="beforeUploadFaultMedia"
                  :custom-request="runFaultMediaUpload"
                  :on-remove="handleCreateUploadRemove(createForm.faultMediaFiles)"
                >
                  <div v-if="!isFaultMediaAtLimit()" class="create-upload-card-trigger">
                    <span class="create-upload-card-trigger__plus">+</span>
                    <span class="create-upload-card-trigger__text">上传</span>
                  </div>
                </AUpload>
              </AFormItem>
              <AFormItem class="mb-12px" :colon="false">
                <template #label>
                  <div class="create-form-item-label-row">
                    <span class="create-form-item-label-title">语音说明</span>
                    <span class="create-form-item-label-tip create-form-item-label-tip--gray">
                      支持 mp3 / wav / amr / aac，单个不超过 10MB，最多 6 个
                    </span>
                  </div>
                </template>
                <div class="create-upload-voice">
                  <AUpload
                    list-type="text"
                    :max-count="FAULT_VOICE_MAX_COUNT"
                    multiple
                    accept=".mp3,.wav,.amr,.aac,audio/mpeg,audio/wav,audio/amr,audio/aac"
                    :open-file-dialog-on-click="!isFaultVoiceAtLimit()"
                    :before-upload="beforeUploadFaultVoice"
                    :custom-request="o => runFileUpload(o, createForm.faultVoiceFiles)"
                    :on-remove="handleCreateUploadRemove(createForm.faultVoiceFiles)"
                    :show-upload-list="{ showPreviewIcon: false, showDownloadIcon: false }"
                  >
                    <AButton v-if="!isFaultVoiceAtLimit()" size="small" type="primary" ghost>选择语音文件</AButton>
                  </AUpload>
                </div>
              </AFormItem>
              <template v-if="isCreateMailMode">
                <AFormItem class="mb-0" :colon="false">
                  <template #label>
                    <div class="create-form-item-label-row">
                      <span class="create-form-item-label-title">寄件快递单号</span>
                      <span class="create-form-item-label-tip create-form-item-label-tip--gray">限2张图片</span>
                    </div>
                  </template>
                  <AUpload
                    class="create-upload-picture-card"
                    list-type="picture-card"
                    :max-count="SENDER_VOUCHER_MAX_COUNT"
                    multiple
                    accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
                    :open-file-dialog-on-click="!isSenderVoucherAtLimit()"
                    :before-upload="beforeUploadFaultImage"
                    :custom-request="runSenderVoucherUpload"
                    :on-remove="handleCreateUploadRemove(createForm.senderVoucherFiles)"
                  >
                    <div v-if="!isSenderVoucherAtLimit()" class="create-upload-card-trigger">
                      <span class="create-upload-card-trigger__plus">+</span>
                      <span class="create-upload-card-trigger__text">凭证</span>
                    </div>
                  </AUpload>
                </AFormItem>
              </template>
            </div>
          </div>
        </AForm>
        <div
          v-if="isCreateUpstreamFirstFormBlocked"
          class="work-order-create-body__mask"
          @click="presentUpstreamFirstTargetBlockedModal()"
        />
      </div>
    </div>

    <!-- 建单提交：先表单校验，再 validateCreateBeforeSubmit / 邮寄 validateSendInfo，无条码可二次确认 -->
    <template #footer>
      <div class="flex justify-end gap-12px">
        <AButton @click="createDrawerOpen = false">取消</AButton>
        <AButton
          type="primary"
          :loading="createSubmitting"
          :disabled="isCreateUpstreamFirstFormBlocked"
          @click="submitCreate"
        >
          提交
        </AButton>
      </div>
    </template>
  </ADrawer>

  <!-- 公司地址簿：邮寄建单选寄件地址 / 管理增删改（与 composeAddressWithRegion 拼完整地址） -->
  <ADrawer v-model:open="companyAddressDialogVisible" :title="companyAddressDialogTitle" :width="760" destroy-on-close>
    <div class="mb-12px">
      <AButton type="primary" size="small" @click="openCompanyAddressForm()">新增地址</AButton>
    </div>
    <ATable
      :columns="companyAddressColumns"
      :data-source="companyAddressList"
      :loading="companyAddressLoading"
      row-key="id"
      size="small"
      :pagination="false"
      :scroll="{ x: 'max-content' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'pick'">
          <ACheckbox
            :checked="String(createForm.companyAddressId ?? '') === String((record as CompanyAddressVO).id)"
            @change="e => onCompanyAddressCheckboxChange(e, record as CompanyAddressVO)"
          />
        </template>
        <template v-else-if="column.key === 'isDefault'">
          <ATag v-if="(record as CompanyAddressVO).isDefault === 1" color="success">默认</ATag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'actions'">
          <ASpace :size="2" :wrap="false">
            <APopconfirm
              v-if="(record as CompanyAddressVO).isDefault !== 1"
              :title="`确认将「${(record as CompanyAddressVO).contactName || '该'}」设为默认地址？`"
              @confirm="handleSetDefaultCompanyAddress(record as CompanyAddressVO)"
            >
              <AButton type="link" size="small">设为默认</AButton>
            </APopconfirm>
            <AButton
              type="link"
              size="small"
              class="table-action-link--primary"
              @click="openCompanyAddressForm(record as CompanyAddressVO)"
            >
              编辑
            </AButton>
            <APopconfirm title="确认删除该地址？" @confirm="handleDeleteCompanyAddress(record as CompanyAddressVO)">
              <AButton type="link" size="small" danger>删除</AButton>
            </APopconfirm>
          </ASpace>
        </template>
      </template>
    </ATable>
    <div class="mt-16px flex justify-end">
      <AButton @click="companyAddressDialogVisible = false">关闭</AButton>
    </div>
    <template #footer>
      <ASpace :size="16">
        <AButton @click="companyAddressDialogVisible = false">关闭</AButton>
      </ASpace>
    </template>
  </ADrawer>

  <!-- 地址编辑子弹层：从地址簿「新增/编辑」打开，保存后刷新列表并尽量保留当前选用项 -->
  <ADrawer v-model:open="companyAddressFormVisible" :title="companyAddressFormTitle" :width="520">
    <AForm
      ref="companyAddressFormRef"
      layout="vertical"
      :model="companyAddressForm"
      :rules="companyAddressFormRules as any"
    >
      <AFormItem label="联系人" name="contactName" required>
        <AInput v-model:value="companyAddressForm.contactName" />
      </AFormItem>
      <AFormItem label="联系电话" name="contactPhone" required>
        <AInput v-model:value="companyAddressForm.contactPhone" />
      </AFormItem>
      <!-- 省市区懒加载级联；详细地址不含省市区前缀，避免重复拼接 -->
      <AFormItem label="省市区" name="regionCodes" required>
        <ACascader
          v-model:value="companyAddressForm.regionCodes"
          class="w-full"
          :options="companyAddressRegionOptions"
          :load-data="loadRegionCascaderData"
          placeholder="请选择省 / 市 / 区"
          allow-clear
        />
      </AFormItem>
      <AFormItem label="详细地址" name="addressDetail" required>
        <ATextarea
          v-model:value="companyAddressForm.addressDetail"
          :rows="3"
          placeholder="请填写街道、门牌号等（不含省市区；保存时会与上方省市区拼接）"
        />
      </AFormItem>
      <!-- 默认地址：syncCreateCompanyAddressSelection 优先选用 isDefault=1 的行 -->
      <AFormItem label="">
        <ACheckbox
          :checked="companyAddressForm.isDefault === 1"
          @change="(e: any) => (companyAddressForm.isDefault = e.target.checked ? 1 : 0)"
        >
          设为默认地址
        </ACheckbox>
      </AFormItem>
    </AForm>
    <template #footer>
      <ASpace :size="16">
        <AButton @click="companyAddressFormVisible = false">取消</AButton>
        <AButton type="primary" :loading="companyAddressSubmitting" @click="submitCompanyAddress">确定</AButton>
      </ASpace>
    </template>
  </ADrawer>
</template>

<style scoped>
.work-order-create-entry-segmented :deep(.ant-segmented) {
  border-radius: 8px;
}

.work-order-create-body {
  position: relative;
}

.work-order-create-body--blocked :deep(.ant-input),
.work-order-create-body--blocked :deep(.ant-input-search),
.work-order-create-body--blocked :deep(.ant-select),
.work-order-create-body--blocked :deep(.ant-input-number),
.work-order-create-body--blocked :deep(textarea.ant-input) {
  pointer-events: none;
}

.work-order-create-body__mask {
  position: absolute;
  inset: 0;
  z-index: 10;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.45);
}

/* 表单项之间留白（原先 margin-bottom:0 会压掉 mb-12px 等工具类） */
.work-order-create-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.work-order-create-form :deep(.ant-form-item.mb-0) {
  margin-bottom: 0;
}

.work-order-create-form :deep(.ant-form-item.mb-8px) {
  margin-bottom: 8px;
}

.work-order-create-form .create-section + .create-section {
  margin-top: 14px;
}

.create-section {
  border-radius: 8px;
  border: 1px solid rgba(5, 5, 5, 0.06);
  background: var(--ant-color-bg-container);
  padding: 14px;
}

.create-section__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
}

.create-section__head--row {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
  justify-content: space-between;
}

.create-section__head--click {
  cursor: pointer;
  user-select: none;
  border-radius: 6px;
  margin: -6px;
  padding: 6px;
  transition: background-color 0.2s;
}

.create-section__head--click:hover {
  background: rgba(5, 5, 5, 0.04);
}

.create-section--supplement .create-section__head--row {
  margin-bottom: 0;
}

.create-section__body {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(5, 5, 5, 0.06);
}

.create-section__bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--ant-color-primary);
  flex-shrink: 0;
}

.create-section__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ant-color-text);
}

.create-meta-block {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(5, 5, 5, 0.02);
  border: 1px solid rgba(5, 5, 5, 0.04);
}

.create-meta-block__label {
  font-size: 12px;
  color: var(--ant-color-text-secondary);
  margin-bottom: 8px;
}

.create-form-item-label-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  width: 100%;
}

.create-form-item-label-title {
  font-weight: 600;
  color: var(--ant-color-text);
}

.create-form-item-label-tip {
  font-size: 12px;
  font-weight: normal;
  line-height: 1.5;
}

.create-form-item-label-tip--gray {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
}

.create-upload-picture-card :deep(.ant-upload-select-picture-card) {
  width: 82px;
  height: 82px;
  margin-inline-end: 8px;
  margin-bottom: 8px;
  border-radius: 8px;
  border-style: dashed;
  border-color: rgba(5, 5, 5, 0.12);
  background: rgba(5, 5, 5, 0.02);
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.create-upload-picture-card :deep(.ant-upload-select-picture-card:hover) {
  border-color: var(--ant-color-primary);
  background: var(--ant-color-primary-bg, rgba(5, 5, 5, 0.02));
}

.create-upload-picture-card :deep(.ant-upload-list-item-container) {
  width: 82px;
  height: 82px;
}

.create-upload-card-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--ant-color-text-secondary);
  line-height: 1.2;
}

.create-upload-picture-card :deep(.ant-upload-select-picture-card:hover) .create-upload-card-trigger {
  color: var(--ant-color-primary);
}

.create-upload-card-trigger__plus {
  font-size: 22px;
  font-weight: 300;
  line-height: 1;
}

.create-upload-card-trigger__text {
  font-size: 12px;
}

.create-upload-voice {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px dashed rgba(5, 5, 5, 0.12);
  background: rgba(5, 5, 5, 0.02);
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.create-upload-voice:hover {
  border-color: rgba(5, 5, 5, 0.18);
}

.create-upload-voice :deep(.ant-upload-select) {
  display: block;
}

.create-upload-voice :deep(.ant-upload-list) {
  margin-top: 10px;
}
</style>
