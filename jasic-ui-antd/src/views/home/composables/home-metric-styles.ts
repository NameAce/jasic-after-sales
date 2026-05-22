import { HOME_PIE_CHART_COLORS, HOME_SOYBEAN_PIE_COLORS } from './home-chart-theme';

/**
 * 首页指标卡片展示样式：按后端稳定 code 映射颜色与图标（工单 / 平台治理共用）。
 */
export interface HomeMetricCardStyle {
  color: { start: string; end: string };
  icon: string;
  /** 渐变方向，默认右下；平台基础配置多数为左右渐变 */
  gradientTo?: 'right' | 'bottom right' | 'right bottom';
}

export const DEFAULT_HOME_METRIC_STYLE: HomeMetricCardStyle = {
  color: { start: '#8e9dff', end: '#6b7fe8' },
  icon: 'mdi:chart-box-outline'
};

/**
 * 工单承接池 / 已转出 KPI 配色（佳士总部调度看板与网点服务工作台共用）。
 * 渐变方向与平台基础配置四卡一致，保证总部、网点顶部指标视觉统一。
 */
export const WORK_ORDER_METRIC_STYLES: Record<string, HomeMetricCardStyle> = {
  CURRENT_TOTAL: {
    color: { start: '#ec4786', end: '#b955a4' },
    icon: 'mdi:file-document-multiple-outline',
    gradientTo: 'right bottom'
  },
  PENDING_ASSIGN: {
    color: { start: '#865ec0', end: '#5144b4' },
    icon: 'mdi:clipboard-clock-outline',
    gradientTo: 'right bottom'
  },
  PENDING_TECH_ACCEPT: {
    color: { start: '#56cdf3', end: '#719de3' },
    icon: 'mdi:account-clock-outline',
    gradientTo: 'right bottom'
  },
  IN_PROGRESS: {
    color: { start: '#5da8ff', end: '#3d7ee8' },
    icon: 'mdi:tools',
    gradientTo: 'right bottom'
  },
  COMPLETED: {
    color: { start: '#2dcf95', end: '#1ea97a' },
    icon: 'mdi:check-circle-outline',
    gradientTo: 'right bottom'
  },
  CLOSED: {
    color: { start: '#8c8c8c', end: '#595959' },
    icon: 'mdi:archive-lock-outline',
    gradientTo: 'right bottom'
  },
  TRANSFER_OUT: {
    color: { start: '#fcbc25', end: '#f68057' },
    icon: 'mdi:swap-horizontal',
    gradientTo: 'right bottom'
  }
};

/** 平台治理看板指标 */
export const PLATFORM_METRIC_STYLES: Record<string, HomeMetricCardStyle> = {
  HQ_COUNT: {
    color: { start: '#5da8ff', end: '#3d7ee8' },
    icon: 'mdi:office-building-outline'
  },
  SERVICE_COUNT: {
    color: { start: '#26deca', end: '#1aab97' },
    icon: 'mdi:store-outline'
  },
  ENABLED_SUBJECT_COUNT: {
    color: { start: '#2dcf95', end: '#1ea97a' },
    icon: 'mdi:check-decagram-outline'
  },
  DISABLED_SUBJECT_COUNT: {
    color: { start: '#8c8c8c', end: '#595959' },
    icon: 'mdi:close-circle-outline'
  },
  USER_TOTAL: {
    color: { start: '#ec4786', end: '#b955a4' },
    icon: 'mdi:account-multiple-outline'
  },
  ENABLED_USER_COUNT: {
    color: { start: '#2dcf95', end: '#1ea97a' },
    icon: 'mdi:account-check-outline'
  },
  DISABLED_USER_COUNT: {
    color: { start: '#fcbc25', end: '#f68057' },
    icon: 'mdi:account-off-outline'
  },
  ROLE_COUNT: {
    color: { start: '#865ec0', end: '#5144b4' },
    icon: 'mdi:account-group-outline'
  },
  PRODUCT_COUNT: {
    color: { start: '#56cdf3', end: '#719de3' },
    icon: 'mdi:barcode-scan'
  },
  SERVICE_TYPE_COUNT: {
    color: { start: '#8e9dff', end: '#6b7fe8' },
    icon: 'mdi:cog-outline'
  },
  DICT_ITEM_COUNT: {
    color: { start: '#fcbc25', end: '#f68057' },
    icon: 'mdi:book-open-variant'
  },
  REGION_COUNT: {
    color: { start: '#5da8ff', end: '#3d7ee8' },
    icon: 'mdi:map-marker-radius-outline'
  }
};

/**
 * 平台基础配置四项卡片配色（参考 Soybean 首页四卡：粉 / 紫 / 蓝 / 橙黄渐变）。
 */
export const PLATFORM_BASIC_CONFIG_METRIC_STYLES: Record<string, HomeMetricCardStyle> = {
  PRODUCT_COUNT: {
    color: { start: '#ec4786', end: '#b955a4' },
    icon: 'mdi:barcode-scan',
    gradientTo: 'right bottom'
  },
  SERVICE_TYPE_COUNT: {
    color: { start: '#865ec0', end: '#5144b4' },
    icon: 'mdi:cog-outline',
    gradientTo: 'right bottom'
  },
  DICT_ITEM_COUNT: {
    color: { start: '#56cdf3', end: '#719de3' },
    icon: 'mdi:book-open-variant',
    gradientTo: 'right bottom'
  },
  REGION_COUNT: {
    color: { start: '#fcbc25', end: '#f68057' },
    icon: 'mdi:map-marker-radius-outline',
    gradientTo: 'right bottom'
  }
};

/**
 * 首页饼图默认色板（RGB 93,168,255 等基色 + 同色系扩展，见 home-chart-theme）。
 * 无 colorByCode 时按指标顺序循环取色。
 */
export const HOME_PIE_COLORS: readonly string[] = HOME_PIE_CHART_COLORS;

/**
 * 组织治理饼图扇区配色（与饼图基色 RGB 逐项对应）。
 * 顺序：总部数 → 服务网点数 → 启用主体数 → 停用主体数。
 */
export const PLATFORM_ORG_PIE_COLOR_BY_CODE: Record<string, string> = {
  HQ_COUNT: HOME_SOYBEAN_PIE_COLORS[0],
  SERVICE_COUNT: HOME_SOYBEAN_PIE_COLORS[1],
  ENABLED_SUBJECT_COUNT: HOME_SOYBEAN_PIE_COLORS[2],
  DISABLED_SUBJECT_COUNT: HOME_SOYBEAN_PIE_COLORS[3]
};

/**
 * 工单承接池饼图扇区配色（与 HOME_PIE_CHART_COLORS 同源，不用 KPI 卡片渐变以免与环图色不一致）。
 * 待派单淡紫 / 待接单天蓝 / 维修中暖黄 / 已完成青绿 / 已关闭灰（与 KPI 卡片色独立，维修中/已转出对调不影响饼图）。
 */
export const WORK_ORDER_PIE_COLOR_BY_CODE: Record<string, string> = {
  PENDING_ASSIGN: HOME_PIE_CHART_COLORS[1],
  PENDING_TECH_ACCEPT: HOME_PIE_CHART_COLORS[0],
  IN_PROGRESS: HOME_PIE_CHART_COLORS[2],
  COMPLETED: HOME_PIE_CHART_COLORS[3],
  CLOSED: HOME_PIE_CHART_COLORS[9]
};

/** 账号治理折线图数据点配色（饼图基色 + 扩展色：蓝 / 紫 / 青 / 玫红） */
export const PLATFORM_ACCOUNT_LINE_COLOR_BY_CODE: Record<string, string> = {
  USER_TOTAL: HOME_PIE_CHART_COLORS[0],
  ENABLED_USER_COUNT: HOME_PIE_CHART_COLORS[1],
  DISABLED_USER_COUNT: HOME_PIE_CHART_COLORS[3],
  ROLE_COUNT: HOME_PIE_CHART_COLORS[5]
};

/**
 * 作用：解析指标卡片展示样式（渐变、图标）；优先业务样式表，否则默认样式。
 * @param code - 后端指标稳定 code（如 CURRENT_TOTAL、PRODUCT_COUNT）
 * @param styleMap - 业务样式映射（WORK_ORDER_METRIC_STYLES 等）
 * @returns 卡片渐变与图标配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolveMetricStyle(
  code: string | undefined,
  styleMap: Record<string, HomeMetricCardStyle>
): HomeMetricCardStyle {
  if (code && styleMap[code]) {
    return styleMap[code];
  }
  return DEFAULT_HOME_METRIC_STYLE;
}
