/**
 * 首页 ECharts 通用视觉配置：卡片标题外置后图表区 grid / 图例 / 折线样式等复用。
 */

/** 折线主色（git platform-oper-log-chart 单线面积图） */
export const HOME_LINE_PRIMARY_COLOR = '#5da8ff';

/**
 * 多序列趋势折线色板（git line-chart.vue：蓝 / 紫 / 青 / 黄，并保留粉备用）。
 */
export const HOME_TREND_LINE_COLORS: readonly string[] = ['#5da8ff', '#8e9dff', '#26deca', '#fcbc25', '#ec4786'];

/**
 * 工单近七天趋势按后端稳定 code 配色（与 /dashboard/hq/home、/dashboard/service/home 的 series.code 对齐）。
 * 总部：FLOW_IN / REPAIR_FINISH / TRANSFER_OUT；网点：TECH_ACCEPT / REPAIR_FINISH / TRANSFER_OUT。
 */
export const HOME_TREND_LINE_COLOR_BY_CODE: Record<string, string> = {
  FLOW_IN: '#5da8ff',
  TECH_ACCEPT: '#8e9dff',
  REPAIR_FINISH: '#26deca',
  TRANSFER_OUT: '#fcbc25'
};

/**
 * 饼图区内标题：标题右对齐（与卡片右上角「当前工单」链接同侧）。
 */
export function buildHomePieChartInlineTitle(text: string) {
  return {
    text,
    right: 16,
    textAlign: 'right' as const
  };
}

/** 多序列趋势图 grid（git line-chart.vue，为图例与标题预留顶部空间） */
export const HOME_TREND_LINE_GRID = {
  left: '3%',
  right: '4%',
  top: 88,
  bottom: '8%',
  containLabel: true
} as const;

/** 单线面积图 grid（git platform-oper-log-chart） */
export const HOME_SINGLE_LINE_GRID = {
  left: '3%',
  right: '4%',
  top: 48,
  bottom: '8%',
  containLabel: true
} as const;

/**
 * 折线面积渐变（git platform-oper-log-chart / line-chart：25% 处主色 → 底部白色）。
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
 * 解析趋势折线颜色：优先按后端稳定 code，否则按序列顺序取色板。
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
 * 构建趋势折线 series。
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

/** 饼图宽屏时右侧图例，窄屏时底部图例 */
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

/** 饼图圆心：宽屏左移为右侧图例腾空间 */
export function resolvePieCenter(containerWidth: number): [string, string] {
  return containerWidth >= 520 ? ['36%', '50%'] : ['50%', '44%'];
}

/** 饼图半径 */
export function resolvePieRadius(containerWidth: number): [string, string] {
  return containerWidth >= 520 ? ['46%', '72%'] : ['42%', '68%'];
}
