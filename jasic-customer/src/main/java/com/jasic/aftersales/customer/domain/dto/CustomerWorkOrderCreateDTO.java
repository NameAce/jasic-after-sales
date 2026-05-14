package com.jasic.aftersales.customer.domain.dto;

import com.jasic.aftersales.common.enums.BrandTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端工单创建参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "C端工单创建参数")
@Data
public class CustomerWorkOrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报修人姓名（兼容旧参数，正式口径由服务端按当前登录客户派生） */
    @ApiModelProperty(value = "报修人姓名（兼容旧参数，正式口径由服务端按当前登录客户派生）")
    private String customerName;

    /** 机器条码，佳士有码报修必填 */
    @ApiModelProperty(value = "机器条码，佳士有码报修必填")
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

    /** 品牌名称 */
    @ApiModelProperty(value = "品牌名称")
    private String brandName;

    /** 品牌类型 */
    @ApiModelProperty(value = "品牌类型", allowableValues = "JASIC,NON_JASIC", required = true)
    @NotNull(message = "品牌类型不能为空")
    private BrandTypeEnum brandType;

    /** 服务方式编码（MAIL/STORE） */
    @ApiModelProperty(value = "服务方式编码", allowableValues = "MAIL,STORE", required = true)
    @NotBlank(message = "服务方式不能为空")
    private String serviceMode;

    /** 质保状态 */
    @ApiModelProperty(value = "质保状态", allowableValues = "IN_WARRANTY,OUT_OF_WARRANTY")
    private String warrantyStatus;

    /** 故障描述选项 */
    @ApiModelProperty(value = "故障描述选项")
    private List<String> faultItems;

    /** 报修描述 */
    @ApiModelProperty(value = "报修描述")
    private String faultDesc;

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

    /** 目标服务网点ID */
    @ApiModelProperty(value = "目标服务网点ID", required = true)
    @NotNull(message = "服务网点不能为空")
    private Long serviceCompanyId;

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


