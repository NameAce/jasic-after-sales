package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 工单派单参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderAssignDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 维修员ID */
    @NotNull(message = "维修员不能为空")
    private Long assignedUserId;
}
