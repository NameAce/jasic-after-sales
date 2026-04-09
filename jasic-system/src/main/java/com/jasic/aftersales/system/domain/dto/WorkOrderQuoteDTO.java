package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单报价参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单报价参数")
@Data
public class WorkOrderQuoteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 故障判定 */
    @ApiModelProperty(value = "故障判定", required = true)
    @NotBlank(message = "故障判定不能为空")
    private String faultJudge;

    /** 报价金额 */
    @ApiModelProperty(value = "报价金额")
    private BigDecimal quoteAmount;

    /** 报价说明 */
    @ApiModelProperty(value = "报价说明")
    private String quoteDesc;
}
