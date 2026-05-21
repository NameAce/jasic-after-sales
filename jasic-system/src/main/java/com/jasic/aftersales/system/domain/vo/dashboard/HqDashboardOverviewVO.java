package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 总部首页概览卡片。
 *
 * <p>该对象在服务主体概览指标基础上，
 * 补充总部独有的转单工单总数。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "总部首页概览卡片")
@Data
public class HqDashboardOverviewVO implements Serializable {

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
     * 总部首页视角下工单总数。
     */
    @ApiModelProperty(value = "总部首页视角下工单总数")
    private Long workOrderTotal;

    /**
     * 总部首页视角下转单工单总数。
     */
    @ApiModelProperty(value = "总部首页视角下转单工单总数")
    private Long transferCount;
}
