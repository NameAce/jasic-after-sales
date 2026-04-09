package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单维修登记参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单维修登记参数")
@Data
public class WorkOrderRepairDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 维修摘要 */
    @ApiModelProperty(value = "维修摘要")
    private String repairSummary;

    /** 维修说明 */
    @ApiModelProperty(value = "维修说明")
    private String repairDesc;

    /** 其他说明 */
    @ApiModelProperty(value = "其他说明")
    private String otherDesc;

    /** 调整后的报价金额 */
    @ApiModelProperty(value = "调整后的报价金额")
    private BigDecimal quoteAmount;

    /** 调整后的报价说明 */
    @ApiModelProperty(value = "调整后的报价说明")
    private String quoteDesc;

    /** 是否维修完成（1=是，0=否） */
    @ApiModelProperty(value = "是否维修完成（1=是，0=否）")
    private Integer isFinished;

    /** 故障点列表 */
    @ApiModelProperty(value = "故障点列表")
    @Valid
    private List<WorkOrderFaultItemDTO> faults;
}
