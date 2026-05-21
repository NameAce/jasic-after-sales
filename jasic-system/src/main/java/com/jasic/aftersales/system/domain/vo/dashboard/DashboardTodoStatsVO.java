package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页待办概览统计结果。
 *
 * <p>该对象只用于承接待办聚合 SQL 的原始结果，
 * 最终仍由各主体首页 VO 映射成业务字段。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "首页待办概览统计结果")
@Data
public class DashboardTodoStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 活跃待办数，口径为 PENDING + READ。
     */
    @ApiModelProperty(value = "活跃待办数")
    private Long activeTodoCount;

    /**
     * 历史待办数，口径为 DONE + INVALID。
     */
    @ApiModelProperty(value = "历史待办数")
    private Long historyTodoCount;
}
