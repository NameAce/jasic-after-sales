package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 条码档案返回对象
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "条码档案返回对象")
@Data
public class MachineBarcodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "机器条码")
    private String barcode;

    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    @ApiModelProperty(value = "归属总部名称")
    private String hqCompanyName;

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

    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
