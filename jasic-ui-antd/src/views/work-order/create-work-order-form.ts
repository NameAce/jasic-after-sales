/**
 * 工单创建表单共享：服务方式常量、故障选项、表单默认值与入口代理（供创建弹窗与列表联动）。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { CREATE_ENTRY_PROXY, type CreateEntryMode } from './create-entry';

// 服务方式：邮寄维修
export const SERVICE_MODE_MAIL = 'MAIL' as const;
// 服务方式：送店维修
export const SERVICE_MODE_STORE = 'STORE' as const;

// 「其它故障」默认展示文案
export const DEFAULT_OTHER_FAULT_LABEL = '其它故障';

/** 与 contractor `REPAIR_TYPE_OPTIONS` 文案一致：送店维修 / 邮寄维修
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export const SERVICE_MODE_OPTIONS = [
  { label: '送店维修', value: SERVICE_MODE_STORE },
  { label: '邮寄维修', value: SERVICE_MODE_MAIL }
] as const;

export interface CreateFileItem {
  fileId: number;
  originalName?: string;
  fileName?: string;
  fileSize?: number;
}

/** 建单「故障图片/视频」合并上传项，提交时按 kind 拆成 faultImageFileIds / faultVideoFileIds
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export interface CreateFaultMediaItem extends CreateFileItem {
  kind: 'image' | 'video';
}

export interface TargetCompanyOption {
  id: number;
  companyName: string;
}

export interface CreateWorkOrderFormState {
  entryMode: CreateEntryMode;
  customerName: string;
  customerMobile: string;
  barcode: string;
  queriedBarcode: string;
  barcodeQueried: boolean;
  barcodeResolved: boolean;
  barcodeQueryFailed: boolean;
  productCode: string;
  productName: string;
  productModel: string;
  machineNo: string;
  brandCode: string;
  serviceMode: typeof SERVICE_MODE_MAIL | typeof SERVICE_MODE_STORE;
  warrantyStatus: string;
  hqCompanyId: number | undefined;
  hqCompanyName: string;
  targetCompanyId: number | undefined;
  targetCompanyName: string;
  targetCompanyOptions: TargetCompanyOption[];
  faultOptions: string[];
  otherFaultLabel: string;
  faultItems: string[];
  faultRemark: string;
  faultMediaFiles: CreateFaultMediaItem[];
  faultVoiceFiles: CreateFileItem[];
  companyAddressId: number | undefined;
  senderName: string;
  senderMobile: string;
  senderAddress: string;
  sendExpressNo: string;
  senderVoucherFiles: CreateFileItem[];
}

/**
 * 作用：将任意值规范为空字符串或 trim 后的字符串。
 * @param value - 原始值
 * @returns 规范化字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function normalizeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim();
}

/**
 * 作用：生成建单表单的默认状态对象。
 * @returns 初始表单状态
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function buildDefaultCreateForm(): CreateWorkOrderFormState {
  return {
    entryMode: CREATE_ENTRY_PROXY,
    customerName: '',
    customerMobile: '',
    barcode: '',
    queriedBarcode: '',
    barcodeQueried: false,
    barcodeResolved: false,
    barcodeQueryFailed: false,
    productCode: '',
    productName: '',
    productModel: '',
    machineNo: '',
    brandCode: '',
    serviceMode: SERVICE_MODE_STORE,
    warrantyStatus: '',
    hqCompanyId: undefined,
    hqCompanyName: '',
    targetCompanyId: undefined,
    targetCompanyName: '',
    targetCompanyOptions: [],
    faultOptions: [],
    otherFaultLabel: DEFAULT_OTHER_FAULT_LABEL,
    faultItems: [],
    faultRemark: '',
    faultMediaFiles: [],
    faultVoiceFiles: [],
    companyAddressId: undefined,
    senderName: '',
    senderMobile: '',
    senderAddress: '',
    sendExpressNo: '',
    senderVoucherFiles: []
  };
}

/**
 * 作用：从附件列表提取有效的 fileId 数组。
 * @param fileList - 附件项列表
 * @returns fileId 数字数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function buildFileIdList(fileList: CreateFileItem[] | undefined): number[] {
  return (fileList || [])
    .map(item => item?.fileId)
    .filter((id): id is number => id !== null && id !== undefined && id !== ('' as never))
    .map(id => Number(id))
    .filter(id => Number.isFinite(id));
}

/**
 * 作用：浅拷贝附件列表（每项展开为新对象）。
 * @param fileList - 附件项列表
 * @returns 拷贝后的数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function cloneFileItems(fileList: CreateFileItem[] | undefined): CreateFileItem[] {
  return (fileList || []).map(item => ({ ...item }));
}

/**
 * 作用：浅拷贝故障媒体列表。
 * @param list - CreateFaultMediaItem 列表
 * @returns 拷贝后的数组
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function cloneFaultMediaItems(list: CreateFaultMediaItem[] | undefined): CreateFaultMediaItem[] {
  return (list || []).map(item => ({ ...item }));
}

/**
 * 作用：将接口或下拉原始对象转为受理网点选项。
 * @param raw - 原始对象
 * @returns 规范化选项或无效时 null
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function normalizeTargetCompanyOption(raw: Record<string, unknown>): TargetCompanyOption | null {
  const id = Number(raw.id ?? raw.companyId ?? raw.value);
  if (!Number.isFinite(id)) return null;
  return {
    id,
    companyName: String(raw.companyName ?? raw.label ?? raw.name ?? '')
  };
}
