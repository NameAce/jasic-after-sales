package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

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

    @ApiModelProperty(value = "机器条码", required = true)
    @NotBlank(message = "机器条码不能为空")
    private String barcode;

    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    @ApiModelProperty(value = "归属总部ID", required = true)
    @NotNull(message = "归属总部不能为空")
    private Long hqCompanyId;

    @ApiModelProperty(value = "CRM 公司ID")
    private String custId;

    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    @ApiModelProperty(value = "物料编码")
    private String productCode;

    @ApiModelProperty(value = "商品名称")
    private String productName;

    @ApiModelProperty(value = "产品型号")
    private String productModel;

    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    @ApiModelProperty(value = "条码扫码时间")
    private LocalDateTime scanDate;

    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    @ApiModelProperty(value = "CRM 创建时间")
    private LocalDateTime crmAddTime;

    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
