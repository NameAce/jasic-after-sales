package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台首页总览返回结构。
 *
 * <p>该对象是 `/dashboard/platform/home` 的唯一返回契约，
 * 仅承载组织治理类统计，不混入工单业务统计。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "平台首页总览返回结构")
@Data
public class PlatformDashboardHomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 顶部概览区。
     */
    @ApiModelProperty(value = "顶部概览区")
    private PlatformDashboardOverviewVO overview;

    /**
     * 主体类型分布。
     */
    @ApiModelProperty(value = "主体类型分布")
    private DashboardSubjectTypeDistributionVO subjectTypeDistribution;

    /**
     * 操作日志近七天趋势。
     */
    @ApiModelProperty(value = "操作日志近七天趋势")
    private DashboardOperLogTrend7dVO operLogTrend7d;
}
