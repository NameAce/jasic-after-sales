package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 总部网点工单只读列表查询参数。
 *
 * @author Codex
 * @date 2026/04/22
 */
@ApiModel(description = "总部网点工单只读列表查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderHqSiteOrderQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 承修方公司ID */
    @ApiModelProperty(value = "承修方公司ID")
    private Long siteCompanyId;

    /** 展示状态（ALL/WAIT_ACCEPT/IN_PROGRESS/COMPLETED/CLOSED） */
    @ApiModelProperty(value = "展示状态", allowableValues = "ALL,WAIT_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String displayStatus;

    /** 工单号（模糊） */
    @ApiModelProperty(value = "工单号（模糊）")
    private String orderNo;

    /** 客户姓名（模糊） */
    @ApiModelProperty(value = "客户姓名（模糊）")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /** 条码（模糊） */
    @ApiModelProperty(value = "条码（模糊）")
    private String barcode;

}
