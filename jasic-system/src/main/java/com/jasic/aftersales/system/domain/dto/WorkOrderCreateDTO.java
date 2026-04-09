package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单创建参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单创建参数")
@Data
public class WorkOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 客户ID（为空时按手机号自动匹配或创建） */
    @ApiModelProperty(value = "客户ID（为空时按手机号自动匹配或创建）")
    private Long customerId;

    /** 客户姓名 */
    @ApiModelProperty(value = "客户姓名", required = true)
    @NotBlank(message = "客户姓名不能为空")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号", required = true)
    @NotBlank(message = "客户手机号不能为空")
    private String customerMobile;

    /** 条码 */
    @ApiModelProperty(value = "条码")
    private String barcode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 机器型号 */
    @ApiModelProperty(value = "机器型号")
    private String productModel;

    /** 品牌编码 */
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /** 服务方式编码（MAIL/STORE） */
    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE", required = true)
    @NotBlank(message = "服务方式不能为空")
    private String serviceMode;

    /** 质保状态 */
    @ApiModelProperty(value = "质保状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    /** 故障描述 */
    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

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

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID", required = true)
    @NotNull(message = "归属总部不能为空")
    private Long hqCompanyId;
}
