package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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

    /** 机器条码 */
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /** 发货单号 */
    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID")
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

    /** 机器小号 */
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /** 产品型号 */
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /** 质保状态 */
    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;
}
