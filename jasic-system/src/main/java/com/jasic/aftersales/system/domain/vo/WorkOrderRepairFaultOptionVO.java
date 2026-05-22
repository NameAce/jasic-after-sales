package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单维修故障选项视图
 *
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "工单维修故障选项视图")
@Data
public class WorkOrderRepairFaultOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障描述 */
    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    /** 维修说明选项 */
    @ApiModelProperty(value = "维修说明选项")
    private List<String> repairOptions;
}
