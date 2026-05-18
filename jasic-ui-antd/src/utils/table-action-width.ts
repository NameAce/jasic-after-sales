/**
 * 作用：列表表格「操作」列统一定义（各页面自行传入固定 width，一排展示即可）。
 * @remarks 配合 `list-table-unify.css` 中 `.table-action-col`；`scroll.x` 需累加同一 width。
 * @修改人 黄碧莲
 * @修改时间 2026-05-15
 */

/** 操作列单元格 class */
export const ANT_TABLE_ACTION_COL_CLASS = 'table-action-col';

export type AntTableActionColumnOptions = {
  /** 是否固定在右侧，默认 `right` */
  fixed?: 'right' | false;
  title?: string;
  /** 列 key，默认 `actions` */
  key?: string;
  /** 与列 `dataIndex` 一致时使用，默认不写 */
  dataIndex?: string;
  /** 列宽（像素），由当前页按实际按钮一排展示需要填写 */
  width: number;
};

/**
 * 作用：生成 Ant Design Vue 表格操作列配置（固定 `width`，不使用 auto）。
 */
export function createAntTableActionColumn(options: AntTableActionColumnOptions) {
  const { fixed = 'right', title = '操作', key = 'actions', dataIndex, width } = options;

  const col: {
    title: string;
    key: string;
    className: string;
    width: number;
    align: 'left';
    fixed?: 'right';
    dataIndex?: string;
  } = {
    title,
    key,
    className: ANT_TABLE_ACTION_COL_CLASS,
    width,
    align: 'left'
  };

  if (dataIndex) {
    col.dataIndex = dataIndex;
  }
  if (fixed) {
    col.fixed = fixed;
  }
  return col;
}
