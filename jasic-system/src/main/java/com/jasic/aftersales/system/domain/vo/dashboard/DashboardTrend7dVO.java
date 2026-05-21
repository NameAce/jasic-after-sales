package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端首页近七天趋势结构。
 *
 * <p>该对象统一承载工单建单趋势与活跃待办生成趋势，
 * 由后端保证始终返回完整七天数组，避免前端再补齐空档日期。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "服务端首页近七天趋势结构")
@Data
public class DashboardTrend7dVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期键列表，格式为 yyyy-MM-dd。
     */
    @ApiModelProperty(value = "日期键列表")
    private List<String> dayKeys = new ArrayList<>();

    /**
     * 近七天建单量趋势。
     */
    @ApiModelProperty(value = "近七天建单量趋势")
    private List<Long> createdWorkOrderCounts = new ArrayList<>();

    /**
     * 近七天活跃待办生成趋势。
     */
    @ApiModelProperty(value = "近七天活跃待办生成趋势")
    private List<Long> activeTodoCounts = new ArrayList<>();
}
