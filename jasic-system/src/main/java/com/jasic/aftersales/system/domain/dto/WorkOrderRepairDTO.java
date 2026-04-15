package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

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

    /** 调整后的报价金额 */
    @ApiModelProperty(value = "调整后的报价金额")
    private BigDecimal quoteAmount;

    /** 调整后的报价说明 */
    @ApiModelProperty(value = "调整后的报价说明")
    private String quoteDesc;

    /** 手工填写的维修说明 */
    @ApiModelProperty(value = "手工填写的维修说明")
    private String repairDesc;

    /** 维修说明选项 */
    @ApiModelProperty(value = "维修说明选项")
    private List<String> repairItems;

    /** 其他维修说明 */
    @ApiModelProperty(value = "其他维修说明")
    private String otherDesc;

    /** 配件名称 */
    @ApiModelProperty(value = "配件名称")
    private String partName;

    /** 配件数量 */
    @ApiModelProperty(value = "配件数量")
    private Integer partQty;

    /** 故障处旧图片文件ID */
    @ApiModelProperty(value = "故障处旧图片文件ID")
    private List<Long> faultOldImageFileIds;

    /** 故障处新图片文件ID */
    @ApiModelProperty(value = "故障处新图片文件ID")
    private List<Long> faultNewImageFileIds;

    /** 机器正面照片文件ID */
    @ApiModelProperty(value = "机器正面照片文件ID")
    private List<Long> machineImageFileIds;

    /** 机器条码照片文件ID */
    @ApiModelProperty(value = "机器条码照片文件ID")
    private List<Long> machineBarcodeImageFileIds;

    /** 其他图片文件ID */
    @ApiModelProperty(value = "其他图片文件ID")
    private List<Long> otherImageFileIds;
}
