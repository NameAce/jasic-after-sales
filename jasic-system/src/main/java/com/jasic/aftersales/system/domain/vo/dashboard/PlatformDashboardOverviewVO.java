package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台首页概览卡片。
 *
 * <p>该对象只承载平台治理看板顶部指标，
 * 不混入工单领域或总部网点领域的业务统计。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "平台首页概览卡片")
@Data
public class PlatformDashboardOverviewVO implements Serializable {

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
