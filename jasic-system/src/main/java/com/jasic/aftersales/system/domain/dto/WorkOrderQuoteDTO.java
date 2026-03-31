package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工单报价参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderQuoteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 故障判定 */
    private String faultJudge;

    /** 报价金额 */
    private BigDecimal quoteAmount;

    /** 报价说明 */
    private String quoteDesc;
}
