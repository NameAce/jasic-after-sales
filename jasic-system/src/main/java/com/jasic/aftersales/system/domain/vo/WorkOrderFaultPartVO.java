package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单故障点配件明细视图
 *
 * @author Codex
 * @date 2026/04/15
 */
@ApiModel(description = "工单故障点配件明细视图")
@Data
public class WorkOrderFaultPartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配件明细ID */
    @ApiModelProperty(value = "配件明细ID")
    private Long id;

    /** 配件名称 */
    @ApiModelProperty(value = "配件名称")
    private String partName;

    /** 配件数量 */
    @ApiModelProperty(value = "配件数量")
    private Integer partQty;

    /** 排序号 */
    @ApiModelProperty(value = "排序号")
    private Integer sortNum;
}
