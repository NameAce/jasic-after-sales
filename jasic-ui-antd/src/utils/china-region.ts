/**
 * 省市区级联：懒加载子节点、按父级 areaCode 缓存选项，供地址/工单等表单复用。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import type { DefaultOptionType } from 'ant-design-vue/es/cascader';
import { type SysArea, type SysAreaOptionVO, getAreaDetail, listAreaOptions } from '@/service/api/org';

/** Ant Design Vue Cascader 选项（value/label 为约定字段）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export type RegionCascaderOption = {
  value: string;
  label: string;
  isLeaf?: boolean;
  loading?: boolean;
  children?: RegionCascaderOption[];
};

// 按父级 areaCode 缓存子级选项，减少同级重复请求
const optionsCache = new Map<string, SysAreaOptionVO[]>();

/**
 * 作用：生成区域选项缓存的 Map key（根级用占位串）。
 * @param parentCode 父级行政区编码
 * @returns {string} 缓存键
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function cacheKey(parentCode: string | undefined) {
  return parentCode ?? '__ROOT__';
}

/**
 * 作用：将后端 SysAreaOptionVO 转为 Cascader 选项结构。
 * @param vo 后端选项
 * @returns {RegionCascaderOption} 组件可用选项
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function mapVoToOption(vo: SysAreaOptionVO): RegionCascaderOption {
  return {
    value: vo.areaCode,
    label: vo.areaName,
    isLeaf: Boolean(vo.leaf)
  };
}

/** 拉取指定父级下的子级（带内存缓存，减轻重复请求）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export async function fetchRegionCascaderOptions(parentCode?: string): Promise<RegionCascaderOption[]> {
  const key = cacheKey(parentCode);
  let raw = optionsCache.get(key);
  if (raw === undefined) {
    const { data, error } = await listAreaOptions(parentCode);
    if (error || !Array.isArray(data)) raw = [];
    else raw = data;
    optionsCache.set(key, raw);
  }
  return raw.map(mapVoToOption);
}

/** 清空缓存（例如切换环境后需要时可调用）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function clearRegionOptionsCache() {
  optionsCache.clear();
}

/**
 * Cascader `load-data`：懒加载子级。
 * 与 Ant Design Vue Cascader 约定一致，会就地修改 `selectedOptions` 末项的 `children` / `loading`。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function loadRegionCascaderData(selectedOptions: DefaultOptionType[]): void {
  loadRegionCascaderDataAsync(selectedOptions as unknown as RegionCascaderOption[]).catch(() => {});
}

/**
 * 作用：异步加载当前选中项的子级区域并写回 children（供 Cascader loadData 使用）。
 * @param selectedOptions 当前已选中的各级选项
 * @returns {Promise<void>}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
async function loadRegionCascaderDataAsync(selectedOptions: RegionCascaderOption[]) {
  const target = selectedOptions[selectedOptions.length - 1];
  if (!target || target.isLeaf) return;
  if (target.children?.length) return;

  target.loading = true;
  try {
    const children = await fetchRegionCascaderOptions(String(target.value ?? ''));
    if (children.length) {
      target.children = children;
    } else if (!target.isLeaf) {
      target.isLeaf = true;
    }
  } finally {
    target.loading = false;
  }
}

/** 按编码链解析省市区名称（并行查详情）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export async function getRegionLabelsByCodes(codes: string[]): Promise<string[]> {
  const trimmed = codes.map(c => String(c).trim()).filter(Boolean);
  if (!trimmed.length) return [];

  const results = await Promise.all(
    trimmed.map(async code => {
      const { data, error } = await getAreaDetail(code);
      if (error || data === null || data === undefined || typeof data !== 'object') return '';
      return String((data as SysArea).areaName ?? '');
    })
  );

  return results.filter(Boolean);
}

export async function composeAddressWithRegion(regionCodes: string[], addressDetail: string): Promise<string> {
  const labels = await getRegionLabelsByCodes(regionCodes);
  const prefix = labels.join('');
  const detail = addressDetail.trim();
  if (!prefix) return detail;
  if (!detail) return prefix;
  return `${prefix}${detail}`;
}

/** 级联已选满三级即可认为合法（选项本身由接口约束）
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function isFullRegionSelection(codes: unknown[]): boolean {
  const list = codes.map(c => String(c ?? '').trim()).filter(Boolean);
  return list.length >= 3;
}

/**
 * 从完整 address 中解析省市区编码与剩余详细地址（后端仅有整段 address 时的编辑回显）。
 * 按「省+市+区」名称前缀做最长匹配；无法匹配时 regionCodes 为空，全文作为 addressDetail。
 * 依赖接口逐级拉取，结果会写入 options 缓存。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export async function splitFullAddressToRegionAndDetail(fullAddress: string): Promise<{
  regionCodes: string[];
  addressDetail: string;
}> {
  const text = fullAddress.trim();
  if (!text) return { regionCodes: [], addressDetail: '' };

  const provinces = await fetchRegionCascaderOptions(undefined);
  const provinceMatches = provinces.filter(prov => text.startsWith(prov.label));

  const perProvince = await Promise.all(
    provinceMatches.map(async prov => {
      const cities = await fetchRegionCascaderOptions(prov.value);
      const matchingCities = cities.filter(city => text.startsWith(prov.label + city.label));
      const perCity = await Promise.all(
        matchingCities.map(async city => {
          const districts = await fetchRegionCascaderOptions(city.value);
          let bestPrefixLen = 0;
          let bestCodes: string[] = [];
          const prefixPc = prov.label + city.label;
          for (const dist of districts) {
            const prefix = prefixPc + dist.label;
            if (text.startsWith(prefix) && prefix.length > bestPrefixLen) {
              bestPrefixLen = prefix.length;
              bestCodes = [prov.value, city.value, dist.value];
            }
          }
          return { bestPrefixLen, bestCodes };
        })
      );
      return perCity.reduce((acc, cur) => (cur.bestPrefixLen > acc.bestPrefixLen ? cur : acc), {
        bestPrefixLen: 0,
        bestCodes: [] as string[]
      });
    })
  );

  const best = perProvince.reduce((acc, cur) => (cur.bestPrefixLen > acc.bestPrefixLen ? cur : acc), {
    bestPrefixLen: 0,
    bestCodes: [] as string[]
  });

  const addressDetail = best.bestPrefixLen > 0 ? text.slice(best.bestPrefixLen).trim() : text;
  return { regionCodes: best.bestCodes, addressDetail };
}
