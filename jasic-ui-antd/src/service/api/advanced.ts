/**
 * 高级/兼容接口：对 system 等模块的别名封装，避免旧页面改 import 路径。
 */
import { listMachineBarcode } from './system';

type Query = Record<string, unknown>;

/** 作用：机器条码档案列表（兼容旧名 `listBarcodeRule`）。 */
export function listBarcodeRule(params?: Query) {
  return listMachineBarcode(params);
}
