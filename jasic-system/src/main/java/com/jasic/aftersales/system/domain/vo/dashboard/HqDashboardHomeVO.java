package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 总部主体首页返回结构。
 *
 * <p>该对象是 `/dashboard/hq/home` 的唯一返回契约。本轮总部首页命名为“调度看板”，
 * 只返回总部当前承接工单池、已转出和近七天事件趋势，不再包含我的事项、网点履约监控、SLA 或风险指标。</p>
 *
 * @author Codex
 * @date 2026/05/21
 */
@ApiModel(description = "总部主体首页返回结构")
@Data
public class HqDashboardHomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 首页标题。
     */
    @ApiModelProperty(value = "首页标题")
    private String title;

    /**
     * 当前总部承接工单池。
     */
    @ApiModelProperty(value = "当前总部承接工单池")
    private HomeSectionVO workOrderPool;

    /**
     * 当前总部作为转出方的工单分区。
     */
    @ApiModelProperty(value = "当前总部作为转出方的工单分区")
    private HomeSectionVO transfer;

    /**
     * 近七天事件趋势。
     */
    @ApiModelProperty(value = "近七天事件趋势")
    private HomeTrendVO trend;
}
