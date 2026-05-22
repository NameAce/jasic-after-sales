package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条码档案查询参数
 *
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "条码档案查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineBarcodeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /**barcode 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /**deliverNumber 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    /**ownerHqId 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "归属总部ID")
    private Long ownerHqId;

    /**targetCompanyId 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "平台目标总部ID")
    private Long targetCompanyId;

    /**custId 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "CRM 公司ID")
    private String custId;

    /**salesOrg 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    /**productCode 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /**machineNo 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /**productModel 字段，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /**状态，用于参与列表查询、筛选或数据权限收口。*/
    @ApiModelProperty(value = "状态")
    private Integer status;
}
