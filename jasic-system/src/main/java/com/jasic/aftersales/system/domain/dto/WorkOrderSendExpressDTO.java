package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 工单上传寄件快递单号参数
 *
 * @author Codex
 * @date 2026/03/27
 */
@Data
public class WorkOrderSendExpressDTO {

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 寄件快递单号 */
    @NotBlank(message = "寄件快递单号不能为空")
    private String sendExpressNo;
}
