package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单转单参数
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单转单参数")
@Data
public class WorkOrderTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 目标公司ID */
    @ApiModelProperty(value = "目标公司ID", required = true)
    @NotNull(message = "目标公司不能为空")
    private Long targetCompanyId;

    /** 转单备注 */
    @ApiModelProperty(value = "转单备注")
    private String remark;
}
