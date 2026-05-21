package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务主体首页总览返回结构。
 *
 * <p>该对象是 `/dashboard/service/home` 的唯一返回契约，
 * 用于替代前端拼装旧分页接口后的首页数据结构。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "服务主体首页总览返回结构")
@Data
public class ServiceDashboardHomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 顶部概览区。
     */
    @ApiModelProperty(value = "顶部概览区")
    private ServiceDashboardOverviewVO overview;

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
     * 最新历史待办列表。
     */
    @ApiModelProperty(value = "最新历史待办列表")
    private List<DashboardHistoryTodoVO> latestHistoryTodos = new ArrayList<>();
}
