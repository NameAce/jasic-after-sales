package com.jasic.aftersales.customer.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * C端工单寄修信息参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class CustomerWorkOrderSendInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 寄件人姓名 */
    @NotBlank(message = "寄件人姓名不能为空")
    private String senderName;

    /** 寄件人手机号 */
    @NotBlank(message = "寄件人手机号不能为空")
    private String senderMobile;

    /** 寄件地址 */
    @NotBlank(message = "寄件地址不能为空")
    private String senderAddress;

    /** 寄件快递单号 */
    private String sendExpressNo;
}
