package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 工单关闭参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderCloseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 机器返回方式 */
    @NotBlank(message = "机器返回方式不能为空")
    private String returnMethod;

    /** 回寄快递单号 */
    private String returnExpressNo;

    /** 关闭原因 */
    @NotBlank(message = "关闭原因不能为空")
    private String closeReason;
}
