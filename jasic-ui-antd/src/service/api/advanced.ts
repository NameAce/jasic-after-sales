import { listMachineBarcode } from './system';

type Query = Record<string, unknown>;

/** 与 jasic 机器条码档案 `listMachineBarcode` 一致（旧名保留给引用处） */
export function listBarcodeRule(params?: Query) {
  return listMachineBarcode(params);
}
