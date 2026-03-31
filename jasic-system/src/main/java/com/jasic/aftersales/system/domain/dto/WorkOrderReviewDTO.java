package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 工单复检参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderReviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 复检结果 */
    private String reviewResult;

    /** 复检说明 */
    private String reviewDesc;

    /** 是否继续维修（1=是，0=否） */
    private Integer isContinueRepair;
}
