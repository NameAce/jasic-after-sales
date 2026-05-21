package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台首页组织治理概览原始统计结果。
 *
 * <p>该对象用于承接平台首页聚合 SQL 的总览结果，
 * 供 Service 映射到平台首页返回结构。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "平台首页组织治理概览原始统计结果")
@Data
public class DashboardPlatformOverviewStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 公司总数。
     */
    @ApiModelProperty(value = "公司总数")
    private Long companyTotal;

    /**
     * 启用公司总数。
     */
    @ApiModelProperty(value = "启用公司总数")
    private Long enabledCompanyTotal;

    /**
     * 用户总数。
     */
    @ApiModelProperty(value = "用户总数")
    private Long userTotal;

    /**
     * 角色总数。
     */
    @ApiModelProperty(value = "角色总数")
    private Long roleTotal;

    /**
     * 通知场景总数。
     */
    @ApiModelProperty(value = "通知场景总数")
    private Long notifySceneTotal;
}
