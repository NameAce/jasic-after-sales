package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 总部首页总览返回结构。
 *
 * <p>该对象是 `/dashboard/hq/home` 的唯一返回契约，
 * 在服务主体首页基础上补充总部专属网点汇总与排行字段。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "总部首页总览返回结构")
@Data
public class HqDashboardHomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 顶部概览区。
     */
    @ApiModelProperty(value = "顶部概览区")
    private HqDashboardOverviewVO overview;

    /**
     * 工单状态统计。
     */
    @ApiModelProperty(value = "工单状态统计")
    private DashboardWorkOrderStatusVO workOrderStatus;

    /**
     * 近七天趋势。
     */
    @ApiModelProperty(value = "近七天趋势")
    private DashboardTrend7dVO trend7d;

    /**
     * 网点汇总卡片。
     */
    @ApiModelProperty(value = "网点汇总卡片")
    private DashboardSiteSummaryVO siteSummary;

    /**
     * 网点待接单排行。
     */
    @ApiModelProperty(value = "网点待接单排行")
    private List<DashboardSiteRankVO> siteWaitAcceptRank = new ArrayList<>();

    /**
     * 最新历史待办列表。
     */
    @ApiModelProperty(value = "最新历史待办列表")
    private List<DashboardHistoryTodoVO> latestHistoryTodos = new ArrayList<>();
}
