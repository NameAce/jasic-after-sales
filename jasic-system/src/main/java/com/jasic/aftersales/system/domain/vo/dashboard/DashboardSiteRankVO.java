package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 总部首页网点待接单排行项。
 *
 * <p>该对象服务总部首页排行区，
 * 用于明确表达“按待接单数排序”的首页语义。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "总部首页网点待接单排行项")
@Data
public class DashboardSiteRankVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 网点公司ID。
     */
    @ApiModelProperty(value = "网点公司ID")
    private Long siteCompanyId;

    /**
     * 网点公司名称。
     */
    @ApiModelProperty(value = "网点公司名称")
    private String siteCompanyName;

    /**
     * 待接单数。
     */
    @ApiModelProperty(value = "待接单数")
    private Long waitAcceptCount;

    /**
     * 工单总数。
     */
    @ApiModelProperty(value = "工单总数")
    private Long totalCount;

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
