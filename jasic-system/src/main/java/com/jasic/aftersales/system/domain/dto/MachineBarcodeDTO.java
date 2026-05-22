package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 条码档案新增/修改参数
 *
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "条码档案新增/修改参数")
@Data
public class MachineBarcodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**主键ID，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "主键")
    private Long id;

    /**barcode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "机器条码", required = true)
    @NotBlank(message = "机器条码不能为空")
    private String barcode;

    /**deliverNumber 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    /**ownerHqId 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "归属总部ID")
    private Long ownerHqId;

    /**targetCompanyId 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "平台目标总部ID")
    private Long targetCompanyId;

    /**custId 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "CRM 公司ID")
    private String custId;

    /**salesOrg 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    /**productCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /**productName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /**productModel 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /**machineNo 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /**brandCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /**scanDate 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "条码扫码时间")
    private LocalDateTime scanDate;

    /**lastOutDate 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    /**crmAddTime 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "CRM 创建时间")
    private LocalDateTime crmAddTime;

    /**lastSyncTime 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

    /**状态，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "状态")
    private Integer status;

    /**备注，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "备注")
    private String remark;
}
