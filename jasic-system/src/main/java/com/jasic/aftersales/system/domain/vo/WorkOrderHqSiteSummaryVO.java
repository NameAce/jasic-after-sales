package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 总部网点工单汇总视图。
 *
 * @author Zoro
 * @date 2026/04/22
 */
@ApiModel(description = "总部网点工单汇总视图")
@Data
public class WorkOrderHqSiteSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 承修方公司ID */
    @ApiModelProperty(value = "承修方公司ID")
    private Long siteCompanyId;

    /** 承修方公司名称 */
    @ApiModelProperty(value = "承修方公司名称")
    private String siteCompanyName;

    /** 总工单数，包含已关闭 */
    @ApiModelProperty(value = "总工单数，包含已关闭")
    private Long totalCount;

    /** 待接单数（待派单 + 待维修员接单） */
    @ApiModelProperty(value = "待接单数（待派单 + 待维修员接单）")
    private Long waitAcceptCount;

    /** 维修中数量 */
    @ApiModelProperty(value = "维修中数量")
    private Long inProgressCount;

    /** 已完成数量，不包含已关闭 */
    @ApiModelProperty(value = "已完成数量，不包含已关闭")
    private Long completedCount;
}
