package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * B端建维修订单条码查询结果。
 *
 * @author Codex
 * @date 2026/04/06
 */
@ApiModel(description = "B端建维修订单条码查询结果。")
@Data
public class WorkOrderCreateBarcodeInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 机器条码 */
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 商品名称 */
    @ApiModelProperty(value = "商品名称")
    private String productName;

    /** 产品型号 */
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /** 机器小号 */
    @ApiModelProperty(value = "机器小号")
    private String machineNo;

    /** 品牌编码 */
    @ApiModelProperty(value = "品牌编码")
    private String brandCode;

    /** 质保状态 */
    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /** 归属总部名称 */
    @ApiModelProperty(value = "归属总部名称")
    private String hqCompanyName;

    /** 故障描述选项 */
    @ApiModelProperty(value = "故障描述选项")
    private List<String> faultOptions;

    /** 其它故障文案 */
    @ApiModelProperty(value = "其它故障文案")
    private String otherFaultLabel;

    /** 目标受理公司选项 */
    @ApiModelProperty(value = "目标受理公司选项")
    private List<SysCompanySimpleVO> targetCompanyOptions;

    /** 默认目标受理公司ID */
    @ApiModelProperty(value = "默认目标受理公司ID")
    private Long defaultTargetCompanyId;
}
