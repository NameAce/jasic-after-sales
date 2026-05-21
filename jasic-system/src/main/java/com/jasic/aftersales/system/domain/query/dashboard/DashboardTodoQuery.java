package com.jasic.aftersales.system.domain.query.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页待办聚合查询参数。
 *
 * <p>该对象仅服务首页专用聚合 SQL，
 * 用于约束当前登录人的待办统计范围、趋势时间窗口和列表截断条数。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "首页待办聚合查询参数")
@Data
public class DashboardTodoQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前登录用户ID。
     */
    @ApiModelProperty(value = "当前登录用户ID")
    private Long receiverId;

    /**
     * 当前登录公司ID。
     */
    @ApiModelProperty(value = "当前登录公司ID")
    private Long receiverCompanyId;

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
