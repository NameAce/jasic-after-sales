package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单故障点配件明细参数
 *
 * @author Zoro
 * @date 2026/04/15
 */
@ApiModel(description = "工单故障点配件明细参数")
@Data
public class WorkOrderFaultPartItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配件名称 */
    @ApiModelProperty(value = "配件名称")
    private String partName;

    /** 配件数量 */
    @ApiModelProperty(value = "配件数量")
    private Integer partQty;
}
