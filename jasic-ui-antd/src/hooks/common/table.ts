/**
 * 表格与列表页：分页请求、列配置、移动端分页简化等与 `useHookTable` 的封装。
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
import { computed, effectScope, onScopeDispose, reactive, ref, shallowRef, toValue, watch } from 'vue';
import type { MaybeRef, Ref } from 'vue';
import { useElementSize } from '@vueuse/core';
import type { TablePaginationConfig } from 'ant-design-vue';
import type { TableRowSelection } from 'ant-design-vue/es/table/interface';
import { useBoolean, useHookTable } from '@sa/hooks';
import { jsonClone } from '@sa/utils';
import { useAppStore } from '@/store/modules/app';
import { $t } from '@/locales';
import { getFlatErrorMsg, getFlatResponseMsg } from '@/service/request/shared';
import { createAntTableListLocale } from '@/utils/list-table-empty-state';

type TableData = AntDesign.TableData;
type GetTableData<A extends AntDesign.TableApiFn> = AntDesign.GetTableData<A>;
type TableColumn<T> = AntDesign.TableColumn<T>;

/**
 * 作用：对接后端分页列表 API 的表格状态：加载、列显隐、分页、搜索参数与移动端简化分页。
 * @param config 表格配置（apiFn、列、immediate 等）
 * @returns 表格数据与分页相关方法与状态
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useTable<A extends AntDesign.TableApiFn>(config: AntDesign.AntDesignTableConfig<A>) {
  const scope = effectScope();
  const appStore = useAppStore();

  const { apiFn, apiParams, immediate } = config;

  const listFetchErrorMsg = ref('');
  const listEmptyBackendMsg = ref('');

  const wrappedApiFn = (async (params: Parameters<A>[0]) => {
    listFetchErrorMsg.value = '';
    listEmptyBackendMsg.value = '';
    const res = await apiFn(params);
    if (res != null && typeof res === 'object' && 'error' in res) {
      const r = res as { error?: unknown };
      if (r.error != null && r.error !== false) {
        listFetchErrorMsg.value = getFlatErrorMsg(res, '数据加载失败');
        return { data: { records: [], current: 1, size: 10, total: 0 } } as Awaited<ReturnType<A>>;
      }
    }
    return res as Awaited<ReturnType<A>>;
  }) as unknown as A;

  const {
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
  } = useHookTable<A, GetTableData<A>, TableColumn<AntDesign.TableDataWithIndex<GetTableData<A>>>>({
    apiFn: wrappedApiFn,
    apiParams,
    columns: config.columns,
    transformer: res => {
      const { records = [], current = 1, size = 10, total = 0 } = res.data || {};

      // Ensure that the size is greater than 0, If it is less than 0, it will cause paging calculation errors.
      const pageSize = size <= 0 ? 10 : size;

      const recordsWithIndex = records.map((item, index) => {
        return {
          ...item,
          index: (current - 1) * pageSize + index + 1
        };
      });

      return {
        data: recordsWithIndex,
        pageNum: current,
        pageSize,
        total
      };
    },
    getColumnChecks: cols => {
      const checks: AntDesign.TableColumnCheck[] = [];

      cols.forEach(column => {
        if (column.key) {
          checks.push({
            key: column.key as string,
            title: column.title as string,
            checked: true
          });
        }
      });

      return checks;
    },
    getColumns: (cols, checks) => {
      const columnMap = new Map<string, TableColumn<GetTableData<A>>>();

      cols.forEach(column => {
        if (column.key) {
          columnMap.set(column.key as string, column);
        }
      });

      const filteredColumns = checks
        .filter(item => item.checked)
        .map(check => columnMap.get(check.key) as TableColumn<GetTableData<A>>);

      return filteredColumns;
    },
    onFetched: async (transformed, response) => {
      if (transformed.data.length === 0 && !listFetchErrorMsg.value) {
        listEmptyBackendMsg.value = getFlatResponseMsg(response, '');
      } else {
        listEmptyBackendMsg.value = '';
      }
      const { pageNum, pageSize, total } = transformed;
      updatePagination({
        current: pageNum,
        pageSize,
        total
      });
    },
    immediate
  });

  const tableListLocale = createAntTableListLocale(listFetchErrorMsg, listEmptyBackendMsg, data);

  const pagination: TablePaginationConfig = reactive({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    pageSizeOptions: ['10', '15', '20', '25', '30'],
    total: 0,
    onChange: async (current: number, size: number) => {
      pagination.current = current;

      updateSearchParams({
        current,
        size
      });

      getData();
    }
  });

  // 移动端下分页使用 simple 样式，依赖 appStore.isMobile
  const mobilePagination = computed(() => {
    const p: TablePaginationConfig = {
      ...pagination,
      simple: appStore.isMobile
    };

    return p;
  });

  function updatePagination(update: Partial<TablePaginationConfig>) {
    Object.assign(pagination, update);
  }

  /**
   * 作用：跳到指定页并拉取数据。
   * @param pageNum 页码，默认 1
   * @returns {Promise<void>}
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  async function getDataByPage(pageNum: number = 1) {
    updatePagination({
      current: pageNum
    });

    updateSearchParams({
      current: pageNum,
      size: pagination.pageSize!
    });

    await getData();
  }

  scope.run(() => {
    // 语言变化时重载列标题等文案
    watch(
      () => appStore.locale,
      () => {
        reloadColumns();
      }
    );
  });

  onScopeDispose(() => {
    scope.stop();
  });

  return {
    loading,
    empty,
    data,
    columns,
    columnChecks,
    reloadColumns,
    pagination,
    mobilePagination,
    tableListLocale,
    updatePagination,
    getData,
    getDataByPage,
    searchParams,
    updateSearchParams,
    resetSearchParams
  };
}

/**
 * 作用：表格行内「新增/编辑抽屉」、多选行与批量删除/删除后刷新的通用状态。
 * @param data 表格行数据 ref
 * @param getData 刷新列表函数
 * @returns 抽屉显隐、操作类型、选中 keys、rowSelection 与删除回调
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useTableOperate<T extends TableData = TableData>(data: Ref<T[]>, getData: () => Promise<void>) {
  const { bool: drawerVisible, setTrue: openDrawer, setFalse: closeDrawer } = useBoolean();

  const operateType = ref<AntDesign.TableOperateType>('add');

  function handleAdd() {
    operateType.value = 'add';
    openDrawer();
  }

  /** the editing row data
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  const editingData: Ref<T | null> = ref(null);

  function handleEdit(id: T['id']) {
    operateType.value = 'edit';
    const findItem = data.value.find(item => item.id === id) || null;
    editingData.value = jsonClone(findItem);

    openDrawer();
  }

  /** the checked row keys of table
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  const checkedRowKeys: Ref<T['id'][]> = ref([]);

  function onSelectChange(keys: (string | number)[]) {
    checkedRowKeys.value = keys as T['id'][];
  }

  /** 根据选中 keys 推导 Ant Design Table rowSelection
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  const rowSelection = computed<TableRowSelection<T>>(() => {
    return {
      columnWidth: 48,
      type: 'checkbox',
      selectedRowKeys: checkedRowKeys.value,
      onChange: onSelectChange
    };
  });

  /** 批量删除成功后的提示与清空选中
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  async function onBatchDeleted() {
    window.$message?.success($t('common.deleteSuccess'));

    checkedRowKeys.value = [];

    await getData();
  }

  /** 单行删除成功后的提示
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
  async function onDeleted() {
    window.$message?.success($t('common.deleteSuccess'));

    await getData();
  }

  return {
    drawerVisible,
    openDrawer,
    closeDrawer,
    operateType,
    handleAdd,
    editingData,
    handleEdit,
    checkedRowKeys,
    onSelectChange,
    rowSelection,
    onBatchDeleted,
    onDeleted
  };
}

/**
 * 作用：根据容器高度计算表格纵向滚动高度与横向 scroll.x。
 * @param scrollX 横向滚动宽度 ref 或静态值
 * @returns 包装 ref 与 scroll 配置 computed
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function useTableScroll(scrollX: MaybeRef<number> = 702) {
  const tableWrapperRef = shallowRef<HTMLElement | null>(null);
  const { height: wrapperElHeight } = useElementSize(tableWrapperRef);

  // 依赖外层高度与 scrollX，生成 y/x 滚动配置
  const scrollConfig = computed(() => {
    return {
      y: wrapperElHeight.value - 72,
      x: toValue(scrollX)
    };
  });

  return {
    tableWrapperRef,
    scrollConfig
  };
}
