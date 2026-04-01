package com.jasic.aftersales.customer.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * C端工单创建参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class CustomerWorkOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报修人姓名 */
    @NotBlank(message = "报修人姓名不能为空")
    private String customerName;

    /** 机器条码，当前 MVP 仅支持有条码报修 */
    @NotBlank(message = "机器条码不能为空")
    private String barcode;

    /** 物料编码 */
    private String productCode;

    /** 机器型号 */
    private String productModel;

    /** 品牌编码 */
    private String brandCode;

    /** 服务方式（寄修/到店维修） */
    @NotBlank(message = "服务方式不能为空")
    private String serviceMode;

    /** 质保状态 */
    private String warrantyStatus;

    /** 报修描述 */
    private String faultDesc;

    /** 目标服务网点ID */
    @NotNull(message = "服务网点不能为空")
    private Long serviceCompanyId;

    /** 归属总部ID */
    @NotNull(message = "归属总部不能为空")
    private Long hqCompanyId;

    /** 寄件人姓名 */
    private String senderName;

    /** 寄件人手机号 */
    private String senderMobile;

    /** 寄件地址 */
    private String senderAddress;

    /** 寄件快递单号 */
    private String sendExpressNo;
}
