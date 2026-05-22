/**
 * 首页 ECharts 通用视觉配置：卡片标题外置后图表区 grid / 图例 / 折线样式等复用。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */

/**
 * 作用：将首页图表 RGB 三元组转为 ECharts 使用的 #RRGGBB 小写十六进制色值。
 * @param rgb - [R, G, B]，各通道 0–255
 * @returns 十六进制颜色字符串
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
function homeRgbToHex(rgb: readonly [number, number, number]): string {
  return `#${rgb.map(channel => channel.toString(16).padStart(2, '0')).join('')}`;
}

/** 首页饼图基色 RGB：天蓝 / 淡紫 / 暖黄 / 青绿 */
const HOME_PIE_RGB_BASE: readonly (readonly [number, number, number])[] = [
  [93, 168, 255],
  [142, 157, 255],
  [254, 220, 105],
  [38, 222, 202]
];

/**
 * 饼图同色系扩展 RGB（与 KPI 卡片渐变主色一致，供第五项及以后扇区循环取色）。
 * 天蓝 → 玫红 → 紫 → 翠绿 → 琥珀 → 中性灰
 */
const HOME_PIE_RGB_EXTENDED: readonly (readonly [number, number, number])[] = [
  [86, 205, 243],
  [236, 71, 134],
  [134, 94, 192],
  [45, 207, 149],
  [252, 188, 37],
  [140, 140, 140]
];

/** 首页饼图完整色板（基色 4 + 扩展 6，无 colorByCode 时按顺序循环） */
export const HOME_PIE_CHART_COLORS: readonly string[] = [...HOME_PIE_RGB_BASE, ...HOME_PIE_RGB_EXTENDED].map(
  homeRgbToHex
);

/** 饼图基色前四项（环图默认、组织治理四指标等） */
export const HOME_SOYBEAN_PIE_COLORS: readonly string[] = HOME_PIE_CHART_COLORS.slice(0, 4);

/** 折线主色（单线面积图，与饼图基色第一项一致） */
export const HOME_LINE_PRIMARY_COLOR = HOME_PIE_CHART_COLORS[0];

/**
 * 首页双折线色（与饼图基色第 2、4 项一致：淡紫 / 青绿）。
 */
export const HOME_SOYBEAN_DUAL_LINE_COLORS: readonly string[] = [HOME_PIE_CHART_COLORS[1], HOME_PIE_CHART_COLORS[3]];

/** 分区条形图默认色板（与饼图完整色板同源） */
export const HOME_SECTION_BAR_COLORS: readonly string[] = [...HOME_PIE_CHART_COLORS];

/**
 * 多序列趋势折线色板（基色四色 + 玫红备用）。
 */
export const HOME_TREND_LINE_COLORS: readonly string[] = [
  HOME_PIE_CHART_COLORS[0],
  HOME_PIE_CHART_COLORS[1],
  HOME_PIE_CHART_COLORS[3],
  HOME_PIE_CHART_COLORS[2],
  HOME_PIE_CHART_COLORS[5]
];

/**
 * 工单近七天趋势按后端稳定 code 配色（与 /dashboard/hq/home、/dashboard/service/home 的 series.code 对齐）。
 */
export const HOME_TREND_LINE_COLOR_BY_CODE: Record<string, string> = {
  FLOW_IN: HOME_PIE_CHART_COLORS[0],
  TECH_ACCEPT: HOME_PIE_CHART_COLORS[1],
  REPAIR_FINISH: HOME_PIE_CHART_COLORS[3],
  TRANSFER_OUT: HOME_PIE_CHART_COLORS[8]
};

/**
 * 作用：构建饼图区内标题配置（标题右对齐，与卡片右上角链接同侧）。
 * @param text - 图表标题文案
 * @returns ECharts title 配置片段
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function buildHomePieChartInlineTitle(text: string) {
  return {
    text,
    right: 16,
    textAlign: 'right' as const
  };
}

/** 多序列趋势图 grid（为图例与标题预留顶部空间） */
export const HOME_TREND_LINE_GRID = {
  left: '3%',
  right: '4%',
  top: 88,
  bottom: '8%',
  containLabel: true
} as const;

/** 单线面积图 grid */
export const HOME_SINGLE_LINE_GRID = {
  left: '3%',
  right: '4%',
  top: 48,
  bottom: '8%',
  containLabel: true
} as const;

/**
 * 作用：构建折线面积渐变（25% 处主色 → 底部白色）。
 * @param color - 折线主色
 * @returns ECharts areaStyle.color 线性渐变配置
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function buildLineAreaGradient(color: string) {
  return {
    type: 'linear' as const,
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0.25, color },
      { offset: 1, color: '#fff' }
    ]
  };
}

/**
 * 作用：解析趋势折线颜色；优先按后端稳定 code，否则按序列顺序取色板。
 * @param code - HomeTrendSeriesVO.code
 * @param index - 序列在 series 数组中的下标（无 code 映射时用于取模）
 * @returns 十六进制颜色值
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolveTrendLineColor(code: string | undefined, index: number): string {
  if (code && HOME_TREND_LINE_COLOR_BY_CODE[code]) {
    return HOME_TREND_LINE_COLOR_BY_CODE[code];
  }
  return HOME_TREND_LINE_COLORS[index % HOME_TREND_LINE_COLORS.length];
}

/** 趋势折线 series 入参（单对象入参以满足 max-params） */
export interface TrendLineSeriesItemInput {
  name: string;
  color: string;
  data: number[];
  /** 是否堆叠（仅遗留双指标 line-chart；接口 trend 禁止堆叠） */
  stacked?: boolean;
}

/**
 * 作用：构建趋势折线 ECharts series 项（含面积渐变、强调态）。
 * @param input - 序列名称、颜色、数据及是否堆叠
 * @returns ECharts line series 配置对象
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function buildTrendLineSeriesItem(input: TrendLineSeriesItemInput) {
  const { name, color, data, stacked } = input;
  const item = {
    name,
    type: 'line' as const,
    smooth: true,
    color,
    lineStyle: { width: 2, color },
    symbol: 'circle',
    symbolSize: 6,
    // 面积渐变与 git 首页折线视觉保持一致
    areaStyle: { color: buildLineAreaGradient(color) },
    emphasis: { focus: 'series' as const },
    data
  };
  if (stacked) {
    return { ...item, stack: 'Total' as const };
  }
  return item;
}

/** 图表区 grid（标题由 ACard 展示，顶部留白减小） */
export const HOME_SECTION_CHART_GRID = {
  left: '3%',
  right: '8%',
  top: 16,
  bottom: '6%',
  containLabel: true
} as const;

/**
 * 作用：按容器宽度解析饼图图例布局（宽屏右侧竖排，窄屏底部横排）。
 * @param containerWidth - 图表 DOM 宽度（px）
 * @returns ECharts legend 配置片段
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolvePieLegendLayout(containerWidth: number) {
  if (containerWidth >= 520) {
    return {
      orient: 'vertical' as const,
      right: '2%',
      top: 'middle',
      itemGap: 12,
      textStyle: { color: '#666', fontSize: 12 }
    };
  }
  return {
    bottom: '2%',
    left: 'center',
    itemGap: 10,
    textStyle: { color: '#666', fontSize: 12 }
  };
}

/**
 * 作用：解析饼图圆心位置（宽屏左移为右侧图例腾空间）。
 * @param containerWidth - 图表 DOM 宽度（px）
 * @returns ECharts center 百分比坐标
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolvePieCenter(containerWidth: number): [string, string] {
  return containerWidth >= 520 ? ['36%', '50%'] : ['50%', '44%'];
}

/**
 * 作用：解析饼图内外半径（随容器宽度切换大小）。
 * @param containerWidth - 图表 DOM 宽度（px）
 * @returns ECharts radius 百分比区间
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
export function resolvePieRadius(containerWidth: number): [string, string] {
  return containerWidth >= 520 ? ['46%', '72%'] : ['42%', '68%'];
}
