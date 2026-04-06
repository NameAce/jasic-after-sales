package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 代客户填写建维修订单参数。
 *
 * @author Codex
 * @date 2026/04/06
 */
@Data
public class WorkOrderProxyCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 客户姓名 */
    @NotBlank(message = "客户姓名不能为空")
    private String customerName;

    /** 客户手机号 */
    @NotBlank(message = "客户手机号不能为空")
    private String customerMobile;

    /** 机器条码 */
    @NotBlank(message = "机器条码不能为空")
    private String barcode;

    /** 服务方式（寄修/到店维修） */
    @NotBlank(message = "服务方式不能为空")
    private String serviceMode;

    /** 故障描述选项 */
    private List<String> faultItems;

    /** 故障备注 */
    private String faultRemark;

    /** 寄件人姓名 */
    private String senderName;

    /** 寄件人手机号 */
    private String senderMobile;

    /** 寄件地址 */
    private String senderAddress;

    /** 寄件快递单号 */
    private String sendExpressNo;
}
