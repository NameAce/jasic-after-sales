package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单上传寄件快递单号参数
 *
 * @author Codex
 * @date 2026/03/27
 */
@ApiModel(description = "工单上传寄件快递单号参数")
@Data
public class WorkOrderSendExpressDTO {

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 寄件快递单号 */
    @ApiModelProperty(value = "寄件快递单号", required = true)
    @NotBlank(message = "寄件快递单号不能为空")
    private String sendExpressNo;

    /** 寄件凭证文件ID */
    @ApiModelProperty(value = "寄件凭证文件ID")
    private List<Long> senderVoucherFileIds;
}
