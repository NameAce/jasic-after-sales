package com.jasic.aftersales.system.mapper;

import com.jasic.aftersales.system.domain.query.dashboard.DashboardOperLogQuery;
import com.jasic.aftersales.system.domain.query.dashboard.DashboardTodoQuery;
import com.jasic.aftersales.system.domain.query.dashboard.DashboardWorkOrderTrendQuery;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardCountByDayVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardPlatformOverviewStatsVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardSubjectTypeCountVO;
import com.jasic.aftersales.system.domain.vo.dashboard.DashboardTodoStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 首页专用聚合 Mapper。
 *
 * <p>该 Mapper 只承载首页总览、趋势、平台治理分布等专用聚合 SQL，
 * 不复用分页列表 SQL 做内存统计，也不影响现有列表页接口契约。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Mapper
public interface DashboardMapper {

    /**
     * 统计当前登录人的活跃待办数与历史待办数。
     *
     * @param query 待办聚合查询参数
     * @return 待办概览统计结果
     */
    DashboardTodoStatsVO selectTodoStats(@Param("query") DashboardTodoQuery query);

    /**
     * 统计当前登录人近七天活跃待办生成趋势。
     *
     * @param query 待办聚合查询参数
     * @return 按天计数结果
     */
    List<DashboardCountByDayVO> selectActiveTodoTrend(@Param("query") DashboardTodoQuery query);

    /**
     * 统计当前工单权限视角下近七天建单趋势。
     *
     * @param query 工单趋势查询参数
     * @return 按天计数结果
     */
    List<DashboardCountByDayVO> selectCreatedWorkOrderTrend(@Param("query") DashboardWorkOrderTrendQuery query);

    /**
     * 统计平台首页组织治理概览。
     *
     * @return 平台组织治理概览
     */
    DashboardPlatformOverviewStatsVO selectPlatformOverviewStats();

    /**
     * 统计平台首页主体类型分布。
     *
     * @return 主体类型分组计数结果
     */
    List<DashboardSubjectTypeCountVO> selectSubjectTypeDistribution();

    /**
     * 统计平台首页近七天操作日志趋势。
     *
     * @param query 操作日志聚合查询参数
     * @return 按天计数结果
     */
    List<DashboardCountByDayVO> selectOperLogTrend(@Param("query") DashboardOperLogQuery query);

    /**
     * 统计平台首页近七天失败操作日志数。
     *
     * @param query 操作日志聚合查询参数
     * @return 失败数
     */
    Long selectOperLogFailedCount(@Param("query") DashboardOperLogQuery query);
}
