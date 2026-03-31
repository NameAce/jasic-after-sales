package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 工单转单参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 目标公司ID */
    @NotNull(message = "目标公司不能为空")
    private Long targetCompanyId;

    /** 转单备注 */
    private String remark;
}
