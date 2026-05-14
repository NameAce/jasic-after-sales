package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 上游报修建维修订单参数。
 *
 * @author Codex
 * @date 2026/04/06
 */
@ApiModel(description = "上游报修建维修订单参数。")
@Data
public class WorkOrderUpstreamCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标受理公司ID */
    @ApiModelProperty(value = "目标受理公司ID")
    private Long targetCompanyId;

    /** 客户姓名 */
    @ApiModelProperty(value = "客户姓名")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /** 机器条码 */
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /** 服务方式编码（MAIL/STORE） */
    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE", required = true)
    @NotBlank(message = "服务方式不能为空")
    private String serviceMode;

    /** 故障描述选项 */
    @ApiModelProperty(value = "故障描述选项")
    private List<String> faultItems;

    /** 故障备注 */
    @ApiModelProperty(value = "故障备注")
    private String faultRemark;

    /** 故障图片文件ID */
    @ApiModelProperty(value = "故障图片文件ID")
    private List<Long> faultImageFileIds;

    /** 故障视频文件ID */
    @ApiModelProperty(value = "故障视频文件ID")
    private List<Long> faultVideoFileIds;

    /** 故障语音文件ID */
    @ApiModelProperty(value = "故障语音文件ID")
    private List<Long> faultVoiceFileIds;

    /** 寄件人姓名 */
    @ApiModelProperty(value = "寄件人姓名")
    private String senderName;

    /** 寄件人手机号 */
    @ApiModelProperty(value = "寄件人手机号")
    private String senderMobile;

    /** 寄件地址 */
    @ApiModelProperty(value = "寄件地址")
    private String senderAddress;

    /** 寄件快递单号 */
    @ApiModelProperty(value = "寄件快递单号")
    private String sendExpressNo;

    /** 寄件凭证文件ID */
    @ApiModelProperty(value = "寄件凭证文件ID")
    private List<Long> senderVoucherFileIds;
}


