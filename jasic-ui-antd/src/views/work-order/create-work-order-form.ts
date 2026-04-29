import { CREATE_ENTRY_PROXY, type CreateEntryMode } from './create-entry';

export const SERVICE_MODE_MAIL = 'MAIL' as const;
export const SERVICE_MODE_STORE = 'STORE' as const;

export const DEFAULT_OTHER_FAULT_LABEL = '其它故障';

/** 与 contractor `REPAIR_TYPE_OPTIONS` 文案一致：送店维修 / 邮寄维修 */
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

/** 建单「故障图片/视频」合并上传项，提交时按 kind 拆成 faultImageFileIds / faultVideoFileIds */
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

export function normalizeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim();
}

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

export function buildFileIdList(fileList: CreateFileItem[] | undefined): number[] {
  return (fileList || [])
    .map(item => item?.fileId)
    .filter((id): id is number => id !== null && id !== undefined && id !== ('' as never))
    .map(id => Number(id))
    .filter(id => Number.isFinite(id));
}

export function cloneFileItems(fileList: CreateFileItem[] | undefined): CreateFileItem[] {
  return (fileList || []).map(item => ({ ...item }));
}

export function cloneFaultMediaItems(list: CreateFaultMediaItem[] | undefined): CreateFaultMediaItem[] {
  return (list || []).map(item => ({ ...item }));
}

export function normalizeTargetCompanyOption(raw: Record<string, unknown>): TargetCompanyOption | null {
  const id = Number(raw.id ?? raw.companyId ?? raw.value);
  if (!Number.isFinite(id)) return null;
  return {
    id,
    companyName: String(raw.companyName ?? raw.label ?? raw.name ?? '')
  };
}
