package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 工单复检参数
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单复检参数")
@Data
public class WorkOrderReviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 手工填写的维修说明 */
    @ApiModelProperty(value = "手工填写的维修说明")
    private String repairDesc;

    /** 维修说明选项 */
    @ApiModelProperty(value = "维修说明选项")
    private List<String> repairItems;

    /** 其他维修说明 */
    @ApiModelProperty(value = "其他维修说明")
    private String otherDesc;

    /** 配件明细列表 */
    @ApiModelProperty(value = "配件明细列表")
    private List<WorkOrderFaultPartItemDTO> partList;

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
