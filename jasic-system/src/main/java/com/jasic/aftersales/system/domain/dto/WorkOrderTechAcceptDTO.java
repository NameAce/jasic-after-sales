package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单维修员接单参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单维修员接单参数")
@Data
public class WorkOrderTechAcceptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 故障判定 */
    @ApiModelProperty(value = "故障判定", allowableValues = "有故障,无故障", required = true)
    @NotBlank(message = "故障判定不能为空")
    private String faultJudge;

    /** 报价金额 */
    @ApiModelProperty(value = "报价金额")
    private BigDecimal quoteAmount;

    /** 报价说明 */
    @ApiModelProperty(value = "报价说明")
    private String quoteDesc;

    /** 机器返回方式 */
    @ApiModelProperty(value = "机器返回方式", allowableValues = "回寄,自提")
    private String returnMethod;

    /** 回寄快递单号 */
    @ApiModelProperty(value = "回寄快递单号")
    private String returnExpressNo;

    /** 回寄凭证文件ID */
    @ApiModelProperty(value = "回寄凭证文件ID")
    private List<Long> returnVoucherFileIds;

    /** 关闭原因 */
    @ApiModelProperty(value = "关闭原因")
    private String closeReason;
}


