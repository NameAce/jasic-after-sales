package com.jasic.aftersales.system.domain.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 总部网点工单内部查询参数。
 *
 * @author Codex
 * @date 2026/04/22
 */
@ApiModel(description = "总部网点工单内部查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderHqSiteInternalQuery extends WorkOrderScopedQuery {

    private static final long serialVersionUID = 1L;

    /** 承修方公司ID */
    @ApiModelProperty(value = "承修方公司ID")
    private Long siteCompanyId;

    /** 承修方公司名称（模糊） */
    @ApiModelProperty(value = "承修方公司名称（模糊）")
    private String siteName;

}
