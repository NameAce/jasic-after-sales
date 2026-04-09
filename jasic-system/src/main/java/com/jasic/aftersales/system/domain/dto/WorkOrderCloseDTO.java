package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单关闭参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单关闭参数")
@Data
public class WorkOrderCloseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 机器返回方式 */
    @ApiModelProperty(value = "机器返回方式", allowableValues = "回寄,自提", required = true)
    @NotBlank(message = "机器返回方式不能为空")
    private String returnMethod;

    /** 回寄快递单号 */
    @ApiModelProperty(value = "回寄快递单号")
    private String returnExpressNo;

    /** 回寄凭证文件ID */
    @ApiModelProperty(value = "回寄凭证文件ID")
    private List<Long> returnVoucherFileIds;

    /** 关闭原因 */
    @ApiModelProperty(value = "关闭原因", required = true)
    @NotBlank(message = "关闭原因不能为空")
    private String closeReason;
}
