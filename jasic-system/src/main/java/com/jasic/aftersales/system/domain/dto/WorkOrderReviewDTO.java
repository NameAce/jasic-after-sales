package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单复检参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单复检参数")
@Data
public class WorkOrderReviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 复检结果 */
    @ApiModelProperty(value = "复检结果", required = true)
    @NotBlank(message = "复检结果不能为空")
    private String reviewResult;

    /** 复检说明 */
    @ApiModelProperty(value = "复检说明")
    private String reviewDesc;

    /** 是否继续维修（1=是，0=否） */
    @ApiModelProperty(value = "是否继续维修（1=是，0=否）")
    private Integer isContinueRepair;
}
