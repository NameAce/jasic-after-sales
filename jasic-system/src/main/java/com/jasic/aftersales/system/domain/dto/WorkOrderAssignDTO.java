package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单派单参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单派单参数")
@Data
public class WorkOrderAssignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 维修员ID */
    @ApiModelProperty(value = "维修员ID", required = true)
    @NotNull(message = "维修员不能为空")
    private Long assignedUserId;
}
