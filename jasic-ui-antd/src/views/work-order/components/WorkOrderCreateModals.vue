<script setup lang="ts">
/* eslint-disable vue/component-name-in-template-casing -- Ant Design Vue Input.TextArea */
import { computed, reactive, ref, watch } from 'vue';
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
import { useAuthStore } from '@/store/modules/auth';
import {
  chinaRegionCascaderOptions,
  composeAddressWithRegion,
  isFullRegionSelection,
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

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const authStore = useAuthStore();
const createDrawerOpen = ref(false);
const createForm = reactive<CreateWorkOrderFormState>(buildDefaultCreateForm());
const createFormRef = ref<FormInstance | null>(null);
const createSubmitting = ref(false);
const createBarcodeLoading = ref(false);
const createSupplementExpanded = ref(false);

const companyAddressLoading = ref(false);
const companyAddressList = ref<CompanyAddressVO[]>([]);
const companyAddressDialogVisible = ref(false);
const companyAddressDialogMode = ref<'select' | 'manage'>('manage');
const companyAddressFormVisible = ref(false);
const companyAddressFormTitle = ref('新增地址');
const companyAddressSubmitting = ref(false);
const companyAddressForm = reactive({
  id: undefined as number | undefined,
  contactName: '',
  contactPhone: '',
  addressDetail: '',
  regionCodes: [] as string[],
  isDefault: 0 as 0 | 1
});

const createEntryOptions = computed(() => getCreateEntryOptions(authStore.userInfo.currentTypeCode));

const isCreateUpstreamEntry = computed(() => createForm.entryMode !== CREATE_ENTRY_PROXY);
const showCreateCustomerFields = computed(() => !isCreateUpstreamEntry.value);
const isCreateMailMode = computed(() => createForm.serviceMode === SERVICE_MODE_MAIL);
const hasCreateResolvedBarcodeInfo = computed(() => Boolean(createForm.barcodeResolved));
const showCreateTargetCompany = computed(
  () => isCreateUpstreamEntry.value && (createForm.targetCompanyOptions || []).length > 0
);
const createTargetCompanyLabel = computed(() =>
  createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST ? '目标一级' : '目标总部'
);
/** 与小程序 `请选择${upstreamTargetCompanyLabel}` 一致 */
const createTargetCompanySelectPlaceholder = computed(() => `请选择${createTargetCompanyLabel.value}`);
const isCreateTargetAutoFilled = computed(() => (createForm.targetCompanyOptions || []).length <= 1);

/** 与小程序 `isOtherFaultSelection` 语义对齐（选项 value 多为故障文案） */
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

/** 与 contractor `jasicRepair` 一致：仅条码查询成功且接口返回非空 faultOptions 时展示「故障描述」多选并校验必填 */
const createBarcodeQueryHasFaultDescription = computed(
  () =>
    createForm.barcodeQueried &&
    !createForm.barcodeQueryFailed &&
    (createForm.faultOptions || []).some(o => Boolean(normalizeText(o)))
);
const effectiveCreateFaultOptions = computed(() => {
  if (!createBarcodeQueryHasFaultDescription.value) return [];
  return createForm.faultOptions || [];
});
const isCreateFaultSelectDisabled = computed(
  () => Boolean(normalizeText(createForm.barcode)) && !createForm.barcodeQueried
);
/** 与小程序 `syncShowFaultRemarkFromState` 一致 */
const showCreateFaultRemark = computed(() => {
  const code = normalizeText(createForm.barcode);
  if (!code) return true;
  if (createBarcodeQueryHasFaultDescription.value) {
    return (createForm.faultItems || []).some(item => isCreateOtherFaultItem(item));
  }
  if (hasCreateResolvedBarcodeInfo.value || createForm.barcodeQueryFailed) return true;
  return false;
});
const createDrawerWidth = computed(() => {
  // 字段较多时加宽抽屉，便于双列排版减少换行
  if (hasCreateResolvedBarcodeInfo.value || isCreateMailMode.value || showCreateTargetCompany.value) {
    return 760;
  }
  return 560;
});
const createFaultPlaceholder = computed(() => {
  if (isCreateFaultSelectDisabled.value) return '请先完成商品查询';
  if (!createBarcodeQueryHasFaultDescription.value) return '当前查询结果无故障选项，请填写下方故障说明备注';
  return '请选择';
});
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
const companyAddressDialogTitle = computed(() =>
  companyAddressDialogMode.value === 'select' ? '选择寄件信息' : '公司地址簿'
);

const companyAddressColumns = computed(() => {
  const rest = [
    { title: '默认', key: 'isDefault', width: 88, align: 'center' as const },
    { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
    { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
    { title: '详细地址', dataIndex: 'address', key: 'address', ellipsis: true },
    {
      title: '操作',
      key: 'actions',
      width: companyAddressDialogMode.value === 'select' ? 200 : 240,
      fixed: 'right' as const
    }
  ];
  if (companyAddressDialogMode.value === 'select') {
    return [{ title: '', key: 'pick', width: 48, align: 'center' as const }, ...rest];
  }
  return rest;
});

/** 与 jasic-ui createRules 一致 */
const createFormRules = computed(() => ({
  customerMobile: [
    {
      validator: async (_rule: unknown, value: string) => {
        const m = normalizeText(value);
        if (createForm.entryMode === CREATE_ENTRY_PROXY && !m) {
          return Promise.reject(new Error('请输入客户手机号码'));
        }
        if (createForm.entryMode === CREATE_ENTRY_PROXY && m && !/^1[3-9]\d{9}$/.test(m)) {
          return Promise.reject(new Error('请输入正确的手机号码'));
        }
        return Promise.resolve();
      },
      trigger: 'blur'
    }
  ],
  serviceMode: [{ required: true, message: '请选择维修路径', trigger: 'change' }]
}));

function resolveInitialEntry(entry?: CreateEntryMode): CreateEntryMode {
  const opts = createEntryOptions.value;
  const allowed = new Set(opts.map(o => o.value));
  if (entry && allowed.has(entry)) return entry;
  return opts[0]!.value;
}

function assignCreateForm(next: CreateWorkOrderFormState) {
  Object.assign(createForm, next);
}

function resetCreateFormToEntry(entry: CreateEntryMode) {
  const d = buildDefaultCreateForm();
  d.entryMode = entry;
  assignCreateForm(d);
}

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
  syncCreateEntryDefaults();
}

function onTabsChange(key: string | number) {
  handleCreateEntryModeChange(String(key) as CreateEntryMode);
}

function toggleCreateSupplementSection() {
  createSupplementExpanded.value = !createSupplementExpanded.value;
}

async function syncCreateEntryDefaults() {
  if (createForm.entryMode !== CREATE_ENTRY_UPSTREAM_FIRST || normalizeText(createForm.barcode)) {
    return;
  }
  try {
    const { data } = await listUpstreamFirstCreateTargetOptions();
    const listRaw = Array.isArray(data) ? data : (data as { records?: unknown[] })?.records || [];
    const list = listRaw.map((r: Record<string, unknown>) => normalizeTargetCompanyOption(r)).filter(Boolean) as Array<{
      id: number;
      companyName: string;
    }>;
    if (!list.length || list.length !== 1) {
      createForm.targetCompanyId = undefined;
      createForm.targetCompanyName = '';
      createForm.targetCompanyOptions = list;
      window.$message?.error('当前二级网点未带出唯一一级网点，请联系管理员排查');
      return;
    }
    const target = list[0]!;
    createForm.targetCompanyId = target.id;
    createForm.targetCompanyName = target.companyName || '';
    createForm.targetCompanyOptions = list;
  } catch {
    createForm.targetCompanyId = undefined;
    createForm.targetCompanyName = '';
    createForm.targetCompanyOptions = [];
  }
}

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

  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && targetCompanyOptions.length !== 1) {
    createForm.targetCompanyId = undefined;
    createForm.targetCompanyName = '';
    window.$message?.error('当前二级网点未带出唯一一级网点，请联系管理员排查');
  }
}

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

function pickApiMessage(result: unknown): string {
  if (!result || typeof result !== 'object') return '';
  const obj = result as Record<string, unknown>;
  return normalizeText(obj.msg ?? obj.message);
}

async function queryCreateBarcodeInfo(options: { preserveTargetSelection?: boolean; silentSuccess?: boolean } = {}) {
  const barcode = normalizeText(createForm.barcode);
  if (!barcode) {
    window.$message?.warning('请输入条形码');
    return;
  }
  const req = resolveCreateBarcodeInfoRequest(barcode);
  if (!req) {
    window.$message?.error('当前建单入口不支持查条码');
    return;
  }
  createBarcodeLoading.value = true;
  resetCreateQueryState({ preserveTargetSelection: Boolean(options.preserveTargetSelection) });
  try {
    const res = await req;
    const body = (res?.data || {}) as Record<string, unknown>;
    applyCreateBarcodeInfo(body, barcode);
    if (!options.silentSuccess) {
      const msg = pickApiMessage(res);
      if (msg) window.$message?.success(msg);
    }
  } catch {
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
    await syncCreateEntryDefaults();
  } finally {
    createBarcodeLoading.value = false;
  }
}

function handleCreateTargetCompanyChange() {
  if (!createForm.barcodeQueried) return;
  queryCreateBarcodeInfo({ preserveTargetSelection: true, silentSuccess: true });
}

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

function applySelectedCompanyAddress(address: CompanyAddressVO | null) {
  createForm.companyAddressId = address ? address.id : undefined;
  createForm.senderName = address ? address.contactName || '' : '';
  createForm.senderMobile = address ? address.contactPhone || '' : '';
  createForm.senderAddress = address ? address.address || '' : '';
}

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

function handleSelectCompanyAddress(row: CompanyAddressVO) {
  applySelectedCompanyAddress(row);
  companyAddressDialogVisible.value = false;
}

function onCompanyAddressCheckboxChange(e: CheckboxChangeEvent, row: CompanyAddressVO) {
  if (e.target.checked) handleSelectCompanyAddress(row);
}

function openCompanyAddressDialog(mode: 'select' | 'manage' = 'manage') {
  companyAddressDialogMode.value = mode;
  companyAddressDialogVisible.value = true;
  loadCompanyAddressList({ preserveSelection: true });
}

function openCompanyAddressForm(address?: CompanyAddressVO | null) {
  if (address) {
    companyAddressFormTitle.value = '编辑地址';
    companyAddressForm.id = address.id;
    companyAddressForm.contactName = address.contactName || '';
    companyAddressForm.contactPhone = address.contactPhone || '';
    const parsed = splitFullAddressToRegionAndDetail(address.address || '');
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

async function submitCompanyAddress() {
  const isDef = (companyAddressForm.isDefault === 1 ? 1 : 0) as 0 | 1;
  const contactName = normalizeText(companyAddressForm.contactName);
  const contactPhone = normalizeText(companyAddressForm.contactPhone);
  const detail = normalizeText(companyAddressForm.addressDetail);

  if (!contactName) {
    window.$message?.error('请输入联系人');
    return;
  }
  if (!contactPhone) {
    window.$message?.error('请输入联系电话');
    return;
  }
  if (!isFullRegionSelection(companyAddressForm.regionCodes)) {
    window.$message?.error('请选择完整的省、市、区');
    return;
  }
  if (!detail) {
    window.$message?.error('请输入详细地址');
    return;
  }

  const address = composeAddressWithRegion(companyAddressForm.regionCodes, detail);

  const payload = {
    contactName,
    contactPhone,
    address,
    isDefault: isDef
  };
  companyAddressSubmitting.value = true;
  try {
    if (companyAddressForm.id) {
      const res = await updateCompanyAddress({ id: companyAddressForm.id, ...payload });
      const msg = pickApiMessage(res);
      if (msg) window.$message?.success(msg);
    } else {
      const res = await createCompanyAddress(payload);
      const msg = pickApiMessage(res);
      if (msg) window.$message?.success(msg);
    }
    companyAddressFormVisible.value = false;
    await loadCompanyAddressList({ preserveSelection: true });
  } finally {
    companyAddressSubmitting.value = false;
  }
}

async function handleDeleteCompanyAddress(row: CompanyAddressVO) {
  if (!row?.id) return;
  Modal.confirm({
    title: '提示',
    content: '确认删除该地址吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await deleteCompanyAddress(row.id);
      const msg = pickApiMessage(res);
      if (msg) window.$message?.success(msg);
      await loadCompanyAddressList({ preserveSelection: true });
    }
  });
}

async function handleSetDefaultCompanyAddress(row: CompanyAddressVO) {
  if (!row?.id || row.isDefault === 1) return;
  const res = await setDefaultCompanyAddress(row.id);
  const msg = pickApiMessage(res);
  if (msg) window.$message?.success(msg);
  await loadCompanyAddressList({ preserveSelection: true });
}

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

function validateCreateBeforeSubmit(): boolean {
  const barcode = normalizeText(createForm.barcode);
  if (barcode && (!createForm.barcodeQueried || createForm.queriedBarcode !== barcode)) {
    window.$message?.error('请先查询商品，再提交');
    return false;
  }
  if (createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && !createForm.targetCompanyId) {
    window.$message?.error('当前二级网点未带出一级网点，请联系管理员排查');
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

function resolveCreateFaultItemsForSubmit(): string[] {
  if (createBarcodeQueryHasFaultDescription.value) {
    return (createForm.faultItems || []).map(item => normalizeText(item)).filter(Boolean);
  }
  return [];
}

function buildCreatePayload(): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    customerName: normalizeText(createForm.customerName),
    customerMobile: normalizeText(createForm.customerMobile),
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
  if (isCreateMailMode.value) {
    payload.senderName = normalizeText(createForm.senderName);
    payload.senderMobile = normalizeText(createForm.senderMobile);
    payload.senderAddress = normalizeText(createForm.senderAddress);
    payload.sendExpressNo = normalizeText(createForm.sendExpressNo);
    payload.senderVoucherFileIds = buildFileIdList(createForm.senderVoucherFiles);
  }
  if (createForm.entryMode !== CREATE_ENTRY_PROXY) {
    payload.targetCompanyId = createForm.targetCompanyId;
  }
  return payload;
}

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
      const msg = pickApiMessage(res);
      if (msg) window.$message?.success(msg);
      createDrawerOpen.value = false;
      emit('created');
    } finally {
      createSubmitting.value = false;
    }
  };

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

const CREATE_UPLOAD_MB = 1024 * 1024;

/** 与 contractor `validateFaultMediaSelection` 一致：最多 1 个视频、3 张图片 */
const FAULT_MEDIA_MAX_IMAGE = 3;
const FAULT_MEDIA_MAX_VIDEO = 1;
const FAULT_MEDIA_MAX_TOTAL = FAULT_MEDIA_MAX_IMAGE + FAULT_MEDIA_MAX_VIDEO;
const SENDER_VOUCHER_MAX_COUNT = 2;
const FAULT_VOICE_MAX_COUNT = 6;
const pendingFaultMediaKinds = new Map<string, 'image' | 'video'>();
const pendingSenderVoucherKeys = new Set<string>();

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

function removeFileItem(list: CreateFileItem[], fileId: number) {
  const i = list.findIndex(f => f.fileId === fileId);
  if (i >= 0) list.splice(i, 1);
}

function rejectOversize(file: File, maxBytes: number, label: string): boolean {
  if (file.size > maxBytes) {
    window.$message?.error(`${label}单文件不能超过 ${Math.round(maxBytes / CREATE_UPLOAD_MB)}MB`);
    return false;
  }
  return true;
}

function getUploadFileKey(file: File | UploadFile): string {
  const anyFile = file as unknown as Record<string, unknown>;
  const uid = normalizeText(anyFile.uid);
  if (uid) return uid;
  const name = normalizeText(anyFile.name);
  const size = Number(anyFile.size || 0);
  const lastModified = Number(anyFile.lastModified || 0);
  return `${name}-${size}-${lastModified}`;
}

function inferFaultMediaKind(file: File): 'image' | 'video' | null {
  if (['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || /\.(jpe?g|png|webp)$/i.test(file.name)) {
    return 'image';
  }
  if (['video/mp4', 'video/quicktime'].includes(file.type) || /\.(mp4|mov)$/i.test(file.name)) {
    return 'video';
  }
  return null;
}

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

function isFaultMediaAtLimit(): boolean {
  return createForm.faultMediaFiles.length + pendingFaultMediaKinds.size >= FAULT_MEDIA_MAX_TOTAL;
}

function isSenderVoucherAtLimit(): boolean {
  return createForm.senderVoucherFiles.length + pendingSenderVoucherKeys.size >= SENDER_VOUCHER_MAX_COUNT;
}

function isFaultVoiceAtLimit(): boolean {
  return createForm.faultVoiceFiles.length >= FAULT_VOICE_MAX_COUNT;
}

function pickFileIdFromUploadFile(file: UploadFile): number | undefined {
  const item = pickUploadedFileItem(file.response as unknown);
  return item?.fileId;
}

/** 与 Upload 列表移除同步，保证提交用的 fileId 列表一致 */
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

watch(
  () => createForm.barcode,
  value => {
    const normalizedBarcode = normalizeText(value);
    if (!normalizedBarcode) {
      resetCreateQueryState();
      syncCreateEntryDefaults();
      return;
    }
    if (createForm.barcodeQueried && normalizedBarcode !== createForm.queriedBarcode) {
      resetCreateQueryState();
    }
  }
);

async function open(entry?: CreateEntryMode) {
  const mode = resolveInitialEntry(entry);
  resetCreateFormToEntry(mode);
  createSupplementExpanded.value = false;
  companyAddressDialogMode.value = 'manage';
  createDrawerOpen.value = true;
  loadCompanyAddressList();
  await syncCreateEntryDefaults();
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
    <div class="flex flex-col gap-16px">
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

      <div class="create-section">
        <div class="create-section__head">
          <span class="create-section__bar" />
          <span class="create-section__title">商品查询</span>
        </div>
        <AInputSearch
          v-model:value="createForm.barcode"
          placeholder="请输入产品条形码"
          :loading="createBarcodeLoading"
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
            <ACol v-if="showCreateCustomerFields" :span="24" :md="12">
              <AFormItem label="客户手机号码" name="customerMobile" class="mb-12px">
                <AInput v-model:value="createForm.customerMobile" allow-clear placeholder="请输入客户手机号码" />
              </AFormItem>
            </ACol>
            <ACol v-if="showCreateTargetCompany" :span="24" :md="12">
              <AFormItem :label="createTargetCompanyLabel" name="targetCompanyId" class="mb-12px">
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
              <AFormItem label="选择维修路径" name="serviceMode" class="mb-12px">
                <ASelect
                  v-model:value="createForm.serviceMode"
                  placeholder="请选择维修路径"
                  :options="[...SERVICE_MODE_OPTIONS]"
                />
              </AFormItem>
            </ACol>
            <ACol v-if="createBarcodeQueryHasFaultDescription" :span="24">
              <AFormItem label="故障描述" name="faultItems" class="mb-12px">
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
              <AFormItem label="故障说明备注" name="faultRemark" class="mb-0">
                <ATextarea
                  v-model:value="createForm.faultRemark"
                  :rows="3"
                  allow-clear
                  placeholder="请输入故障说明备注"
                />
              </AFormItem>
            </ACol>
            <ACol v-if="isCreateMailMode" :span="24">
              <AFormItem label="寄件信息" class="mb-0">
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
    </div>

    <template #footer>
      <div class="flex justify-end gap-12px">
        <AButton @click="createDrawerOpen = false">取消</AButton>
        <AButton type="primary" :loading="createSubmitting" @click="submitCreate">提交</AButton>
      </div>
    </template>
  </ADrawer>

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
          <AButton
            v-if="(record as CompanyAddressVO).isDefault !== 1"
            type="link"
            size="small"
            @click="handleSetDefaultCompanyAddress(record as CompanyAddressVO)"
          >
            设为默认
          </AButton>
          <AButton
            type="link"
            size="small"
            class="table-action-link--primary"
            @click="openCompanyAddressForm(record as CompanyAddressVO)"
          >
            编辑
          </AButton>
          <AButton type="link" size="small" danger @click="handleDeleteCompanyAddress(record as CompanyAddressVO)">
            删除
          </AButton>
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

  <ADrawer v-model:open="companyAddressFormVisible" :title="companyAddressFormTitle" :width="520">
    <AForm layout="vertical">
      <AFormItem label="联系人" required>
        <AInput v-model:value="companyAddressForm.contactName" />
      </AFormItem>
      <AFormItem label="联系电话" required>
        <AInput v-model:value="companyAddressForm.contactPhone" />
      </AFormItem>
      <AFormItem label="省市区" required>
        <ACascader
          v-model:value="companyAddressForm.regionCodes"
          class="w-full"
          :options="chinaRegionCascaderOptions"
          placeholder="请选择省 / 市 / 区"
          allow-clear
        />
      </AFormItem>
      <AFormItem label="详细地址" required>
        <ATextarea
          v-model:value="companyAddressForm.addressDetail"
          :rows="3"
          placeholder="请填写街道、门牌号等（不含省市区；保存时会与上方省市区拼接）"
        />
      </AFormItem>
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
