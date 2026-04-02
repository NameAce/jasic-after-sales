package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 故障与维修配置新增修改参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class FaultRepairConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 归属总部ID */
    @NotNull(message = "归属总部不能为空")
    private Long companyId;

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 状态 */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 备注 */
    private String remark;

    /** 故障配置项 */
    @Valid
    private List<FaultRepairConfigFaultDTO> faults;
}
