package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工单维修故障选项视图
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class WorkOrderRepairFaultOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障描述 */
    private String faultDesc;

    /** 维修说明选项 */
    private List<String> repairOptions;
}
