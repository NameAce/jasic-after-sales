package com.jasic.aftersales.system.domain.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 总部网点工单汇总查询参数。
 *
 * @author Zoro
 * @date 2026/04/22
 */
@ApiModel(description = "总部网点工单汇总查询参数")
@Data
public class WorkOrderHqSiteSummaryQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 承修方公司名称（模糊） */
    @ApiModelProperty(value = "承修方公司名称（模糊）")
    private String siteName;

}
