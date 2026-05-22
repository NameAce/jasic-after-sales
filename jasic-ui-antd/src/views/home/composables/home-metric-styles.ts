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
    color: { start: '#fcbc25', end: '#f68057' },
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
    color: { start: '#5da8ff', end: '#3d7ee8' },
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
 * 首页饼图默认色板（与组织治理环图一致：天蓝 / 淡紫 / 暖黄 / 青绿，并补充灰 / 粉 / 蓝备用）。
 * 无 colorByCode 时按指标顺序循环取色。
 */
export const HOME_PIE_COLORS: readonly string[] = [
  '#5CA9FF',
  '#94A1FF',
  '#FFD666',
  '#32D9CB',
  '#8c8c8c',
  '#ec4786',
  '#5da8ff'
];

/**
 * 组织治理饼图扇区配色（参考四色环图：天蓝 / 淡紫 / 暖黄 / 青绿）。
 * 顺序与后端组织治理指标一致：总部数 → 服务网点数 → 启用主体数 → 停用主体数。
 */
export const PLATFORM_ORG_PIE_COLOR_BY_CODE: Record<string, string> = {
  HQ_COUNT: '#5CA9FF',
  SERVICE_COUNT: '#94A1FF',
  ENABLED_SUBJECT_COUNT: '#FFD666',
  DISABLED_SUBJECT_COUNT: '#32D9CB'
};

/**
 * 工单承接池饼图扇区配色：取 WORK_ORDER_METRIC_STYLES 渐变起始色，与总部/网点 KPI 卡片主色一致。
 */
export const WORK_ORDER_PIE_COLOR_BY_CODE: Record<string, string> = {
  PENDING_ASSIGN: WORK_ORDER_METRIC_STYLES.PENDING_ASSIGN.color.start,
  PENDING_TECH_ACCEPT: WORK_ORDER_METRIC_STYLES.PENDING_TECH_ACCEPT.color.start,
  IN_PROGRESS: WORK_ORDER_METRIC_STYLES.IN_PROGRESS.color.start,
  COMPLETED: WORK_ORDER_METRIC_STYLES.COMPLETED.color.start,
  CLOSED: WORK_ORDER_METRIC_STYLES.CLOSED.color.start
};

/** 账号治理折线图数据点配色（git platform-kpi 四色：蓝 / 紫 / 青 / 粉） */
export const PLATFORM_ACCOUNT_LINE_COLOR_BY_CODE: Record<string, string> = {
  USER_TOTAL: '#5da8ff',
  ENABLED_USER_COUNT: '#8e9dff',
  DISABLED_USER_COUNT: '#26deca',
  ROLE_COUNT: '#ec4786'
};

/**
 * 解析指标卡片样式：优先业务样式表，否则默认样式。
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
