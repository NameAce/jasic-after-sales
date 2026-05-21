package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 服务主体首页概览卡片。
 *
 * <p>该对象只服务服务主体首页顶部概览区，
 * 不承载总部与平台特有指标，避免字段含义混杂。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "服务主体首页概览卡片")
@Data
public class ServiceDashboardOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活跃待办数。
     */
    @ApiModelProperty(value = "活跃待办数")
    private Long activeTodoCount;

    /**
     * 历史待办数。
     */
    @ApiModelProperty(value = "历史待办数")
    private Long historyTodoCount;

    /**
     * 当前首页视角下工单总数。
     */
    @ApiModelProperty(value = "当前首页视角下工单总数")
    private Long workOrderTotal;
}
