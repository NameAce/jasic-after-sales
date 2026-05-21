/**
 * 首页看板接口：按登录主体类型分别调用平台 / 总部 / 服务主体首页聚合接口。
 * 与后端 `PlatformDashboardController`、`HqDashboardController`、`ServiceDashboardController` 契约对齐。
 */
import { request } from '../request';

/** 首页工单状态统计（固定字段，替代 status-count 列表二次映射） */
export interface DashboardWorkOrderStatusVO {
  all?: number;
  pendingAssign?: number;
  pendingTechAccept?: number;
  inProgress?: number;
  completed?: number;
  closed?: number;
}

/** 近七天趋势（后端已补齐空档日期） */
export interface DashboardTrend7dVO {
  dayKeys?: string[];
  createdWorkOrderCounts?: number[];
  activeTodoCounts?: number[];
}

/** 首页最新动态项 */
export interface DashboardHistoryTodoVO {
  id?: number;
  title?: string;
  summary?: string;
  bizType?: string;
  bizId?: number;
  routeType?: string;
  routeValue?: string;
  todoStatus?: string;
  createTime?: string;
}

/** 平台首页概览 */
export interface PlatformDashboardOverviewVO {
  companyTotal?: number;
  enabledCompanyTotal?: number;
  userTotal?: number;
  roleTotal?: number;
  notifySceneTotal?: number;
}

/** 主体类型分布 */
export interface DashboardSubjectTypeDistributionVO {
  platformCount?: number;
  hqCount?: number;
  serviceCount?: number;
}

/** 平台操作日志近七天趋势 */
export interface DashboardOperLogTrend7dVO {
  dayKeys?: string[];
  operLogCounts?: number[];
  failedCount?: number;
}

/** 平台首页 `/dashboard/platform/home` */
export interface PlatformDashboardHomeVO {
  overview?: PlatformDashboardOverviewVO;
  subjectTypeDistribution?: DashboardSubjectTypeDistributionVO;
  operLogTrend7d?: DashboardOperLogTrend7dVO;
}

/** 服务主体 / 总部首页概览（总部多 transferCount） */
export interface ServiceDashboardOverviewVO {
  activeTodoCount?: number;
  historyTodoCount?: number;
  workOrderTotal?: number;
}

export interface HqDashboardOverviewVO extends ServiceDashboardOverviewVO {
  transferCount?: number;
}

/** 总部网点汇总卡片 */
export interface DashboardSiteSummaryVO {
  siteCount?: number;
  totalCount?: number;
  waitAcceptCount?: number;
  inProgressCount?: number;
  completedCount?: number;
}

/** 总部网点待接单排行项 */
export interface DashboardSiteRankVO {
  siteCompanyId?: number;
  siteCompanyName?: string;
  waitAcceptCount?: number;
  totalCount?: number;
  inProgressCount?: number;
  completedCount?: number;
}

/** 服务主体首页 `/dashboard/service/home` */
export interface ServiceDashboardHomeVO {
  overview?: ServiceDashboardOverviewVO;
  workOrderStatus?: DashboardWorkOrderStatusVO;
  trend7d?: DashboardTrend7dVO;
  latestHistoryTodos?: DashboardHistoryTodoVO[];
}

/** 总部首页 `/dashboard/hq/home` */
export interface HqDashboardHomeVO {
  overview?: HqDashboardOverviewVO;
  workOrderStatus?: DashboardWorkOrderStatusVO;
  trend7d?: DashboardTrend7dVO;
  siteSummary?: DashboardSiteSummaryVO;
  siteWaitAcceptRank?: DashboardSiteRankVO[];
  latestHistoryTodos?: DashboardHistoryTodoVO[];
}

/** 查询平台超管首页总览 */
export function getPlatformDashboardHome() {
  return request<PlatformDashboardHomeVO>({
    url: '/dashboard/platform/home',
    method: 'get'
  });
}

/** 查询总部首页总览 */
export function getHqDashboardHome() {
  return request<HqDashboardHomeVO>({
    url: '/dashboard/hq/home',
    method: 'get'
  });
}

/** 查询服务主体（网点等）首页总览 */
export function getServiceDashboardHome() {
  return request<ServiceDashboardHomeVO>({
    url: '/dashboard/service/home',
    method: 'get'
  });
}
