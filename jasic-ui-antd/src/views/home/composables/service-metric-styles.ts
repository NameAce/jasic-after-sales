/**
 * 服务主体首页指标卡片样式：与总部/网点工单 KPI 共用 WORK_ORDER 配色，避免重复维护两套色板。
 * @修改人 黄碧莲
 * @修改时间 2026-05-22
 */
import { DEFAULT_HOME_METRIC_STYLE, type HomeMetricCardStyle, WORK_ORDER_METRIC_STYLES } from './home-metric-styles';

/** 服务主体指标卡片默认样式（无 code 映射时兜底） */
export const DEFAULT_SERVICE_METRIC_STYLE: HomeMetricCardStyle = DEFAULT_HOME_METRIC_STYLE;

/** 服务主体承接池 / 已转出等指标 code → 渐变与图标（与 WORK_ORDER_METRIC_STYLES 同源） */
export const SERVICE_METRIC_CARD_STYLES: Record<string, HomeMetricCardStyle> = WORK_ORDER_METRIC_STYLES;
