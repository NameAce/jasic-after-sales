package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 条码档案批量导入单项参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "条码档案批量导入单项参数")
@Data
public class MachineBarcodeImportItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 机器条码 */
    @ApiModelProperty(value = "机器条码", required = true)
    @NotBlank(message = "机器条码不能为空")
    private String barcode;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID", required = true)
    @NotNull(message = "归属总部不能为空")
    private Long hqCompanyId;

    /** CRM 公司ID */
    @ApiModelProperty(value = "CRM 公司ID")
    private String custId;

    /** 销售组织 */
    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 商品名称 */
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /** 机器小号 */
    @ApiModelProperty(value = "机器小号")
    private String productTrumpet;

    /** 产品型号 */
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /** 机器小号 */
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /** 品牌编码 */
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /** 厂家最后出库日期 */
    @ApiModelProperty(value = "厂家最后出库日期")
    private LocalDateTime scanDate;

    /** 经销商最新出库日期 */
    @ApiModelProperty(value = "经销商最新出库日期")
    private LocalDateTime dealerOutDate;

    /** CRM 创建时间 */
    @ApiModelProperty(value = "CRM 创建时间")
    private LocalDateTime crmAddTime;

    /** 最近同步时间 */
    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

    /** 质保状态 */
    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
