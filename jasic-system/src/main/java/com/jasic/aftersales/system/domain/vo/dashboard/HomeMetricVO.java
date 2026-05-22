package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页指标卡片。
 *
 * <p>该对象统一承载平台治理指标与工单指标。工单指标必须同时返回统计说明、跳转目标和列表查询说明，
 * 便于验收时确认“首页数字 = 点击后的列表 total”。平台治理指标同样可以返回 routeTarget，
 * 但不需要依赖工单列表口径。</p>
 *
 * @author Codex
 * @date 2026/05/21
 */
@ApiModel(description = "首页指标卡片")
@Data
public class HomeMetricVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 指标编码。
     *
     * <p>编码用于前端稳定渲染和测试定位，不作为业务筛选条件。</p>
     */
    @ApiModelProperty(value = "指标编码")
    private String code;

    /**
     * 指标标题。
     */
    @ApiModelProperty(value = "指标标题")
    private String title;

    /**
     * 指标数值。
     */
    @ApiModelProperty(value = "指标数值")
    private Long value;

    /**
     * 数值单位。
     */
    @ApiModelProperty(value = "数值单位")
    private String unit;

    /**
     * 统计条件说明。
     *
     * <p>工单指标必须写清当前主体、当前承接或转出方、主状态等业务条件。</p>
     */
    @ApiModelProperty(value = "统计条件说明")
    private String statCondition;

    /**
     * 列表查询条件说明。
     *
     * <p>工单指标必须写清点击后列表后端接收到的查询条件，便于和列表 total 做同条件校验。</p>
     */
    @ApiModelProperty(value = "列表查询条件说明")
    private String listQueryCondition;

    /**
     * 点击跳转目标。
     *
     * <p>为空时前端仅展示指标，不做跳转。</p>
     */
    @ApiModelProperty(value = "点击跳转目标")
    private HomeRouteTargetVO routeTarget;
}
