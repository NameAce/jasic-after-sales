/**
 * 列表表格：请求失败或空数据时统一「空状态图标 + 接口 msg」展示。
 * @修改人 黄碧莲
 * @修改时间 2026-05-15
 */
import { type Ref, computed, h, ref } from 'vue';
import { Empty } from 'ant-design-vue';
import { getFlatErrorMsg, getFlatResponseMsg } from '@/service/request/shared';

const DEFAULT_NO_DATA = '暂无数据';

function isNil(value: unknown): value is null | undefined {
  return value === null || value === undefined;
}

/**
 * 作用：根据错误文案、成功但空列表时的后端文案、以及数据源是否为空，生成 ATable `locale`（含 Empty 简单插图）。
 * @param listFetchErrorMsg - 列表请求失败时的提示（优先展示）
 * @param listEmptyBackendMsg - 请求成功且列表为空时，从接口解析的提示（可为空）
 * @param dataSource - 表格数据源
 * @returns {ComputedRef} 供 `:locale` 绑定
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function createAntTableListLocale(
  listFetchErrorMsg: Ref<string>,
  listEmptyBackendMsg: Ref<string>,
  dataSource: Ref<readonly unknown[]>
) {
  const noDataLabel = DEFAULT_NO_DATA;
  return computed(() => {
    if (dataSource.value?.length) return {};
    const err = listFetchErrorMsg.value?.trim() || '';
    const backend = listEmptyBackendMsg.value?.trim() || '';
    const desc = err || backend || noDataLabel;
    return {
      emptyText: h(Empty, {
        image: Empty.PRESENTED_IMAGE_SIMPLE,
        description: desc
      })
    };
  });
}

/**
 * 作用：与 `createAntTableListLocale` 配套：在手动请求列表时维护错误/空列表后端文案。
 * @returns 文案 ref 与解析方法
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function useListRequestTableMsgs() {
  const listFetchErrorMsg = ref('');
  const listEmptyBackendMsg = ref('');

  function clearListMsgs() {
    listFetchErrorMsg.value = '';
    listEmptyBackendMsg.value = '';
  }

  /**
   * 作用：若扁平结果为业务失败，写入错误文案并返回 true（调用方应清空表格数据）。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  function consumeFlatError(flat: unknown): boolean {
    if (!isNil(flat) && typeof flat === 'object' && 'error' in flat) {
      const r = flat as { error?: unknown };
      if (!isNil(r.error) && r.error !== false) {
        listFetchErrorMsg.value = getFlatErrorMsg(flat, '数据加载失败');
        return true;
      }
    }
    return false;
  }

  /**
   * 作用：请求成功且未标记错误时，若列表为空则尝试从扁平结果取后端 msg。
   * @修改人 黄碧莲
   * @修改时间 2026-05-22
   */
  function refreshEmptySuccessMsg(flat: unknown, rowCount: number) {
    if (rowCount === 0 && !listFetchErrorMsg.value) {
      listEmptyBackendMsg.value = getFlatResponseMsg(flat, '');
    } else {
      listEmptyBackendMsg.value = '';
    }
  }

  function setMsgFromCatch(e: unknown) {
    const withResp =
      e && typeof e === 'object' && 'response' in e ? { response: (e as { response: unknown }).response } : null;
    listFetchErrorMsg.value =
      (withResp ? getFlatErrorMsg(withResp, '') : '') ||
      (e instanceof Error && e.message ? e.message : '') ||
      '数据加载失败';
  }

  return {
    listFetchErrorMsg,
    listEmptyBackendMsg,
    clearListMsgs,
    consumeFlatError,
    refreshEmptySuccessMsg,
    setMsgFromCatch
  };
}
