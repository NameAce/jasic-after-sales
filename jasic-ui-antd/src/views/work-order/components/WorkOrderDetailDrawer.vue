<script setup lang="ts">
/* eslint-disable vue/component-name-in-template-casing -- Ant Design Vue Input.TextArea */
import { computed, reactive, ref, watch } from 'vue';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import type { UploadRequestOption } from 'ant-design-vue/es/vc-upload/interface';
import {
  assignWorkOrder,
  closeWorkOrder,
  getWorkOrder,
  listAssignUserOptions,
  listRepairFaultOptions,
  listRepairProductModelOptions,
  listTransferTargetOptions,
  repairWorkOrder,
  reviewWorkOrder,
  techAcceptWorkOrder,
  transferWorkOrder,
  updateRepairProductModel,
  updateWorkOrderSendExpress,
  uploadSystemFile
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';
import type { WorkOrderListActionCode } from '../list-actions';

/** 维修登记 / 复检登记「故障处图片」每格 1 张；与建单页 picture-card 达上限隐藏「+」逻辑一致 */
const DETAIL_UPLOAD_SINGLE_MAX = 1;
const DETAIL_UPLOAD_CLOSE_VOUCHER_MAX = 4;

const props = defineProps<{
  open: boolean;
  workOrderId: number | null;
}>();

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void;
  (e: 'success'): void;
}>();

const loading = ref(false);
const detail = ref<Record<string, unknown>>({});
const previewOpen = ref(false);
const previewType = ref<'image' | 'video'>('image');
const previewUrl = ref('');
const previewTitle = ref('');
const { hasAuth } = useAuth();
const authStore = useAuthStore();

const assignOpen = ref(false);
const assignSubmitting = ref(false);
const assignUserId = ref<number | undefined>(undefined);
const assignOptions = ref<Array<{ label: string; value: number }>>([]);
const faultJudgeHasFault = '有故障';
const faultJudgeNoFault = '无故障';
const OTHER_REPAIR_OPTION = '其它维修说明';
const DEFAULT_OTHER_FAULT_LABEL = '其它故障';

const transferOpen = ref(false);
const transferSubmitting = ref(false);
const transferCompanyId = ref<number | undefined>(undefined);
const transferRemark = ref('');
const transferOptions = ref<Array<{ label: string; value: number }>>([]);

const repairOpen = ref(false);
const repairSubmitting = ref(false);
const repairForm = reactive({
  quoteAmount: undefined as number | undefined,
  quoteDesc: '',
  faultItems: [] as string[],
  faultRemark: '',
  repairDesc: '',
  repairItems: [] as string[],
  otherDesc: '',
  partList: [{ partName: '', partQty: 1 }] as Array<{ partName: string; partQty?: number }>,
  faultOldImageFileIds: [] as number[],
  faultImageFileIds: [] as number[],
  machineImageFileIds: [] as number[],
  machineBarcodeImageFileIds: [] as number[],
  otherImageFileIds: [] as number[]
});

const reviewOpen = ref(false);
const reviewSubmitting = ref(false);
const reviewForm = reactive({
  repairDesc: '',
  repairItems: [] as string[],
  otherDesc: '',
  partList: [{ partName: '', partQty: 1 }] as Array<{ partName: string; partQty?: number }>,
  faultOldImageFileIds: [] as number[],
  faultImageFileIds: [] as number[],
  machineImageFileIds: [] as number[],
  machineBarcodeImageFileIds: [] as number[],
  otherImageFileIds: [] as number[]
});

const mailOpen = ref(false);
const mailSubmitting = ref(false);
const mailForm = reactive({
  senderVoucherFileIds: [] as number[]
});

const closeOpen = ref(false);
const closeSubmitting = ref(false);
const closeForm = reactive({
  returnMethod: '自提',
  closeReason: '',
  returnVoucherFileIds: [] as number[]
});

const techAcceptOpen = ref(false);
const techAcceptSubmitting = ref(false);
const techAcceptForm = reactive({
  faultJudge: '',
  quoteAmount: undefined as number | undefined,
  quoteDesc: ''
});
const actionRepairFaultOptions = ref<Array<{ faultDesc: string; repairOptions: string[] }>>([]);
const pendingTechAcceptPayload = ref<Record<string, unknown> | null>(null);
const repairProductModelOpen = ref(false);
const repairProductModelSubmitting = ref(false);
const repairProductModelOptionsLoading = ref(false);
const repairProductModelPendingAction = ref<WorkOrderListActionCode | ''>('');
const repairProductModel = ref('');
const repairProductModelOptions = ref<string[]>([]);

function pickRows(data: unknown) {
  if (Array.isArray(data)) return data;
  if (data && typeof data === 'object') {
    const o = data as Record<string, unknown>;
    if (Array.isArray(o.records)) return o.records;
  }
  return [];
}

function pickUploadedFileId(raw: unknown): number | undefined {
  if (raw && typeof raw === 'object') {
    const o = raw as Record<string, unknown>;
    const id = o.id ?? o.fileId;
    if (typeof id === 'number' && Number.isFinite(id)) return id;
    if (typeof id === 'string' && /^\d+$/.test(id)) return Number(id);
  }
  return undefined;
}

/** 与建单页一致：从 Upload 列表删除时同步移除已提交的 fileId */
function handleDetailIdUploadRemove(bucket: number[]) {
  return (file: UploadFile) => {
    const fid = pickUploadedFileId(file.response as unknown);
    if (typeof fid === 'number') {
      const idx = bucket.indexOf(fid);
      if (idx !== -1) bucket.splice(idx, 1);
    }
  };
}

const id = computed(() => props.workOrderId);

const availableActionCodes = computed(() => {
  const raw = detail.value?.availableActions;
  const list = Array.isArray(raw) ? raw : [];
  return list.filter(item => typeof item === 'string' && item !== 'RETURN_METHOD') as WorkOrderListActionCode[];
});
function hasAction(action: WorkOrderListActionCode) {
  return availableActionCodes.value.includes(action);
}
function shouldShowAssignFallback() {
  if (!detail.value || typeof detail.value !== 'object') return false;
  if (hasAction('ASSIGN')) return false;
  if (!hasAuth('workorder:assign')) return false;
  const d = detail.value as Record<string, unknown>;
  if (String(d.mainStatus || '') !== 'PENDING_ASSIGN') return false;
  if (d.assignedUserId !== undefined && d.assignedUserId !== null && String(d.assignedUserId) !== '') return false;
  return String(d.currentAcceptCompanyId || '') === String(authStore.userInfo.currentCompanyId || '');
}
const showAssign = computed(() => hasAction('ASSIGN') || shouldShowAssignFallback());
const showAccept = computed(() => hasAction('TECH_ACCEPT'));
const showTransfer = computed(() => hasAction('TRANSFER'));
const showRepair = computed(() => hasAction('REPAIR_FINISH'));
const showReview = computed(() => hasAction('REVIEW'));
const showMail = computed(() => hasAction('UPLOAD_SEND_EXPRESS'));
const showClose = computed(() => hasAction('CLOSE'));
const hasRepairFaultConfig = computed(() => actionRepairFaultOptions.value.length > 0);
const repairFaultOptionsWithOther = computed(() => {
  const list = actionRepairFaultOptions.value.map(item => item.faultDesc).filter(Boolean);
  if (!list.includes(DEFAULT_OTHER_FAULT_LABEL)) list.push(DEFAULT_OTHER_FAULT_LABEL);
  return list;
});
const firstRepairFaultRecord = computed(() => {
  const repairs = (detail.value.repairs || []) as Array<Record<string, unknown>>;
  for (let i = repairs.length - 1; i >= 0; i -= 1) {
    const repair = repairs[i];
    const stage = String(repair?.registerStage || '');
    const faults = Array.isArray(repair?.faults) ? (repair.faults as Array<Record<string, unknown>>) : [];
    if (stage === 'REPAIR' && faults.length) return faults[0]!;
  }
  return null;
});
const firstRepairConfirmedFaultDesc = computed(() => normalizeText(firstRepairFaultRecord.value?.faultDesc));
const firstRepairConfirmedFaultRemark = computed(() => normalizeText(firstRepairFaultRecord.value?.faultRemark));
const reviewFaultItems = computed(() => splitFaultDescSelections(firstRepairConfirmedFaultDesc.value));
const showRepairFaultRemarkInput = computed(() =>
  normalizeFaultItems(repairForm.faultItems).includes(DEFAULT_OTHER_FAULT_LABEL)
);
const showReviewFaultRemark = computed(() => Boolean(firstRepairConfirmedFaultRemark.value));
const currentRepairOptions = computed(() => {
  const optionSet = new Set<string>();
  normalizeFaultItems(repairForm.faultItems).forEach(faultDesc => {
    const matched = actionRepairFaultOptions.value.find(item => item.faultDesc === faultDesc);
    (matched?.repairOptions || []).forEach(item => {
      const value = normalizeText(item);
      if (value) optionSet.add(value);
    });
  });
  return [...optionSet];
});
const reviewRepairOptions = computed(() => {
  const optionSet = new Set<string>();
  reviewFaultItems.value.forEach(faultDesc => {
    const matched = actionRepairFaultOptions.value.find(item => item.faultDesc === faultDesc);
    (matched?.repairOptions || []).forEach(item => {
      const value = normalizeText(item);
      if (value) optionSet.add(value);
    });
  });
  return [...optionSet];
});
const isOtherRepairSelected = computed(() =>
  normalizeRepairItems(repairForm.repairItems).includes(OTHER_REPAIR_OPTION)
);
const isOtherReviewSelected = computed(() =>
  normalizeRepairItems(reviewForm.repairItems).includes(OTHER_REPAIR_OPTION)
);

async function loadDetail() {
  if (!id.value) {
    detail.value = {};
    return;
  }
  loading.value = true;
  try {
    const { data, error } = await getWorkOrder(id.value);
    if (!error && data && typeof data === 'object') {
      detail.value = data as Record<string, unknown>;
    } else {
      detail.value = {};
    }
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.open, props.workOrderId] as const,
  ([isOpen]) => {
    if (isOpen) loadDetail();
  },
  { immediate: true }
);

function valueByKeys(...keys: string[]) {
  const d = detail.value as Record<string, unknown>;
  for (const key of keys) {
    const value = d[key];
    if (value !== undefined && value !== null && String(value).trim() !== '') return value;
  }
  return '';
}

function containsOtherFault(raw: unknown) {
  const text = normalizeText(raw);
  if (!text) return false;
  return text.includes('其它故障') || text.includes('其他故障');
}

type DetailInfoItem = {
  key: string;
  label: string;
  value: unknown;
  span?: number;
};

const workOrderInfoItems = computed<DetailInfoItem[]>(() => [
  { key: 'orderNo', label: '工单号', value: valueByKeys('orderNo') },
  { key: 'mainStatus', label: '主状态', value: valueByKeys('mainStatusLabel', 'mainStatus', 'displayStatus') },
  { key: 'brandType', label: '工单类型', value: valueByKeys('brandTypeLabel', 'brandType') },
  { key: 'submitTime', label: '提交时间', value: valueByKeys('submitTime', 'createTime') },
  { key: 'applicationSource', label: '申请来源', value: valueByKeys('applicationSourceName') },
  { key: 'createEntryType', label: '建单入口', value: valueByKeys('createEntryTypeLabel', 'createEntryType') },
  { key: 'createCompanyName', label: '建单公司', value: valueByKeys('createCompanyName') },
  { key: 'hqCompanyName', label: '归属总部', value: valueByKeys('hqCompanyName') }
]);

const productInfoItems = computed<DetailInfoItem[]>(() => [
  { key: 'productModel', label: '机器型号', value: valueByKeys('productModel') },
  { key: 'barcode', label: '条形码', value: valueByKeys('barcode') },
  { key: 'serialNo', label: '机器小号', value: valueByKeys('serialNo') },
  { key: 'productCode', label: '物料编码', value: valueByKeys('productCode') },
  { key: 'brandCode', label: '品牌编码', value: valueByKeys('brandCode') },
  { key: 'brandName', label: '品牌名称', value: valueByKeys('brandName') },
  { key: 'lastOutDate', label: '最后出库日期', value: valueByKeys('lastOutDate') },
  { key: 'warrantyStatus', label: '质保判定', value: valueByKeys('warrantyStatus') }
]);

const statusText = computed(() => normalizeText(valueByKeys('mainStatusLabel', 'mainStatus', 'displayStatus')));
const showCompletedRepairInfo = computed(() => /已完成|完成|COMPLETED|FINISHED/i.test(statusText.value));

/** 详情接口 `quotes`（`WorkOrderQuoteVO`：faultJudge、quoteAmount、quoteDesc 等） */
const detailQuotesList = computed(() => {
  const raw = (detail.value as Record<string, unknown>)?.quotes;
  return Array.isArray(raw) ? (raw as Record<string, unknown>[]) : [];
});

/** 当前有效报价行，无则取最新一条（与后端 `listQuoteVos` 排序一致） */
const displayQuoteForWorkOrder = computed(() => {
  const list = detailQuotesList.value;
  if (!list.length) return null;
  const current = list.find(r => Number(r.isCurrentValid) === 1);
  return current || list[0] || null;
});

/** 维修信息区顶部：故障判定 / 维修报价 / 说明（接口 `quotes` 有值才展示） */
const repairQuoteSummaryItems = computed<DetailInfoItem[]>(() => {
  const q = displayQuoteForWorkOrder.value;
  if (!q) return [];
  const items: DetailInfoItem[] = [];
  const fj = normalizeText(q.faultJudge);
  if (fj) items.push({ key: 'quoteFaultJudge', label: '故障判定', value: fj });
  const amt = q.quoteAmount;
  if (amt !== undefined && amt !== null && String(amt).trim() !== '') {
    items.push({ key: 'quoteAmount', label: '维修报价', value: amt });
  }
  const desc = normalizeText(q.quoteDesc);
  if (desc) items.push({ key: 'quoteDesc', label: '维修报价说明', value: desc, span: 2 });
  return items;
});

function serviceInfoFieldHasValue(value: unknown): boolean {
  if (value === undefined || value === null) return false;
  if (typeof value === 'number') return Number.isFinite(value);
  return String(value).trim() !== '';
}

const serviceInfoItems = computed<DetailInfoItem[]>(() => {
  const items: DetailInfoItem[] = [
    { key: 'serviceMode', label: '维修方式', value: valueByKeys('serviceModeLabel', 'serviceMode') },
    { key: 'senderName', label: '寄件人', value: valueByKeys('senderName') },
    { key: 'senderMobile', label: '寄件手机号', value: valueByKeys('senderMobile') },
    { key: 'senderAddress', label: '寄件地址', value: valueByKeys('senderAddress'), span: 2 }
  ];
  const rm = (detail.value as Record<string, unknown>)?.returnMethod;
  if (serviceInfoFieldHasValue(rm)) items.push({ key: 'returnMethod', label: '返回方式', value: rm });
  const re = (detail.value as Record<string, unknown>)?.returnExpressNo;
  if (serviceInfoFieldHasValue(re)) items.push({ key: 'returnExpressNo', label: '回寄单号', value: re });
  const ct = (detail.value as Record<string, unknown>)?.completedTime;
  if (serviceInfoFieldHasValue(ct)) items.push({ key: 'completedTime', label: '完成时间', value: ct });
  const clt = (detail.value as Record<string, unknown>)?.closedTime;
  if (serviceInfoFieldHasValue(clt)) items.push({ key: 'closedTime', label: '关闭时间', value: clt });
  const cr = (detail.value as Record<string, unknown>)?.closeReason;
  if (serviceInfoFieldHasValue(cr)) items.push({ key: 'closeReason', label: '关闭原因', value: cr, span: 2 });
  return items;
});

const faultImageFiles = computed(() => asFileList(detail.value.faultImageFiles));
const faultVideoFiles = computed(() => asFileList(detail.value.faultVideoFiles));
const hasFaultImageOrVideo = computed(() => faultImageFiles.value.length > 0 || faultVideoFiles.value.length > 0);
const faultVoiceFiles = computed(() => asFileList(detail.value.faultVoiceFiles));
const senderVoucherFiles = computed(() => asFileList(detail.value.senderVoucherFiles));
const returnVoucherFiles = computed(() => asFileList(detail.value.returnVoucherFiles));

const faultInfoItems = computed<DetailInfoItem[]>(() => {
  const faultDesc = valueByKeys('faultDesc');
  const firstRepairDesc = firstRepairConfirmedFaultDesc.value;
  const items: DetailInfoItem[] = [
    { key: 'customerName', label: '客户姓名', value: valueByKeys('customerName') },
    { key: 'customerMobile', label: '客户手机号', value: valueByKeys('customerMobile') },
    { key: 'faultDesc', label: '故障描述', value: faultDesc, span: 2 }
  ];
  if (containsOtherFault(faultDesc)) {
    items.push({ key: 'faultRemark', label: '故障说明备注', value: valueByKeys('faultRemark'), span: 2 });
  }
  if (showCompletedRepairInfo.value) {
    items.push({ key: 'firstRepairConfirmedFaultDesc', label: '首次维修确认故障', value: firstRepairDesc, span: 2 });
    if (containsOtherFault(firstRepairDesc)) {
      items.push({ key: 'firstRepairConfirmedFaultRemark', label: '其它故障说明', value: firstRepairConfirmedFaultRemark.value, span: 2 });
    }
  }
  return items;
});

const acceptorInfoItems = computed<DetailInfoItem[]>(() => [
  { key: 'currentAcceptCompanyName', label: '受理方', value: valueByKeys('currentAcceptCompanyName') },
  { key: 'currentAcceptCompanyPhone', label: '网点电话', value: valueByKeys('currentAcceptCompanyPhone') },
  { key: 'assignedUserName', label: '当前维修员', value: valueByKeys('assignedUserName') },
  { key: 'transferCount', label: '转单次数', value: valueByKeys('transferCount') || '0' }
]);

const evaluationInfoItems = computed(() => {
  const evaluation = detail.value.evaluation as Record<string, unknown> | undefined;
  if (!evaluation) return [];
  return [
    { key: 'timelinessScore', label: '服务时效', value: evaluation.timelinessScore },
    { key: 'qualityScore', label: '维修质量', value: evaluation.qualityScore },
    { key: 'satisfactionScore', label: '服务满意度', value: evaluation.satisfactionScore },
    { key: 'createTime', label: '评价时间', value: evaluation.createTime },
    { key: 'tags', label: '标签', value: evaluation.tags, span: 2 },
    { key: 'content', label: '评价内容', value: evaluation.content, span: 2 }
  ];
});

function asFileList(value: unknown) {
  return Array.isArray(value) ? (value as Array<Record<string, unknown>>) : [];
}

function fileDisplayName(file: Record<string, unknown>) {
  return textValue(file.fileName || file.originName || file.name || file.originalFilename, '附件');
}

function fileOpenUrl(file: Record<string, unknown>) {
  const raw = file.url || file.fileUrl || file.previewUrl || file.downloadUrl || file.path;
  return normalizeText(raw);
}

function fileMimeType(file: Record<string, unknown>) {
  return normalizeText(file.contentType || file.mimeType || file.fileType).toLowerCase();
}

function fileExt(file: Record<string, unknown>) {
  const name = normalizeText(file.fileName || file.originName || file.name || file.originalFilename).toLowerCase();
  const url = fileOpenUrl(file).toLowerCase().split('?')[0];
  const fromName = name.includes('.') ? name.split('.').pop() || '' : '';
  const fromUrl = url.includes('.') ? url.split('.').pop() || '' : '';
  return fromName || fromUrl;
}

function isImageFile(file: Record<string, unknown>) {
  const mime = fileMimeType(file);
  if (mime.startsWith('image/')) return true;
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(fileExt(file));
}

function isVideoFile(file: Record<string, unknown>) {
  const mime = fileMimeType(file);
  if (mime.startsWith('video/')) return true;
  return ['mp4', 'webm', 'ogg', 'mov', 'm4v', 'avi'].includes(fileExt(file));
}

function openFilePreview(file: Record<string, unknown>) {
  const url = fileOpenUrl(file);
  if (!url) {
    window.$message?.warning('附件地址不存在');
    return;
  }
  if (isImageFile(file)) {
    previewType.value = 'image';
  } else if (isVideoFile(file)) {
    previewType.value = 'video';
  } else {
    window.open(url, '_blank');
    return;
  }
  previewUrl.value = url;
  previewTitle.value = fileDisplayName(file);
  previewOpen.value = true;
}

const participants = computed(() => (Array.isArray(detail.value.participants) ? detail.value.participants : []));
const flows = computed(() => (Array.isArray(detail.value.flows) ? detail.value.flows : []));
const quotes = computed(() => (Array.isArray(detail.value.quotes) ? detail.value.quotes : []));
const repairs = computed(() => (Array.isArray(detail.value.repairs) ? detail.value.repairs : []));
const notifyEvents = computed(() => (Array.isArray(detail.value.notifyEvents) ? detail.value.notifyEvents : []));
const faultPointHistoryRows = computed(() => {
  const rows: Array<Record<string, unknown>> = [];
  repairs.value.forEach((repair, repairIndex) => {
    const repairObj = repair as Record<string, unknown>;
    const faults = Array.isArray(repairObj.faults) ? (repairObj.faults as Array<Record<string, unknown>>) : [];
    faults.forEach((fault, faultIndex) => {
      rows.push({
        key: `${repairObj.id || repairIndex}-${fault.id || faultIndex}`,
        registerStageLabel: repairObj.registerStageLabel,
        repairCreateTime: repairObj.createTime,
        faultDesc: fault.faultDesc,
        faultRemark: fault.faultRemark,
        repairDesc: fault.repairDesc,
        otherDesc: fault.otherDesc,
        partList: fault.partList,
        createdByName: fault.createdByName,
        createTime: fault.createTime || repairObj.createTime
      });
    });
  });
  return rows;
});

function repairAttachmentFileLists(repair: Record<string, unknown>) {
  return [
    { key: 'faultOld', label: '故障处旧图片', files: asFileList(repair.faultOldImageFiles ?? repair.faultOldImageFileList) },
    {
      key: 'faultNew',
      label: '故障处新图片',
      files: asFileList(repair.faultImageFiles ?? repair.faultNewImageFiles ?? repair.faultNewImageFileList)
    },
    { key: 'machine', label: '机器正面照片', files: asFileList(repair.machineImageFiles ?? repair.machineImageFileList) },
    {
      key: 'barcode',
      label: '机器条码照片',
      files: asFileList(repair.machineBarcodeImageFiles ?? repair.machineBarcodeImageFileList)
    },
    { key: 'other', label: '其它图片', files: asFileList(repair.otherImageFiles ?? repair.otherImageFileList) }
  ].filter(g => g.files.length > 0);
}

const visibleRepairs = computed(() =>
  repairs.value.filter(item => {
    if (!item || typeof item !== 'object') return false;
    const repair = item as Record<string, unknown>;
    const fileKeys = [
      'faultOldImageFiles',
      'faultImageFiles',
      'faultNewImageFiles',
      'machineImageFiles',
      'machineBarcodeImageFiles',
      'otherImageFiles',
      'faultOldImageFileList',
      'faultNewImageFileList',
      'machineImageFileList',
      'machineBarcodeImageFileList',
      'otherImageFileList'
    ];
    const hasRepairFiles = fileKeys.some(key => asFileList(repair[key]).length > 0);
    return (
      [
        repair.registerStageLabel,
        repair.createTime,
        repair.faultJudge,
        repair.quoteAmount,
        repair.quoteDesc,
        repair.companyName,
        repair.repairUserName,
        repair.isFinished,
        repair.finishedTime
      ].some(value => normalizeText(value) !== '') || hasRepairFiles
    );
  })
);

const participantsColumns = [
  { title: '公司', dataIndex: 'companyName', key: 'companyName' },
  { title: '主体类型', dataIndex: 'subjectType', key: 'subjectType' },
  { title: '参与类型', dataIndex: 'participateType', key: 'participateType' },
  { title: '当前处理方', dataIndex: 'isCurrentHandler', key: 'isCurrentHandler' },
  { title: '首次参与时间', dataIndex: 'firstParticipateTime', key: 'firstParticipateTime' },
  { title: '最后参与时间', dataIndex: 'lastParticipateTime', key: 'lastParticipateTime' }
];

const flowsColumns = [
  { title: '动作', dataIndex: 'actionName', key: 'actionName' },
  { title: '前状态', dataIndex: 'beforeStatusName', key: 'beforeStatusName' },
  { title: '后状态', dataIndex: 'afterStatusName', key: 'afterStatusName' },
  { title: '来源公司', dataIndex: 'fromCompanyName', key: 'fromCompanyName' },
  { title: '目标公司', dataIndex: 'toCompanyName', key: 'toCompanyName' },
  { title: '操作公司', dataIndex: 'operatorCompanyName', key: 'operatorCompanyName' },
  { title: '操作人', dataIndex: 'operatorUserName', key: 'operatorUserName' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' }
];

const quotesColumns = [
  { title: '报价公司', dataIndex: 'companyName', key: 'companyName' },
  { title: '报价人', dataIndex: 'quotedByName', key: 'quotedByName' },
  { title: '故障判断', dataIndex: 'faultJudge', key: 'faultJudge' },
  { title: '报价金额', dataIndex: 'quoteAmount', key: 'quoteAmount' },
  { title: '报价说明', dataIndex: 'quoteDesc', key: 'quoteDesc' },
  { title: '当前有效', dataIndex: 'isCurrentValid', key: 'isCurrentValid' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' }
];

const notifyColumns = [
  { title: '归属公司', dataIndex: 'companyName', key: 'companyName' },
  { title: '事件类型', dataIndex: 'eventType', key: 'eventType' },
  { title: '触发节点', dataIndex: 'triggerNode', key: 'triggerNode' },
  { title: '接收对象', dataIndex: 'receiverType', key: 'receiverType' },
  { title: '发送状态', dataIndex: 'sendStatus', key: 'sendStatus' },
  { title: '标题快照', dataIndex: 'titleSnapshot', key: 'titleSnapshot' },
  { title: '内容快照', dataIndex: 'contentSnapshot', key: 'contentSnapshot' },
  { title: '失败原因', dataIndex: 'failReason', key: 'failReason' },
  { title: '发送时间', dataIndex: 'sendTime', key: 'sendTime' }
];

const faultPointColumns = [
  { title: '登记阶段', dataIndex: 'registerStageLabel', key: 'registerStageLabel' },
  { title: '故障点', dataIndex: 'faultDesc', key: 'faultDesc' },
  { title: '其它故障说明', dataIndex: 'faultRemark', key: 'faultRemark' },
  { title: '维修说明', dataIndex: 'repairDesc', key: 'repairDesc' },
  { title: '其他维修说明', dataIndex: 'otherDesc', key: 'otherDesc' },
  { title: '登记人', dataIndex: 'createdByName', key: 'createdByName' },
  { title: '登记时间', dataIndex: 'createTime', key: 'createTime' }
];

function textValue(value: unknown, empty = '-') {
  const text = String(value ?? '').trim();
  return text || empty;
}

function successMessageFromResult(result: unknown, fallback: string) {
  if (!result) return fallback;
  if (typeof result === 'string') {
    const t = result.trim();
    return t || fallback;
  }
  if (typeof result === 'object') {
    const o = result as Record<string, unknown>;
    const direct = [o.msg, o.message, o.successMessage].find(v => typeof v === 'string' && String(v).trim());
    if (typeof direct === 'string') return direct.trim() || fallback;
    if (o.response && typeof o.response === 'object') {
      const response = o.response as Record<string, unknown>;
      if (response.data && typeof response.data === 'object') {
        const responseData = response.data as Record<string, unknown>;
        const responseMsg = [responseData.msg, responseData.message].find(v => typeof v === 'string' && String(v).trim());
        if (typeof responseMsg === 'string') return responseMsg.trim() || fallback;
      }
    }
    if (o.data && typeof o.data === 'object') {
      const d = o.data as Record<string, unknown>;
      const nested = [d.msg, d.message].find(v => typeof v === 'string' && String(v).trim());
      if (typeof nested === 'string') return nested.trim() || fallback;
    }
  }
  return fallback;
}

function normalizeText(value: unknown) {
  return value === null || value === undefined ? '' : String(value).trim();
}

function normalizeFaultItems(items: unknown[]) {
  return [...new Set((items || []).map(item => normalizeText(item)).filter(Boolean))];
}

function normalizeRepairItems(items: unknown[]) {
  return (items || []).map(item => normalizeText(item)).filter(Boolean);
}

function splitFaultDescSelections(rawFaultDesc: string) {
  if (!rawFaultDesc) return [];
  return rawFaultDesc
    .split(/[；;]+/)
    .map(item => normalizeText(item))
    .filter(Boolean);
}

async function loadRepairFaultConfig() {
  if (!id.value) return;
  const { data } = await listRepairFaultOptions(id.value);
  const rows = pickRows(data) as Array<Record<string, unknown>>;
  actionRepairFaultOptions.value = rows
    .map(item => ({
      faultDesc: normalizeText(item.faultDesc),
      repairOptions: Array.isArray(item.repairOptions)
        ? (item.repairOptions as unknown[]).map(opt => normalizeText(opt)).filter(Boolean)
        : []
    }))
    .filter(item => item.faultDesc);
}

function handleRepairFaultItemsChange() {
  repairForm.faultItems = normalizeFaultItems(repairForm.faultItems);
  if (!showRepairFaultRemarkInput.value) repairForm.faultRemark = '';
  repairForm.repairItems = [];
  repairForm.otherDesc = '';
  if (hasRepairFaultConfig.value) repairForm.repairDesc = '';
}

function handleRepairItemsChange() {
  repairForm.repairItems = normalizeRepairItems(repairForm.repairItems);
  if (!isOtherRepairSelected.value) repairForm.otherDesc = '';
  if (hasRepairFaultConfig.value) repairForm.repairDesc = '';
}

function handleReviewItemsChange() {
  reviewForm.repairItems = normalizeRepairItems(reviewForm.repairItems);
  if (!isOtherReviewSelected.value) reviewForm.otherDesc = '';
  if (hasRepairFaultConfig.value) reviewForm.repairDesc = '';
}

async function openAssign() {
  if (!id.value) return;
  assignUserId.value = undefined;
  const { data } = await listAssignUserOptions(id.value);
  const rows = pickRows(data) as Record<string, unknown>[];
  const selfId = Number(authStore.userInfo.userId);
  assignOptions.value = rows
    .map(r => {
      const uid = Number(r.userId ?? r.id ?? r.assignedUserId);
      if (!Number.isFinite(uid)) return null;
      const realName = normalizeText(r.realName);
      const phone = normalizeText(r.phone);
      const base =
        realName ||
        phone ||
        (Number.isFinite(uid) ? `用户${uid}` : '') ||
        normalizeText(r.userName) ||
        normalizeText(r.label) ||
        normalizeText(r.name) ||
        String(uid);
      const label =
        Number.isFinite(selfId) && selfId > 0 && uid === selfId ? `${base}（本人）` : base;
      return { label, value: uid };
    })
    .filter(Boolean) as Array<{ label: string; value: number }>;
  assignOpen.value = true;
}

async function submitAssign() {
  if (!id.value || assignUserId.value === undefined || assignUserId.value === null) {
    window.$message?.warning('请选择维修员');
    return Promise.reject(new Error('validation'));
  }
  assignSubmitting.value = true;
  try {
    const chosenId = assignUserId.value;
    const assignResult = await assignWorkOrder({ workOrderId: id.value, assignedUserId: chosenId });
    assignOpen.value = false;
    const selfId = Number(authStore.userInfo.userId);
    if (Number.isFinite(selfId) && selfId > 0 && chosenId === selfId) {
      window.$message?.success(successMessageFromResult(assignResult, '已派单给自己，可在「待接单」中接单'));
    } else {
      window.$message?.success(successMessageFromResult(assignResult, '派单成功'));
    }
    await loadDetail();
    emit('success');
  } finally {
    assignSubmitting.value = false;
  }
  return undefined;
}

function openAccept() {
  techAcceptForm.faultJudge = '';
  techAcceptForm.quoteAmount = undefined;
  techAcceptForm.quoteDesc = '';
  techAcceptOpen.value = true;
}

async function submitAccept() {
  if (!id.value) return undefined;
  if (!techAcceptForm.faultJudge) {
    window.$message?.warning('请输入故障判定');
    return Promise.reject(new Error('validation'));
  }
  techAcceptSubmitting.value = true;
  try {
    const payload: Record<string, unknown> = { workOrderId: id.value, faultJudge: techAcceptForm.faultJudge };
    if (techAcceptForm.faultJudge === faultJudgeHasFault) {
      payload.quoteAmount = techAcceptForm.quoteAmount;
      payload.quoteDesc = techAcceptForm.quoteDesc || undefined;
    }
    if (techAcceptForm.faultJudge === faultJudgeNoFault) {
      pendingTechAcceptPayload.value = payload;
      techAcceptOpen.value = false;
      openClose({ fromNoFaultTechAccept: true });
      return undefined;
    }
    await techAcceptWorkOrder(payload);
    techAcceptOpen.value = false;
    await loadDetail();
    emit('success');
  } finally {
    techAcceptSubmitting.value = false;
  }
  return undefined;
}

function shouldSupplementRepairProductModel(action: WorkOrderListActionCode) {
  const brandType = normalizeText(detail.value.brandType);
  const productModel = normalizeText(detail.value.productModel);
  return (action === 'REPAIR_FINISH' || action === 'REVIEW') && brandType === 'JASIC' && !productModel;
}

async function loadRepairProductModelOptionList(keyword = '') {
  if (!id.value) {
    repairProductModelOptions.value = [];
    return [];
  }
  repairProductModelOptionsLoading.value = true;
  try {
    const { data } = await listRepairProductModelOptions(id.value, { keyword: keyword || undefined });
    const rows = pickRows(data);
    const list = rows
      .map(item => {
        if (typeof item === 'string') return normalizeText(item);
        if (item && typeof item === 'object') {
          return normalizeText(
            (item as Record<string, unknown>).productModel || (item as Record<string, unknown>).label
          );
        }
        return '';
      })
      .filter(Boolean);
    repairProductModelOptions.value = [...new Set(list)];
    return repairProductModelOptions.value;
  } finally {
    repairProductModelOptionsLoading.value = false;
  }
}

async function prepareRepairProductModelDialog(action: WorkOrderListActionCode) {
  repairProductModelPendingAction.value = action;
  repairProductModel.value = '';
  repairProductModelOpen.value = true;
  const options = await loadRepairProductModelOptionList('');
  if (!options.length) {
    window.$message?.error('当前归属总部未配置启用机型，请先维护故障与维修配置');
    repairProductModelOpen.value = false;
    repairProductModelPendingAction.value = '';
  }
}

async function submitRepairProductModel() {
  if (!id.value || !normalizeText(repairProductModel.value)) {
    window.$message?.warning('请选择机型');
    return;
  }
  repairProductModelSubmitting.value = true;
  try {
    await updateRepairProductModel({ workOrderId: id.value, productModel: normalizeText(repairProductModel.value) });
    const nextAction = repairProductModelPendingAction.value;
    repairProductModelOpen.value = false;
    repairProductModelPendingAction.value = '';
    await loadDetail();
    if (nextAction === 'REPAIR_FINISH') openRepair();
    if (nextAction === 'REVIEW') openReview();
  } finally {
    repairProductModelSubmitting.value = false;
  }
}

function validatePartList(partList: Array<{ partName: string; partQty?: number }>) {
  let hasValidPart = false;
  for (const item of partList || []) {
    const partName = String(item?.partName || '').trim();
    const partQty = item?.partQty;
    if (!partName && (partQty === undefined || partQty === null || partQty === ('' as unknown as number))) {
      // skip fully empty row
    } else if (!partName) {
      window.$message?.warning('请输入配件名称');
      return false;
    } else if (!partQty || Number(partQty) <= 0) {
      window.$message?.warning('请输入正确的配件数量');
      return false;
    } else {
      hasValidPart = true;
    }
  }
  if (!hasValidPart) {
    window.$message?.warning('请至少填写一条配件明细');
    return false;
  }
  return true;
}

async function openTransfer() {
  if (!id.value) return;
  transferCompanyId.value = undefined;
  transferRemark.value = '';
  const { data } = await listTransferTargetOptions(id.value);
  const rows = pickRows(data) as Record<string, unknown>[];
  transferOptions.value = rows
    .map(r => {
      const cid = Number(r.companyId ?? r.id ?? r.targetCompanyId);
      const label = String(r.companyName ?? r.label ?? r.name ?? cid);
      return Number.isFinite(cid) ? { label, value: cid } : null;
    })
    .filter(Boolean) as Array<{ label: string; value: number }>;
  transferOpen.value = true;
}

async function submitTransfer() {
  if (!id.value || transferCompanyId.value === undefined || transferCompanyId.value === null) {
    window.$message?.warning('请选择目标公司');
    return Promise.reject(new Error('validation'));
  }
  transferSubmitting.value = true;
  try {
    await transferWorkOrder({
      workOrderId: id.value,
      targetCompanyId: transferCompanyId.value,
      remark: transferRemark.value || undefined
    });
    transferOpen.value = false;
    await loadDetail();
    emit('success');
  } finally {
    transferSubmitting.value = false;
  }
  return undefined;
}

function openRepair() {
  const qa = detail.value.quoteAmount;
  repairForm.quoteAmount = qa !== undefined && qa !== null && qa !== '' ? Number(qa) : undefined;
  repairForm.quoteDesc = '';
  repairForm.repairDesc = '';
  repairForm.faultItems = [];
  repairForm.faultRemark = '';
  repairForm.repairItems = [];
  repairForm.otherDesc = '';
  repairForm.partList = [{ partName: '', partQty: 1 }];
  repairForm.faultOldImageFileIds = [];
  repairForm.faultImageFileIds = [];
  repairForm.machineImageFileIds = [];
  repairForm.machineBarcodeImageFileIds = [];
  repairForm.otherImageFileIds = [];
  loadRepairFaultConfig();
  repairOpen.value = true;
}

// eslint-disable-next-line complexity
async function submitRepair() {
  if (!id.value) return undefined;
  if (repairForm.quoteAmount !== undefined && Number(repairForm.quoteAmount) < 0) {
    window.$message?.warning('报价金额不能小于 0');
    return Promise.reject(new Error('validation'));
  }
  if (hasRepairFaultConfig.value) {
    if (!normalizeFaultItems(repairForm.faultItems).length) {
      window.$message?.warning('请选择维修确认故障');
      return Promise.reject(new Error('validation'));
    }
    if (showRepairFaultRemarkInput.value && !normalizeText(repairForm.faultRemark)) {
      window.$message?.warning('选择其它故障时，必须填写其它故障说明');
      return Promise.reject(new Error('validation'));
    }
    if (!normalizeRepairItems(repairForm.repairItems).length) {
      window.$message?.warning('请选择维修说明');
      return Promise.reject(new Error('validation'));
    }
    if (isOtherRepairSelected.value && !normalizeText(repairForm.otherDesc)) {
      window.$message?.warning('选择其它维修说明后，必须填写其他维修说明');
      return Promise.reject(new Error('validation'));
    }
  } else if (!normalizeText(repairForm.repairDesc)) {
    window.$message?.warning('请输入维修说明');
    return Promise.reject(new Error('validation'));
  }
  if (!validatePartList(repairForm.partList)) {
    return Promise.reject(new Error('validation'));
  }
  repairSubmitting.value = true;
  try {
    await repairWorkOrder({
      workOrderId: id.value,
      quoteAmount: repairForm.quoteAmount,
      quoteDesc: repairForm.quoteDesc || undefined,
      faultItems: normalizeFaultItems(repairForm.faultItems),
      faultRemark: normalizeText(repairForm.faultRemark) || undefined,
      repairDesc: hasRepairFaultConfig.value ? undefined : repairForm.repairDesc || undefined,
      repairItems: normalizeRepairItems(repairForm.repairItems),
      otherDesc: normalizeText(repairForm.otherDesc) || undefined,
      partList: (repairForm.partList || []).filter(item => item.partName && item.partQty),
      faultOldImageFileIds: repairForm.faultOldImageFileIds.length ? repairForm.faultOldImageFileIds : undefined,
      faultNewImageFileIds: repairForm.faultImageFileIds.length ? repairForm.faultImageFileIds : undefined,
      machineImageFileIds: repairForm.machineImageFileIds.length ? repairForm.machineImageFileIds : undefined,
      machineBarcodeImageFileIds: repairForm.machineBarcodeImageFileIds.length
        ? repairForm.machineBarcodeImageFileIds
        : undefined,
      otherImageFileIds: repairForm.otherImageFileIds.length ? repairForm.otherImageFileIds : undefined
    });
    repairOpen.value = false;
    await loadDetail();
    emit('success');
  } finally {
    repairSubmitting.value = false;
  }
  return undefined;
}

function openReview() {
  reviewForm.repairDesc = '';
  reviewForm.repairItems = [];
  reviewForm.otherDesc = '';
  reviewForm.partList = [{ partName: '', partQty: 1 }];
  reviewForm.faultOldImageFileIds = [];
  reviewForm.faultImageFileIds = [];
  reviewForm.machineImageFileIds = [];
  reviewForm.machineBarcodeImageFileIds = [];
  reviewForm.otherImageFileIds = [];
  loadRepairFaultConfig();
  reviewOpen.value = true;
}

// eslint-disable-next-line complexity
async function submitReview() {
  if (!id.value) return undefined;
  if (hasRepairFaultConfig.value) {
    if (!reviewFaultItems.value.length) {
      window.$message?.warning('首次维修登记未记录故障描述，无法提交复检登记');
      return Promise.reject(new Error('validation'));
    }
    if (!normalizeRepairItems(reviewForm.repairItems).length) {
      window.$message?.warning('请选择维修说明');
      return Promise.reject(new Error('validation'));
    }
    if (isOtherReviewSelected.value && !normalizeText(reviewForm.otherDesc)) {
      window.$message?.warning('选择其它维修说明后，必须填写其他维修说明');
      return Promise.reject(new Error('validation'));
    }
  } else if (!normalizeText(reviewForm.repairDesc)) {
    window.$message?.warning('请输入维修说明');
    return Promise.reject(new Error('validation'));
  }
  if (!validatePartList(reviewForm.partList)) {
    return Promise.reject(new Error('validation'));
  }
  reviewSubmitting.value = true;
  try {
    const reviewResult = await reviewWorkOrder({
      workOrderId: id.value,
      repairDesc: hasRepairFaultConfig.value ? undefined : reviewForm.repairDesc || undefined,
      repairItems: normalizeRepairItems(reviewForm.repairItems),
      otherDesc: normalizeText(reviewForm.otherDesc) || undefined,
      partList: (reviewForm.partList || []).filter(item => item.partName && item.partQty),
      faultOldImageFileIds: reviewForm.faultOldImageFileIds.length ? reviewForm.faultOldImageFileIds : undefined,
      faultNewImageFileIds: reviewForm.faultImageFileIds.length ? reviewForm.faultImageFileIds : undefined,
      machineImageFileIds: reviewForm.machineImageFileIds.length ? reviewForm.machineImageFileIds : undefined,
      machineBarcodeImageFileIds: reviewForm.machineBarcodeImageFileIds.length
        ? reviewForm.machineBarcodeImageFileIds
        : undefined,
      otherImageFileIds: reviewForm.otherImageFileIds.length ? reviewForm.otherImageFileIds : undefined
    });
    window.$message?.success(successMessageFromResult(reviewResult, '复检登记提交成功'));
    reviewOpen.value = false;
    await loadDetail();
    emit('success');
  } finally {
    reviewSubmitting.value = false;
  }
  return undefined;
}

function openMail() {
  mailForm.senderVoucherFileIds = [];
  mailOpen.value = true;
}

async function submitMail() {
  if (!id.value) return undefined;
  if (!mailForm.senderVoucherFileIds.length) {
    window.$message?.warning('请上传寄件凭证');
    return Promise.reject(new Error('validation'));
  }
  mailSubmitting.value = true;
  try {
    await updateWorkOrderSendExpress({
      workOrderId: id.value,
      senderVoucherFileIds: mailForm.senderVoucherFileIds
    });
    mailOpen.value = false;
    await loadDetail();
    emit('success');
  } finally {
    mailSubmitting.value = false;
  }
  return undefined;
}

function openClose(options: { fromNoFaultTechAccept?: boolean } = {}) {
  if (!options.fromNoFaultTechAccept) {
    pendingTechAcceptPayload.value = null;
  }
  closeForm.returnMethod = '自提';
  closeForm.closeReason = '';
  closeForm.returnVoucherFileIds = [];
  closeOpen.value = true;
}

async function submitClose() {
  if (!id.value || !closeForm.closeReason.trim()) {
    window.$message?.warning('请填写关闭原因');
    return Promise.reject(new Error('validation'));
  }
  if (!closeForm.returnMethod) {
    window.$message?.warning('请选择返回方式');
    return Promise.reject(new Error('validation'));
  }
  if (closeForm.returnMethod === '回寄' && !closeForm.returnVoucherFileIds.length) {
    window.$message?.warning('请上传回寄凭证');
    return Promise.reject(new Error('validation'));
  }
  closeSubmitting.value = true;
  try {
    let closeResult: unknown;
    const closePayload = {
      workOrderId: id.value,
      returnMethod: closeForm.returnMethod,
      closeReason: closeForm.closeReason.trim(),
      returnVoucherFileIds: closeForm.returnVoucherFileIds.length ? closeForm.returnVoucherFileIds : undefined
    };
    if (pendingTechAcceptPayload.value) {
      closeResult = await techAcceptWorkOrder({
        ...pendingTechAcceptPayload.value,
        returnMethod: closePayload.returnMethod,
        returnVoucherFileIds: closePayload.returnVoucherFileIds,
        closeReason: closePayload.closeReason
      });
    } else {
      closeResult = await closeWorkOrder(closePayload);
    }
    window.$message?.success(successMessageFromResult(closeResult, '关闭工单提交成功'));
    pendingTechAcceptPayload.value = null;
    closeOpen.value = false;
    await loadDetail();
    emit('success');
  } finally {
    closeSubmitting.value = false;
  }
  return undefined;
}

function cancelCloseDialog() {
  pendingTechAcceptPayload.value = null;
  closeOpen.value = false;
}

async function runUploadToIds(bucket: number[], opt: UploadRequestOption) {
  try {
    const raw = opt.file as File;
    const { data, error } = await uploadSystemFile(raw);
    if (error) {
      opt.onError?.(new Error('upload'));
      return;
    }
    const fid = pickUploadedFileId(data);
    if (typeof fid === 'number') bucket.push(fid);
    opt.onSuccess?.(data as never, opt.file as never);
  } catch {
    opt.onError?.(new Error('upload'));
  }
}

async function customRequestRepairImage(opt: UploadRequestOption) {
  await runUploadToIds(repairForm.faultImageFileIds, opt);
}

async function customRequestReviewImage(opt: UploadRequestOption) {
  await runUploadToIds(reviewForm.faultImageFileIds, opt);
}

async function customRequestCloseVoucher(opt: UploadRequestOption) {
  await runUploadToIds(closeForm.returnVoucherFileIds, opt);
}

async function customRequestMailVoucher(opt: UploadRequestOption) {
  try {
    const raw = opt.file as File;
    const { data, error } = await uploadSystemFile(raw);
    if (error) {
      opt.onError?.(new Error('upload'));
      return;
    }
    const fid = pickUploadedFileId(data);
    if (typeof fid === 'number') mailForm.senderVoucherFileIds = [fid];
    opt.onSuccess?.(data as never, opt.file as never);
  } catch {
    opt.onError?.(new Error('upload'));
  }
}

async function customRequestRepairOldImage(opt: UploadRequestOption) {
  await runUploadToIds(repairForm.faultOldImageFileIds, opt);
}

async function customRequestRepairMachineImage(opt: UploadRequestOption) {
  await runUploadToIds(repairForm.machineImageFileIds, opt);
}

async function customRequestRepairMachineBarcodeImage(opt: UploadRequestOption) {
  await runUploadToIds(repairForm.machineBarcodeImageFileIds, opt);
}

async function customRequestRepairOtherImage(opt: UploadRequestOption) {
  await runUploadToIds(repairForm.otherImageFileIds, opt);
}

async function customRequestReviewOldImage(opt: UploadRequestOption) {
  await runUploadToIds(reviewForm.faultOldImageFileIds, opt);
}

async function customRequestReviewMachineImage(opt: UploadRequestOption) {
  await runUploadToIds(reviewForm.machineImageFileIds, opt);
}

async function customRequestReviewMachineBarcodeImage(opt: UploadRequestOption) {
  await runUploadToIds(reviewForm.machineBarcodeImageFileIds, opt);
}

async function customRequestReviewOtherImage(opt: UploadRequestOption) {
  await runUploadToIds(reviewForm.otherImageFileIds, opt);
}

function addPartRow(target: Array<{ partName: string; partQty?: number }>) {
  target.push({ partName: '', partQty: 1 });
}

function removePartRow(target: Array<{ partName: string; partQty?: number }>, index: number) {
  if (target.length <= 1) return;
  target.splice(index, 1);
}

/** 列表行点击「派单」等与 jasic-ui 一致：先拉详情再打开对应子流程 */
async function openActionFromList(action: string) {
  if (!id.value) return;
  await loadDetail();
  if (!detail.value || !Object.keys(detail.value).length) {
    window.$message?.error('加载工单失败');
    return;
  }
  const code = action as WorkOrderListActionCode;
  if (shouldSupplementRepairProductModel(code)) {
    await prepareRepairProductModelDialog(code);
    return;
  }
  switch (code) {
    case 'ASSIGN':
      await openAssign();
      break;
    case 'TECH_ACCEPT':
      openAccept();
      break;
    case 'TRANSFER':
      await openTransfer();
      break;
    case 'REPAIR_FINISH':
      openRepair();
      break;
    case 'REVIEW':
      openReview();
      break;
    case 'UPLOAD_SEND_EXPRESS':
      openMail();
      break;
    case 'CLOSE':
      openClose();
      break;
    default:
      break;
  }
}

defineExpose({
  openActionFromList
});
</script>

<template>
  <ADrawer
    :open="open"
    :width="1400"
    title="工单详情"
    destroy-on-close
    @update:open="(v: boolean) => emit('update:open', v)"
  >
    <ASpin :spinning="loading">
      <!-- 块级包裹：避免 ASpace(inline-flex) 上 margin 不生效 / 外边距折叠 -->
      <div class="w-full pb-16px">
        <ASpace wrap>
          <AButton v-if="showAssign" type="primary" size="small" @click="openAssign">派单</AButton>
          <AButton v-if="showAccept" type="primary" size="small" @click="openAccept">维修员接单</AButton>
          <AButton v-if="showTransfer" size="small" @click="openTransfer">转单</AButton>
          <AButton v-if="showRepair" size="small" @click="openRepair">维修登记</AButton>
          <AButton v-if="showReview" size="small" @click="openReview">复检登记</AButton>
          <AButton v-if="showMail" size="small" @click="openMail">上传寄件单号</AButton>
          <AButton v-if="showClose" danger size="small" @click="() => openClose()">关闭工单</AButton>
        </ASpace>
      </div>

      <div class="mb-8px text-14px text-gray-700">工单信息</div>
      <ADescriptions bordered size="small" :column="2" class="mb-16px">
        <ADescriptionsItem v-for="it in workOrderInfoItems" :key="it.key" :label="it.label" :span="it.span || 1">
          {{ textValue(it.value) }}
        </ADescriptionsItem>
      </ADescriptions>

      <div class="mb-8px text-14px text-gray-700">商品信息</div>
      <ADescriptions bordered size="small" :column="2" class="mb-16px">
        <ADescriptionsItem v-for="it in productInfoItems" :key="it.key" :label="it.label" :span="it.span || 1">
          {{ textValue(it.value) }}
        </ADescriptionsItem>
      </ADescriptions>

      <div class="mb-8px text-14px text-gray-700">服务信息</div>
      <ADescriptions bordered size="small" :column="2" class="mb-16px">
        <ADescriptionsItem v-for="it in serviceInfoItems" :key="it.key" :label="it.label" :span="it.span || 1">
          {{ textValue(it.value) }}
        </ADescriptionsItem>
        <ADescriptionsItem v-if="senderVoucherFiles.length" label="寄件凭证" :span="2">
          <div class="flex flex-wrap gap-8px">
            <div
              v-for="(file, fileIndex) in senderVoucherFiles"
              :key="`sender-${String(file.fileId || file.id || fileIndex)}`"
              class="h-100px w-100px overflow-hidden border border-gray-200 rounded"
            >
              <img
                v-if="isImageFile(file) && fileOpenUrl(file)"
                :src="fileOpenUrl(file)"
                :alt="fileDisplayName(file)"
                class="h-full w-full cursor-pointer object-cover"
                @click="openFilePreview(file)"
              />
              <video
                v-else-if="isVideoFile(file) && fileOpenUrl(file)"
                :src="fileOpenUrl(file)"
                muted
                playsinline
                preload="metadata"
                class="h-full w-full cursor-pointer bg-black object-cover"
                @click="openFilePreview(file)"
              />
              <div v-else class="h-full w-full flex items-center justify-center bg-gray-50 p-4px text-center">
                <AButton type="link" class="max-w-full text-ellipsis whitespace-nowrap p-0" @click="openFilePreview(file)">
                  {{ fileDisplayName(file) }}
                </AButton>
              </div>
            </div>
          </div>
        </ADescriptionsItem>
        <ADescriptionsItem v-if="returnVoucherFiles.length" label="回寄凭证" :span="2">
          <div class="flex flex-wrap gap-8px">
            <div
              v-for="(file, fileIndex) in returnVoucherFiles"
              :key="`return-${String(file.fileId || file.id || fileIndex)}`"
              class="h-100px w-100px overflow-hidden border border-gray-200 rounded"
            >
              <img
                v-if="isImageFile(file) && fileOpenUrl(file)"
                :src="fileOpenUrl(file)"
                :alt="fileDisplayName(file)"
                class="h-full w-full cursor-pointer object-cover"
                @click="openFilePreview(file)"
              />
              <video
                v-else-if="isVideoFile(file) && fileOpenUrl(file)"
                :src="fileOpenUrl(file)"
                muted
                playsinline
                preload="metadata"
                class="h-full w-full cursor-pointer bg-black object-cover"
                @click="openFilePreview(file)"
              />
              <div v-else class="h-full w-full flex items-center justify-center bg-gray-50 p-4px text-center">
                <AButton type="link" class="max-w-full text-ellipsis whitespace-nowrap p-0" @click="openFilePreview(file)">
                  {{ fileDisplayName(file) }}
                </AButton>
              </div>
            </div>
          </div>
        </ADescriptionsItem>
      </ADescriptions>

      <div class="mb-8px text-14px text-gray-700">故障信息</div>
      <ADescriptions bordered size="small" :column="2" class="mb-16px">
        <ADescriptionsItem v-for="it in faultInfoItems" :key="it.key" :label="it.label" :span="it.span || 1">
          {{ textValue(it.value) }}
        </ADescriptionsItem>
        <ADescriptionsItem v-if="hasFaultImageOrVideo" label="故障图片/视频" :span="2">
          <div class="flex flex-wrap gap-8px">
            <div
              v-for="(file, fileIndex) in faultImageFiles"
              :key="`fault-img-${String(file.fileId || file.id || fileIndex)}`"
              class="h-100px w-100px overflow-hidden border border-gray-200 rounded"
            >
              <img
                v-if="fileOpenUrl(file)"
                :src="fileOpenUrl(file)"
                :alt="fileDisplayName(file)"
                class="h-full w-full cursor-pointer object-cover"
                @click="openFilePreview(file)"
              />
            </div>
            <div
              v-for="(file, fileIndex) in faultVideoFiles"
              :key="`fault-vid-${String(file.fileId || file.id || fileIndex)}`"
              class="fault-video-thumb relative h-100px w-100px cursor-pointer overflow-hidden border border-gray-200 rounded bg-black"
              role="button"
              tabindex="0"
              @click="openFilePreview(file)"
              @keydown.enter="openFilePreview(file)"
            >
              <video
                v-if="fileOpenUrl(file)"
                :src="fileOpenUrl(file)"
                muted
                playsinline
                preload="metadata"
                class="pointer-events-none h-full w-full object-cover"
              />
              <div
                class="pointer-events-none absolute inset-0 flex items-center justify-center"
                style="background-color: rgba(0, 0, 0, 0.32)"
              >
                <svg
                  width="28"
                  height="28"
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                  style="filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.35))"
                >
                  <path d="M9 7.2v9.6L16.2 12 9 7.2Z" fill="#ffffff" fill-opacity="0.96" />
                </svg>
              </div>
            </div>
          </div>
        </ADescriptionsItem>
        <ADescriptionsItem v-if="faultVoiceFiles.length" label="故障语音" :span="2">
          <div class="flex flex-wrap gap-8px">
            <div
              v-for="(file, fileIndex) in faultVoiceFiles"
              :key="`fault-voice-${String(file.fileId || file.id || fileIndex)}`"
              class="h-100px w-100px overflow-hidden border border-gray-200 rounded"
            >
              <div class="h-full w-full flex items-center justify-center bg-gray-50 p-4px text-center">
                <AButton type="link" class="max-w-full text-ellipsis whitespace-nowrap p-0" @click="openFilePreview(file)">
                  {{ fileDisplayName(file) }}
                </AButton>
              </div>
            </div>
          </div>
        </ADescriptionsItem>
      </ADescriptions>

      <template v-if="showCompletedRepairInfo && faultPointHistoryRows.length">
        <div class="mb-8px text-14px text-gray-700">故障点信息（全部历史记录）</div>
        <ATable
          bordered
          class="mb-16px"
          :columns="faultPointColumns"
          :data-source="faultPointHistoryRows"
          :pagination="false"
          size="small"
          row-key="key"
        />
      </template>

      <template v-if="visibleRepairs.length">
        <div class="mb-8px text-14px text-gray-700">维修信息</div>
      </template>
      <ADescriptions
        v-if="repairQuoteSummaryItems.length"
        bordered
        size="small"
        :column="2"
        class="mb-12px"
      >
        <ADescriptionsItem
          v-for="it in repairQuoteSummaryItems"
          :key="it.key"
          :label="it.label"
          :span="it.span || 1"
        >
          {{ textValue(it.value) }}
        </ADescriptionsItem>
      </ADescriptions>
      <div
        v-for="(repair, repairIndex) in visibleRepairs"
        :key="String((repair as Record<string, unknown>).id || `repair-${repairIndex}`)"
        class="mb-16px"
      >
        <ADescriptions bordered size="small" :column="2">
          <ADescriptionsItem label="登记阶段">{{ textValue((repair as Record<string, unknown>).registerStageLabel) }}</ADescriptionsItem>
          <ADescriptionsItem label="登记时间">{{ textValue((repair as Record<string, unknown>).createTime) }}</ADescriptionsItem>
          <ADescriptionsItem label="维修公司">{{ textValue((repair as Record<string, unknown>).companyName) }}</ADescriptionsItem>
          <ADescriptionsItem label="维修人">{{ textValue((repair as Record<string, unknown>).repairUserName) }}</ADescriptionsItem>
          <ADescriptionsItem label="维修完成">{{ textValue((repair as Record<string, unknown>).isFinished) }}</ADescriptionsItem>
          <ADescriptionsItem label="完成时间">{{ textValue((repair as Record<string, unknown>).finishedTime) }}</ADescriptionsItem>
          <template
            v-for="att in repairAttachmentFileLists(repair as Record<string, unknown>)"
            :key="`${(repair as Record<string, unknown>).id || repairIndex}-${att.key}`"
          >
            <ADescriptionsItem :label="att.label" :span="2">
              <div class="flex flex-wrap gap-8px">
                <div
                  v-for="(file, fileIndex) in att.files"
                  :key="`${(repair as Record<string, unknown>).id || repairIndex}-${att.key}-${String(file.fileId || file.id || fileIndex)}`"
                  class="h-100px w-100px overflow-hidden border border-gray-200 rounded"
                >
                  <img
                    v-if="isImageFile(file) && fileOpenUrl(file)"
                    :src="fileOpenUrl(file)"
                    :alt="fileDisplayName(file)"
                    class="h-full w-full cursor-pointer object-cover"
                    @click="openFilePreview(file)"
                  />
                  <video
                    v-else-if="isVideoFile(file) && fileOpenUrl(file)"
                    :src="fileOpenUrl(file)"
                    muted
                    playsinline
                    preload="metadata"
                    class="h-full w-full cursor-pointer bg-black object-cover"
                    @click="openFilePreview(file)"
                  />
                  <div v-else class="h-full w-full flex items-center justify-center bg-gray-50 p-4px text-center">
                    <AButton type="link" class="max-w-full text-ellipsis whitespace-nowrap p-0" @click="openFilePreview(file)">
                      {{ fileDisplayName(file) }}
                    </AButton>
                  </div>
                </div>
              </div>
            </ADescriptionsItem>
          </template>
        </ADescriptions>
      </div>

      <div class="mb-8px text-14px text-gray-700">受理方信息</div>
      <ADescriptions bordered size="small" :column="2" class="mb-16px">
        <ADescriptionsItem v-for="it in acceptorInfoItems" :key="it.key" :label="it.label" :span="it.span || 1">
          {{ textValue(it.value) }}
        </ADescriptionsItem>
      </ADescriptions>

      <template v-if="evaluationInfoItems.length">
        <div class="mb-8px text-14px text-gray-700">客户评价</div>
        <ADescriptions bordered size="small" :column="2" class="mb-16px">
          <ADescriptionsItem v-for="it in evaluationInfoItems" :key="it.key" :label="it.label" :span="it.span || 1">
            {{ textValue(it.value) }}
          </ADescriptionsItem>
        </ADescriptions>
      </template>

      <div class="mb-8px text-14px text-gray-700">参与方</div>
      <ATable
        bordered
        class="mb-16px"
        :columns="participantsColumns"
        :data-source="participants"
        :pagination="false"
        size="small"
        row-key="id"
      />

      <div class="mb-8px text-14px text-gray-700">流转历史</div>
      <ATable
        bordered
        class="mb-16px"
        :columns="flowsColumns"
        :data-source="flows"
        :pagination="false"
        size="small"
        row-key="id"
      />

      <template v-if="quotes.length">
        <div class="mb-8px text-14px text-gray-700">报价记录</div>
        <ATable
          bordered
          class="mb-16px"
          :columns="quotesColumns"
          :data-source="quotes"
          :pagination="false"
          size="small"
          row-key="id"
        />
      </template>

      <template v-if="notifyEvents.length">
        <div class="mb-8px text-14px text-gray-700">通知事件</div>
        <ATable
          bordered
          class="mb-16px"
          :columns="notifyColumns"
          :data-source="notifyEvents"
          :pagination="false"
          size="small"
          row-key="id"
        />
      </template>

      <AModal v-model:open="previewOpen" :title="previewTitle || '附件预览'" :footer="null" :width="820" centered>
        <img v-if="previewType === 'image' && previewUrl" :src="previewUrl" class="max-h-70vh w-full object-contain" />
        <video
          v-else-if="previewType === 'video' && previewUrl"
          :key="previewUrl"
          :src="previewUrl"
          controls
          autoplay
          playsinline
          class="max-h-70vh w-full bg-black"
        />
      </AModal>
    </ASpin>

    <ADrawer v-model:open="techAcceptOpen" title="维修员接单" :width="420">
      <AForm layout="vertical">
        <AFormItem label="故障判断" required>
          <ASelect
            v-model:value="techAcceptForm.faultJudge"
            :options="[
              { label: faultJudgeHasFault, value: faultJudgeHasFault },
              { label: faultJudgeNoFault, value: faultJudgeNoFault }
            ]"
            placeholder="请选择故障判断"
          />
        </AFormItem>
        <template v-if="techAcceptForm.faultJudge === faultJudgeHasFault">
          <AFormItem label="报价金额">
            <AInputNumber v-model:value="techAcceptForm.quoteAmount" class="w-full" :min="0" :precision="2" />
          </AFormItem>
          <AFormItem label="报价说明">
            <ATextarea v-model:value="techAcceptForm.quoteDesc" :rows="3" allow-clear placeholder="请输入报价说明" />
          </AFormItem>
        </template>
        <AAlert
          v-else-if="techAcceptForm.faultJudge === faultJudgeNoFault"
          type="warning"
          show-icon
          :closable="false"
          message="选择无故障后，下一步将进入关闭工单弹窗填写返回方式和关闭原因"
        />
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="techAcceptOpen = false">取消</AButton>
          <AButton type="primary" :loading="techAcceptSubmitting" @click="submitAccept">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="assignOpen" title="派单" :width="360">
      <AForm layout="vertical">
        <AFormItem label="维修员" required>
          <ASelect
            v-model:value="assignUserId"
            show-search
            option-filter-prop="label"
            :options="assignOptions"
            placeholder="请选择维修员"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="assignOpen = false">取消</AButton>
          <AButton type="primary" :loading="assignSubmitting" @click="submitAssign">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="transferOpen" title="转单" :width="360">
      <AForm layout="vertical">
        <AFormItem label="目标公司" required>
          <ASelect
            v-model:value="transferCompanyId"
            show-search
            option-filter-prop="label"
            :options="transferOptions"
            placeholder="请选择目标公司"
          />
        </AFormItem>
        <AFormItem label="转单备注">
          <ATextarea v-model:value="transferRemark" :rows="3" allow-clear placeholder="请输入转单备注" />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="transferOpen = false">取消</AButton>
          <AButton type="primary" :loading="transferSubmitting" @click="submitTransfer">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="repairOpen" title="维修登记" :width="560">
      <AForm layout="vertical">
        <AFormItem label="报价金额">
          <AInputNumber v-model:value="repairForm.quoteAmount" class="w-full" :min="0" :precision="2" />
        </AFormItem>
        <AFormItem label="报价说明">
          <AInput v-model:value="repairForm.quoteDesc" allow-clear placeholder="请输入报价说明" />
        </AFormItem>
        <AFormItem v-if="hasRepairFaultConfig" label="维修确认故障" required>
          <ASelect
            v-model:value="repairForm.faultItems"
            mode="multiple"
            :options="repairFaultOptionsWithOther.map(item => ({ label: item, value: item }))"
            placeholder="请选择维修确认故障"
            @change="handleRepairFaultItemsChange"
          />
        </AFormItem>
        <AFormItem v-if="showRepairFaultRemarkInput" label="其它故障说明" required>
          <ATextarea v-model:value="repairForm.faultRemark" :rows="2" allow-clear placeholder="请输入其它故障说明" />
        </AFormItem>
        <AFormItem v-if="hasRepairFaultConfig" label="维修说明" required>
          <ASelect
            v-model:value="repairForm.repairItems"
            mode="multiple"
            :options="[
              ...currentRepairOptions.map(item => ({ label: item, value: item })),
              { label: OTHER_REPAIR_OPTION, value: OTHER_REPAIR_OPTION }
            ]"
            placeholder="请选择维修说明"
            @change="handleRepairItemsChange"
          />
        </AFormItem>
        <AFormItem v-else label="维修说明" required>
          <ATextarea v-model:value="repairForm.repairDesc" :rows="3" allow-clear placeholder="请输入维修说明" />
        </AFormItem>
        <AFormItem v-if="isOtherRepairSelected || !hasRepairFaultConfig" label="其他维修说明">
          <ATextarea v-model:value="repairForm.otherDesc" :rows="2" allow-clear placeholder="请输入其他维修说明" />
        </AFormItem>
        <AFormItem label="更换配件" required>
          <div
            v-for="(item, idx) in repairForm.partList"
            :key="`repair-part-${idx}`"
            class="mb-8px flex items-center gap-8px"
          >
            <AInput v-model:value="item.partName" placeholder="请输入配件名称" />
            <AInputNumber v-model:value="item.partQty" :min="1" :precision="0" />
            <AButton size="small" @click="addPartRow(repairForm.partList)">+</AButton>
            <AButton
              size="small"
              :disabled="repairForm.partList.length <= 1"
              @click="removePartRow(repairForm.partList, idx)"
            >
              -
            </AButton>
          </div>
        </AFormItem>
        <AFormItem label="故障处图片">
          <div class="grid grid-cols-2 gap-12px lg:grid-cols-3 xl:grid-cols-4">
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="repairForm.faultOldImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestRepairOldImage"
                :on-remove="handleDetailIdUploadRemove(repairForm.faultOldImageFileIds)"
              >
                <div
                  v-if="repairForm.faultOldImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">故障处旧图片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="repairForm.faultImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestRepairImage"
                :on-remove="handleDetailIdUploadRemove(repairForm.faultImageFileIds)"
              >
                <div
                  v-if="repairForm.faultImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">故障处新图片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="repairForm.machineImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestRepairMachineImage"
                :on-remove="handleDetailIdUploadRemove(repairForm.machineImageFileIds)"
              >
                <div
                  v-if="repairForm.machineImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">机器正面照片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="repairForm.machineBarcodeImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestRepairMachineBarcodeImage"
                :on-remove="handleDetailIdUploadRemove(repairForm.machineBarcodeImageFileIds)"
              >
                <div
                  v-if="repairForm.machineBarcodeImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">机器条码照片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="repairForm.otherImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestRepairOtherImage"
                :on-remove="handleDetailIdUploadRemove(repairForm.otherImageFileIds)"
              >
                <div
                  v-if="repairForm.otherImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">其它图片</span>
                </div>
              </AUpload>
            </div>
          </div>
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="repairOpen = false">取消</AButton>
          <AButton type="primary" :loading="repairSubmitting" @click="submitRepair">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="reviewOpen" title="复检登记" :width="560">
      <AForm layout="vertical">
        <AFormItem label="客户报修故障">
          <ATextarea :value="textValue(detail.faultDesc, '当前工单未记录故障描述')" :rows="2" disabled />
        </AFormItem>
        <AFormItem label="维修确认故障">
          <ATextarea
            :value="textValue(firstRepairConfirmedFaultDesc, '首次维修登记未记录故障描述')"
            :rows="2"
            disabled
          />
        </AFormItem>
        <AFormItem v-if="showReviewFaultRemark" label="其它故障说明">
          <ATextarea :value="textValue(firstRepairConfirmedFaultRemark)" :rows="2" disabled />
        </AFormItem>
        <AFormItem v-if="hasRepairFaultConfig" label="维修说明" required>
          <ASelect
            v-model:value="reviewForm.repairItems"
            mode="multiple"
            :options="[
              ...reviewRepairOptions.map(item => ({ label: item, value: item })),
              { label: OTHER_REPAIR_OPTION, value: OTHER_REPAIR_OPTION }
            ]"
            placeholder="请选择维修说明"
            @change="handleReviewItemsChange"
          />
        </AFormItem>
        <AFormItem v-else label="复检说明" required>
          <ATextarea v-model:value="reviewForm.repairDesc" :rows="3" allow-clear placeholder="请输入维修说明" />
        </AFormItem>
        <AFormItem v-if="isOtherReviewSelected || !hasRepairFaultConfig" label="其他维修说明">
          <ATextarea v-model:value="reviewForm.otherDesc" :rows="2" allow-clear placeholder="请输入其他维修说明" />
        </AFormItem>
        <AFormItem label="更换配件" required>
          <div
            v-for="(item, idx) in reviewForm.partList"
            :key="`review-part-${idx}`"
            class="mb-8px flex items-center gap-8px"
          >
            <AInput v-model:value="item.partName" placeholder="请输入配件名称" />
            <AInputNumber v-model:value="item.partQty" :min="1" :precision="0" />
            <AButton size="small" @click="addPartRow(reviewForm.partList)">+</AButton>
            <AButton
              size="small"
              :disabled="reviewForm.partList.length <= 1"
              @click="removePartRow(reviewForm.partList, idx)"
            >
              -
            </AButton>
          </div>
        </AFormItem>
        <AFormItem label="故障处图片">
          <div class="grid grid-cols-2 gap-12px lg:grid-cols-3 xl:grid-cols-4">
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="reviewForm.faultOldImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestReviewOldImage"
                :on-remove="handleDetailIdUploadRemove(reviewForm.faultOldImageFileIds)"
              >
                <div
                  v-if="reviewForm.faultOldImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">故障处旧图片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="reviewForm.faultImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestReviewImage"
                :on-remove="handleDetailIdUploadRemove(reviewForm.faultImageFileIds)"
              >
                <div
                  v-if="reviewForm.faultImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">故障处新图片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="reviewForm.machineImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestReviewMachineImage"
                :on-remove="handleDetailIdUploadRemove(reviewForm.machineImageFileIds)"
              >
                <div
                  v-if="reviewForm.machineImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">机器正面照片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="reviewForm.machineBarcodeImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestReviewMachineBarcodeImage"
                :on-remove="handleDetailIdUploadRemove(reviewForm.machineBarcodeImageFileIds)"
              >
                <div
                  v-if="reviewForm.machineBarcodeImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">机器条码照片</span>
                </div>
              </AUpload>
            </div>
            <div>
              <AUpload
                class="create-upload-picture-card"
                list-type="picture-card"
                accept="image/*"
                :max-count="DETAIL_UPLOAD_SINGLE_MAX"
                :open-file-dialog-on-click="reviewForm.otherImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                :custom-request="customRequestReviewOtherImage"
                :on-remove="handleDetailIdUploadRemove(reviewForm.otherImageFileIds)"
              >
                <div
                  v-if="reviewForm.otherImageFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
                  class="create-upload-card-trigger"
                >
                  <span class="create-upload-card-trigger__plus">+</span>
                  <span class="create-upload-card-trigger__text">其它图片</span>
                </div>
              </AUpload>
            </div>
          </div>
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="reviewOpen = false">取消</AButton>
          <AButton type="primary" :loading="reviewSubmitting" @click="submitReview">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="mailOpen" title="上传寄件单号" :width="420">
      <AForm layout="vertical">
        <AFormItem label="寄件凭证" required>
          <AUpload
            class="create-upload-picture-card"
            list-type="picture-card"
            accept="image/*"
            :max-count="DETAIL_UPLOAD_SINGLE_MAX"
            :open-file-dialog-on-click="mailForm.senderVoucherFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
            :custom-request="customRequestMailVoucher"
            :on-remove="handleDetailIdUploadRemove(mailForm.senderVoucherFileIds)"
          >
            <div
              v-if="mailForm.senderVoucherFileIds.length < DETAIL_UPLOAD_SINGLE_MAX"
              class="create-upload-card-trigger"
            >
              <span class="create-upload-card-trigger__plus">+</span>
              <span class="create-upload-card-trigger__text">上传</span>
            </div>
          </AUpload>
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="mailOpen = false">取消</AButton>
          <AButton type="primary" :loading="mailSubmitting" @click="submitMail">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="closeOpen" title="关闭工单" :width="420">
      <AForm layout="vertical">
        <AFormItem label="返回方式" required>
          <ASelect
            v-model:value="closeForm.returnMethod"
            placeholder="请选择返回方式"
            :options="[
              { label: '自提', value: '自提' },
              { label: '回寄', value: '回寄' }
            ]"
          />
        </AFormItem>
        <AFormItem v-if="closeForm.returnMethod === '回寄'" label="回寄凭证" required>
          <AUpload
            class="create-upload-picture-card"
            list-type="picture-card"
            accept="image/*"
            :max-count="DETAIL_UPLOAD_CLOSE_VOUCHER_MAX"
            :open-file-dialog-on-click="closeForm.returnVoucherFileIds.length < DETAIL_UPLOAD_CLOSE_VOUCHER_MAX"
            :custom-request="customRequestCloseVoucher"
            :on-remove="handleDetailIdUploadRemove(closeForm.returnVoucherFileIds)"
          >
            <div
              v-if="closeForm.returnVoucherFileIds.length < DETAIL_UPLOAD_CLOSE_VOUCHER_MAX"
              class="create-upload-card-trigger"
            >
              <span class="create-upload-card-trigger__plus">+</span>
              <span class="create-upload-card-trigger__text">上传</span>
            </div>
          </AUpload>
        </AFormItem>
        <AFormItem label="关闭原因" required>
          <ATextarea v-model:value="closeForm.closeReason" :rows="3" allow-clear placeholder="请输入关闭原因" />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="cancelCloseDialog">取消</AButton>
          <AButton type="primary" :loading="closeSubmitting" @click="submitClose">确定</AButton>
        </ASpace>
      </template>
    </ADrawer>

    <ADrawer v-model:open="repairProductModelOpen" title="补录机型" :width="560">
      <AForm layout="vertical">
        <AAlert
          type="warning"
          show-icon
          class="mb-12px"
          :closable="false"
          message="佳士品牌工单在维修登记或复检前必须先补录机器型号，补录后不可再次修改。"
        />
        <AFormItem label="机器型号" required>
          <ASelect
            v-model:value="repairProductModel"
            show-search
            allow-clear
            :filter-option="false"
            placeholder="请输入关键字搜索并选择"
            :options="repairProductModelOptions.map(item => ({ label: item, value: item }))"
            :loading="repairProductModelOptionsLoading"
            @search="loadRepairProductModelOptionList"
          />
        </AFormItem>
      </AForm>
      <template #footer>
        <ASpace :size="16">
          <AButton @click="repairProductModelOpen = false">取消</AButton>
          <AButton type="primary" :loading="repairProductModelSubmitting" @click="submitRepairProductModel">
            确定
          </AButton>
        </ASpace>
      </template>
    </ADrawer>
  </ADrawer>
</template>

<style scoped>
/* 与 `WorkOrderCreateModals` 建单页 picture-card 上传一致：达上限后隐藏「+」触发区 */
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
</style>
