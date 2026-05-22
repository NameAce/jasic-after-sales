package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页近七天事件趋势。
 *
 * <p>本轮首页趋势固定为近七天，并且全部按工单流转事件 create_time 统计。
 * 该对象不用于当前状态存量统计，因此趋势点不要求与任一状态列表 total 对齐。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "首页近七天事件趋势")
@Data
public class HomeTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 趋势标题。
     */
    @ApiModelProperty(value = "趋势标题")
    private String title;

    /**
     * 日期列表，格式 yyyy-MM-dd。
     */
    @ApiModelProperty(value = "日期列表")
    private List<String> days = new ArrayList<>();

    /**
     * 趋势序列列表。
     */
    @ApiModelProperty(value = "趋势序列列表")
    private List<HomeTrendSeriesVO> series = new ArrayList<>();
}
