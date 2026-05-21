package com.jasic.aftersales.system.domain.query.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台首页操作日志聚合查询参数。
 *
 * <p>该对象只用于平台首页近七天日志趋势与失败数统计，
 * 不向前端暴露，也不承载列表页分页筛选语义。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "平台首页操作日志聚合查询参数")
@Data
public class DashboardOperLogQuery implements Serializable {

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
