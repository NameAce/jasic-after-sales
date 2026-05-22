package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页入口项。
 *
 * <p>该对象用于“历史参与”等不作为核心 KPI 的入口。入口只提供标题和跳转目标，
 * 不提供数值，避免前端把入口误展示为核心指标。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "首页入口项")
@Data
public class HomeEntryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 入口标题。
     */
    @ApiModelProperty(value = "入口标题")
    private String title;

    /**
     * 入口说明。
     */
    @ApiModelProperty(value = "入口说明")
    private String description;

    /**
     * 点击跳转目标。
     */
    @ApiModelProperty(value = "点击跳转目标")
    private HomeRouteTargetVO routeTarget;
}
