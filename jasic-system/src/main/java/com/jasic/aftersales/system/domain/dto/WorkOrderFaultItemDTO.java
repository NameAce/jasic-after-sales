package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单故障点参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单故障点参数")
@Data
public class WorkOrderFaultItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障描述 */
    @ApiModelProperty(value = "故障描述", required = true)
    @NotBlank(message = "故障描述不能为空")
    private String faultDesc;

    /** 维修说明 */
    @ApiModelProperty(value = "维修说明")
    private String repairDesc;

    /** 维修说明选项 */
    @ApiModelProperty(value = "维修说明选项")
    private List<String> repairItems;

    /** 其他维修说明 */
    @ApiModelProperty(value = "其他维修说明")
    private String otherDesc;

    /** 配件信息 */
    @ApiModelProperty(value = "配件信息")
    private String partDesc;

    /** 图片地址集合 */
    @ApiModelProperty(value = "图片地址集合")
    private String imageUrls;
}
