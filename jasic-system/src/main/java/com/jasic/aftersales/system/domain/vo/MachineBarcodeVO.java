package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 条码档案返回对象
 *
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "条码档案返回对象")
@Data
public class MachineBarcodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**主键ID，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "主键")
    private Long id;

    /**barcode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "机器条码")
    private String barcode;

    /**deliverNumber 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "发货单号")
    private String deliverNumber;

    /**hqCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "归属总部ID")
    private Long hqCompanyId;

    /**hqCompanyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "归属总部名称")
    private String hqCompanyName;

    /**custId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 公司ID")
    private String custId;

    /**salesOrg 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

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

    /**scanDate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "条码扫码时间")
    private LocalDateTime scanDate;

    /**lastOutDate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "最后出库日期")
    private LocalDateTime lastOutDate;

    /**crmAddTime 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 创建时间")
    private LocalDateTime crmAddTime;

    /**lastSyncTime 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

    /**warrantyStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "质保状态")
    private String warrantyStatus;

    /**状态，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "状态")
    private Integer status;

    /**备注，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "备注")
    private String remark;

    /**创建时间，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**更新时间，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
