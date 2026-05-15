// 路由 component 字段中 layout 前缀
const LAYOUT_PREFIX = 'layout.';
// 路由 component 字段中页面视图前缀
const VIEW_PREFIX = 'view.';
// 一级路由中 layout 与 page 的分隔符
const FIRST_LEVEL_ROUTE_COMPONENT_SPLIT = '$';

/**
 * 作用：从后端 component 字符串解析出 layout 名与页面组件名。
 * @param component - 形如 layout.base$view.xxx 的字符串
 * @returns layout、page 字段
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getLayoutAndPage(component?: string | null) {
  let layout = '';
  let page = '';

  const [layoutOrPage = '', pageItem = ''] = component?.split(FIRST_LEVEL_ROUTE_COMPONENT_SPLIT) || [];

  layout = getLayout(layoutOrPage);
  page = getPage(pageItem || layoutOrPage);

  return { layout, page };
}

/**
 * 作用：去掉 layout 前缀得到布局组件短名。
 * @param layout - 原始片段
 * @returns 布局名或空
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function getLayout(layout: string) {
  return layout.startsWith(LAYOUT_PREFIX) ? layout.replace(LAYOUT_PREFIX, '') : '';
}

/**
 * 作用：去掉 view 前缀得到页面组件短名。
 * @param page - 原始片段
 * @returns 页面组件名或空
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
function getPage(page: string) {
  return page.startsWith(VIEW_PREFIX) ? page.replace(VIEW_PREFIX, '') : '';
}

/**
 * 作用：将 layout 与 page 拼回后端约定的 component 字符串。
 * @param layout - 布局名
 * @param page - 页面组件名
 * @returns component 字段值
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function transformLayoutAndPageToComponent(layout: string, page: string) {
  const hasLayout = Boolean(layout);
  const hasPage = Boolean(page);

  if (hasLayout && hasPage) {
    return `${LAYOUT_PREFIX}${layout}${FIRST_LEVEL_ROUTE_COMPONENT_SPLIT}${VIEW_PREFIX}${page}`;
  }

  if (hasLayout) {
    return `${LAYOUT_PREFIX}${layout}`;
  }

  if (hasPage) {
    return `${VIEW_PREFIX}${page}`;
  }

  return '';
}

/**
 * 作用：将路由 name（下划线）转为 URL path。
 * @param routeName - 路由 name
 * @returns 以 / 开头的路径
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getRoutePathByRouteName(routeName: string) {
  return `/${routeName.replace(/_/g, '/')}`;
}

/**
 * 作用：从路径字符串中拆分静态 path 与动态参数段。
 * @param routePath - 路由 path（可含 /:param）
 * @returns path 与 param 名称
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getPathParamFromRoutePath(routePath: string) {
  const [path, param = ''] = routePath.split('/:');

  return {
    path,
    param
  };
}

/**
 * 作用：将 path 与动态参数名组合成带占位符的路由 path。
 * @param routePath - 静态 path
 * @param param - 参数名（可无）
 * @returns 完整 path 模板
 * @修改人 黄碧莲
 * @修改时间 2026-05-14
 */
export function getRoutePathWithParam(routePath: string, param: string) {
  if (param.trim()) {
    return `${routePath}/:${param}`;
  }

  return routePath;
}
