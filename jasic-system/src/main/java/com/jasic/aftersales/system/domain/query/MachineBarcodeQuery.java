package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条码档案查询参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "条码档案查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineBarcodeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机器条码")
    private String barcode;

    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    @ApiModelProperty(value = "归属总部ID")
    private Long ownerHqId;

    @ApiModelProperty(value = "平台目标总部ID")
    private Long targetCompanyId;

    @ApiModelProperty(value = "CRM 公司ID")
    private String custId;

    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    @ApiModelProperty(value = "物料编码")
    private String productCode;

    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    @ApiModelProperty(value = "产品型号")
    private String productModel;

    @ApiModelProperty(value = "状态")
    private Integer status;
}
