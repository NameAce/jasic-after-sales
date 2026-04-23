package com.jasic.aftersales.customer.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * C 端条码信息
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "C 端条码信息")
@Data
public class CustomerBarcodeInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机器条码")
    private String barcode;

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

    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    @ApiModelProperty(value = "归属总部名称")
    private String hqCompanyName;

    @ApiModelProperty(value = "故障描述选项")
    private List<String> faultOptions;

    @ApiModelProperty(value = "其它故障文案")
    private String otherFaultLabel;
}
