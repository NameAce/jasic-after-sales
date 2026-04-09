package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 故障与维修配置新增修改参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "故障与维修配置新增修改参数")
@Data
public class FaultRepairConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID", required = true)
    @NotNull(message = "归属总部不能为空")
    private Long companyId;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 产品型号 */
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /** 状态 */
    @ApiModelProperty(value = "状态", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 故障配置项 */
    @ApiModelProperty(value = "故障配置项")
    @Valid
    private List<FaultRepairConfigFaultDTO> faults;
}
