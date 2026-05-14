package com.jasic.aftersales.customer.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端工单寄修信息参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单寄修信息参数")
@Data
public class CustomerWorkOrderSendInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 寄件人姓名 */
    @ApiModelProperty(value = "寄件人姓名", required = true)
    @NotBlank(message = "寄件人姓名不能为空")
    private String senderName;

    /** 寄件人手机号 */
    @ApiModelProperty(value = "寄件人手机号", required = true)
    @NotBlank(message = "寄件人手机号不能为空")
    private String senderMobile;

    /** 寄件地址 */
    @ApiModelProperty(value = "寄件地址", required = true)
    @NotBlank(message = "寄件地址不能为空")
    private String senderAddress;

    /** 寄件快递单号 */
    @ApiModelProperty(value = "寄件快递单号")
    private String sendExpressNo;

    /** 寄件凭证文件ID */
    @ApiModelProperty(value = "寄件凭证文件ID")
    private List<Long> senderVoucherFileIds;
}


