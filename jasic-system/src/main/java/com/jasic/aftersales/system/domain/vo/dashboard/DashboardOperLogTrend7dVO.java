package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台首页操作日志近七天趋势。
 *
 * <p>该对象统一承载操作日志近七天趋势与失败数，
 * 用于替代前端从日志分页列表中截断后再手工统计的实现。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "平台首页操作日志近七天趋势")
@Data
public class DashboardOperLogTrend7dVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期键列表。
     */
    @ApiModelProperty(value = "日期键列表")
    private List<String> dayKeys = new ArrayList<>();

    /**
     * 日志数量趋势。
     */
    @ApiModelProperty(value = "日志数量趋势")
    private List<Long> operLogCounts = new ArrayList<>();

    /**
     * 近七天失败数。
     */
    @ApiModelProperty(value = "近七天失败数")
    private Long failedCount;
}
