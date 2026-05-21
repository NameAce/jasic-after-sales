package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 总部首页网点汇总卡片。
 *
 * <p>该对象对总部网点汇总结果再次收敛，
 * 方便前端直接渲染卡片，不再自行汇总所有网点列表。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "总部首页网点汇总卡片")
@Data
public class DashboardSiteSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 网点数量。
     */
    @ApiModelProperty(value = "网点数量")
    private Long siteCount;

    /**
     * 工单总数。
     */
    @ApiModelProperty(value = "工单总数")
    private Long totalCount;

    /**
     * 待接单数。
     */
    @ApiModelProperty(value = "待接单数")
    private Long waitAcceptCount;

    /**
     * 处理中数。
     */
    @ApiModelProperty(value = "处理中数")
    private Long inProgressCount;

    /**
     * 已完成数。
     */
    @ApiModelProperty(value = "已完成数")
    private Long completedCount;
}
