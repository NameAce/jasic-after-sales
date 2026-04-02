package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 故障与维修配置故障项参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class FaultRepairConfigFaultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障描述 */
    private String faultDesc;

    /** 维修说明列表 */
    private List<String> repairOptions;
}
