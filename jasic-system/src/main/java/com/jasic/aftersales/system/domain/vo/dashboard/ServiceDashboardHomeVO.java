package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 服务网点主体首页返回结构。
 *
 * <p>该对象是 `/dashboard/service/home` 的唯一返回契约。本轮服务网点首页命名为“服务工作台”，
 * 一级网点和二级网点统一使用同一结构，只返回当前服务公司承接工单、已转出、历史参与入口和近七天事件趋势。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "服务网点主体首页返回结构")
@Data
public class ServiceDashboardHomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 首页标题。
     */
    @ApiModelProperty(value = "首页标题")
    private String title;

    /**
     * 当前服务公司承接工单池。
     */
    @ApiModelProperty(value = "当前服务公司承接工单池")
    private HomeSectionVO currentPool;

    /**
     * 当前服务公司作为转出方的工单分区。
     */
    @ApiModelProperty(value = "当前服务公司作为转出方的工单分区")
    private HomeSectionVO transfer;

    /**
     * 历史参与入口。
     */
    @ApiModelProperty(value = "历史参与入口")
    private HomeEntryVO historyEntry;

    /**
     * 近七天事件趋势。
     */
    @ApiModelProperty(value = "近七天事件趋势")
    private HomeTrendVO trend;
}
