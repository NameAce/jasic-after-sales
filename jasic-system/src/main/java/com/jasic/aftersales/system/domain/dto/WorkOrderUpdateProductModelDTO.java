package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 工单补录机器型号参数
 *
 * @author Zoro
 * @date 2026/04/16
 */
@ApiModel(description = "工单补录机器型号参数")
@Data
public class WorkOrderUpdateProductModelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 机器型号 */
    @ApiModelProperty(value = "机器型号", required = true)
    @NotBlank(message = "机器型号不能为空")
    private String productModel;
}
