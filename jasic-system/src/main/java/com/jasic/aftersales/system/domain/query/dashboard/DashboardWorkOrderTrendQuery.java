package com.jasic.aftersales.system.domain.query.dashboard;

import com.jasic.aftersales.system.domain.query.WorkOrderScopedQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 首页工单趋势聚合查询参数。
 *
 * <p>该对象在复用工单现有权限上下文的基础上，
 * 额外补充首页趋势统计所需的时间窗口，避免继续复用分页列表结果做二次聚合。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "首页工单趋势聚合查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class DashboardWorkOrderTrendQuery extends WorkOrderScopedQuery {

    private static final long serialVersionUID = 1L;

    /**
     * 统计开始时间，含边界。
     */
    @ApiModelProperty(value = "统计开始时间，含边界")
    private LocalDateTime startTime;

    /**
     * 统计结束时间，不含边界。
     */
    @ApiModelProperty(value = "统计结束时间，不含边界")
    private LocalDateTime endTime;
}
