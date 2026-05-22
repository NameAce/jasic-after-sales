package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单状态统计视图
 *
 * @author Zoro
 * @date 2026/03/27
 */
@ApiModel(description = "工单状态统计视图")
@Data
public class WorkOrderStatusCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态编码 */
    @ApiModelProperty(value = "状态编码")
    private String mainStatus;

    /** 状态名称 */
    @ApiModelProperty(value = "状态名称")
    private String displayStatus;

    /** 数量 */
    @ApiModelProperty(value = "数量")
    private Long countNum;
}
