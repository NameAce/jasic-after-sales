import { computed, reactive, ref } from 'vue';
import type { Ref } from 'vue';
import { jsonClone } from '@sa/utils';
import useBoolean from './use-boolean';
import useLoading from './use-loading';

export type MaybePromise<T> = T | Promise<T>;

export type ApiFn = (args: any) => Promise<unknown>;

export type TableColumnCheck = {
  key: string;
  title: string;
  checked: boolean;
};

export type TableDataWithIndex<T> = T & { index: number };

export type TransformedData<T> = {
  data: TableDataWithIndex<T>[];
  pageNum: number;
  pageSize: number;
  total: number;
};

export type Transformer<T, Response> = (response: Response) => TransformedData<T>;

export type TableConfig<A extends ApiFn, T, C> = {
  /** api function to get table data */
  apiFn: A;
  /** api params */
  apiParams?: Parameters<A>[0];
  /** transform api response to table data */
  transformer: Transformer<T, Awaited<ReturnType<A>>>;
  /** columns factory */
  columns: () => C[];
  /**
   * get column checks
   *
   * @param columns
   */
  getColumnChecks: (columns: C[]) => TableColumnCheck[];
  /**
   * get columns
   *
   * @param columns
   */
  getColumns: (columns: C[], checks: TableColumnCheck[]) => C[];
  /**
   * callback when response fetched
   *
   * @param transformed transformed data
   * @param response 原始接口返回值（扁平请求时含 error/response/data，便于取成功时的 msg）
   */
  onFetched?: (transformed: TransformedData<T>, response: Awaited<ReturnType<A>>) => MaybePromise<void>;
  /**
   * whether to get data immediately
   *
   * @default true
   */
  immediate?: boolean;
};

export default function useHookTable<A extends ApiFn, T, C>(config: TableConfig<A, T, C>) {
  const { loading, startLoading, endLoading } = useLoading();
  const { bool: empty, setBool: setEmpty } = useBoolean();

  const { apiFn, apiParams, transformer, immediate = true, getColumnChecks, getColumns } = config;

  const searchParams: NonNullable<Parameters<A>[0]> = reactive({ ...apiParams });

  const allColumns = ref(config.columns()) as Ref<C[]>;

  const data: Ref<TableDataWithIndex<T>[]> = ref([]);

  const columnChecks: Ref<TableColumnCheck[]> = ref(getColumnChecks(config.columns()));

  const columns = computed(() => getColumns(allColumns.value, columnChecks.value));

  function reloadColumns() {
    allColumns.value = config.columns();

    const checkMap = new Map(columnChecks.value.map(col => [col.key, col.checked]));

    const defaultChecks = getColumnChecks(allColumns.value);

    columnChecks.value = defaultChecks.map(col => ({
      ...col,
      checked: checkMap.get(col.key) ?? col.checked
    }));
  }

  async function getData() {
    startLoading();

    const formattedParams = formatSearchParams(searchParams);

    const response = await apiFn(formattedParams);

    const transformed = transformer(response as Awaited<ReturnType<A>>);

    data.value = transformed.data;

    setEmpty(transformed.data.length === 0);

    await config.onFetched?.(transformed, response as Awaited<ReturnType<A>>);

    endLoading();
  }

  function formatSearchParams(params: Record<string, unknown>) {
    const formattedParams: Record<string, unknown> = {};

    Object.entries(params).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        formattedParams[key] = value;
      }
    });

    return formattedParams;
  }

  /**
   * update search params
   *
   * @param params
   */
  function updateSearchParams(params: Partial<Parameters<A>[0]>) {
    Object.assign(searchParams, params);
  }

  /**
   * 重置查询参数为 apiParams 快照。
   * 说明：仅用 Object.assign 合并会遗留「运行期写入但不在 apiParams 声明里」的字段，
   * 导致重置后接口仍带上旧筛选；这里先删掉多余键再写入默认值，保证与初次进入列表一致。
   */
  function resetSearchParams() {
    const defaults = jsonClone(apiParams ?? {}) as Record<string, unknown>;
    const snapshot = searchParams as Record<string, unknown>;
    const extraKeys = Object.keys(snapshot).filter(key => !(key in defaults));
    for (let i = 0; i < extraKeys.length; i += 1) {
      Reflect.deleteProperty(snapshot, extraKeys[i]);
    }
    Object.assign(searchParams, defaults);
    // 重置后按默认条件刷新列表，避免仅靠子组件重置而表格仍显示旧筛选结果。
    getData();
  }

  if (immediate) {
    getData();
  }

  return {
    loading,
    empty,
    data,
    columns,
    columnChecks,
    reloadColumns,
    getData,
    searchParams,
    updateSearchParams,
    resetSearchParams
  };
}
