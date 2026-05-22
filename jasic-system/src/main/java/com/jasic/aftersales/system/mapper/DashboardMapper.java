package com.jasic.aftersales.system.mapper;

import com.jasic.aftersales.system.domain.query.dashboard.DashboardWorkOrderTrendQuery;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardCountByDayVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardPlatformGovernanceStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 三类主体首页专用聚合 Mapper。
 *
 * <p>该 Mapper 只承载本轮首页重构需要的聚合 SQL：
 * 平台治理看板的组织、账号、基础配置统计，以及总部/服务网点近七天工单事件趋势。
 * 工单卡片存量数字不在这里另写 SQL，而是由 Service 复用工单列表同源的 countByStatus 能力，
 * 确保首页数字和列表 total 可以按同条件对齐。</p>
 *
 * @author Codex
 * @date 2026/05/21
 */
@Mapper
public interface DashboardMapper {

    /**
     * 统计平台治理看板三块指标。
     *
     * @return 平台治理看板原始统计结果
     */
    DashboardPlatformGovernanceStatsVO selectPlatformGovernanceStats();

    /**
     * 统计近七天流入当前主体承接池的工单事件。
     *
     * <p>总部用于“流入总部”，服务网点用于“流入服务公司”。事件时间取工单流水 create_time。</p>
     *
     * @param query 趋势查询参数，包含当前登录公司、数据权限上下文和近七天时间窗口
     * @return 按天聚合结果
     */
    List<DashboardCountByDayVO> selectFlowInTrend(@Param("query") DashboardWorkOrderTrendQuery query);

    /**
     * 统计近七天当前主体维修员接单事件。
     *
     * <p>该趋势只服务服务工作台，按 TECH_ACCEPT 流水事件发生时间聚合。</p>
     *
     * @param query 趋势查询参数，包含当前登录公司、数据权限上下文和近七天时间窗口
     * @return 按天聚合结果
     */
    List<DashboardCountByDayVO> selectTechAcceptTrend(@Param("query") DashboardWorkOrderTrendQuery query);

    /**
     * 统计近七天当前主体完成维修事件。
     *
     * <p>该趋势按 REPAIR_FINISH 流水事件发生时间聚合，不按当前主状态存量统计。</p>
     *
     * @param query 趋势查询参数，包含当前登录公司、数据权限上下文和近七天时间窗口
     * @return 按天聚合结果
     */
    List<DashboardCountByDayVO> selectRepairFinishTrend(@Param("query") DashboardWorkOrderTrendQuery query);

    /**
     * 统计近七天当前主体作为转出方的转单事件。
     *
     * <p>该趋势以 work_order_flow.from_company_id = 当前登录公司为准，
     * 不使用 work_order.has_transfer，避免把当前主体不是转出方的工单误计入。</p>
     *
     * @param query 趋势查询参数，包含当前登录公司、数据权限上下文和近七天时间窗口
     * @return 按天聚合结果
     */
    List<DashboardCountByDayVO> selectTransferOutTrend(@Param("query") DashboardWorkOrderTrendQuery query);
}
