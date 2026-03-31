package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 工单维修登记参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderRepairDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 维修摘要 */
    private String repairSummary;

    /** 维修说明 */
    private String repairDesc;

    /** 其他说明 */
    private String otherDesc;

    /** 是否维修完成（1=是，0=否） */
    private Integer isFinished;

    /** 故障点列表 */
    private List<WorkOrderFaultItemDTO> faults;
}
