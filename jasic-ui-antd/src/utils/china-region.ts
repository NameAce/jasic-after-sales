import { type DataItem, regionData } from 'element-china-area-data';

export const chinaRegionCascaderOptions: DataItem[] = regionData;

/** 根据已选区域编码解析为名称路径（用于拼接完整地址） */
export function getRegionLabelsByCodes(codes: string[], options: DataItem[] = regionData): string[] {
  const labels: string[] = [];
  let level = options;
  for (const code of codes) {
    const node = level.find(i => i.value === code);
    if (!node) break;
    labels.push(node.label);
    level = node.children || [];
  }
  return labels;
}

/** 省市区名称拼接 + 详细地址，生成入库的 address 字符串 */
export function composeAddressWithRegion(regionCodes: string[], addressDetail: string): string {
  const prefix = getRegionLabelsByCodes(regionCodes).join('');
  const detail = addressDetail.trim();
  if (!prefix) return detail;
  if (!detail) return prefix;
  return `${prefix}${detail}`;
}

/** 是否已选完整省市区（三级） */
export function isFullRegionSelection(codes: string[]): boolean {
  if (codes.length < 3) return false;
  let level: DataItem[] | undefined = regionData;
  for (const code of codes) {
    const node: DataItem | undefined = level?.find((i: DataItem) => i.value === code);
    if (!node) return false;
    level = node.children;
  }
  return true;
}

type RegionMatchState = { text: string; prefixLen: number; codes: string[] };

function matchDistrictsUnderCity(prov: DataItem, city: DataItem, state: RegionMatchState) {
  const { text } = state;
  const prefixPc = prov.label + city.label;
  if (!text.startsWith(prefixPc)) return;
  for (const dist of city.children || []) {
    const prefix = prefixPc + dist.label;
    if (text.startsWith(prefix) && prefix.length > state.prefixLen) {
      state.prefixLen = prefix.length;
      state.codes = [prov.value, city.value, dist.value];
    }
  }
}

/**
 * 从完整 address 中解析省市区编码与剩余详细地址（后端仅有整段 address 时的编辑回显）。
 * 按「省+市+区」名称前缀做最长匹配；无法匹配时 regionCodes 为空，全文作为 addressDetail。
 */
export function splitFullAddressToRegionAndDetail(fullAddress: string): {
  regionCodes: string[];
  addressDetail: string;
} {
  const text = fullAddress.trim();
  if (!text) return { regionCodes: [], addressDetail: '' };

  const state: RegionMatchState = { text, prefixLen: 0, codes: [] };

  for (const prov of regionData) {
    if (text.startsWith(prov.label)) {
      for (const city of prov.children || []) {
        matchDistrictsUnderCity(prov, city, state);
      }
    }
  }

  const addressDetail = state.prefixLen > 0 ? text.slice(state.prefixLen).trim() : text;
  return { regionCodes: state.codes, addressDetail };
}
