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
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "C 端条码信息")
@Data
public class CustomerBarcodeInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**barcode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /**productCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /**productName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /**productModel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /**machineNo 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /**brandCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /**lastOutDate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    /**warrantyStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    /**hqCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /**hqCompanyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "归属总部名称")
    private String hqCompanyName;

    /**faultOptions 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "故障描述选项")
    private List<String> faultOptions;

    /**otherFaultLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "其它故障文案")
    private String otherFaultLabel;
}
