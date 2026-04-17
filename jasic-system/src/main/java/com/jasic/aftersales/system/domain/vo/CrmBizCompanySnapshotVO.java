package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CRM 公司快照返回对象
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司快照返回对象")
@Data
public class CrmBizCompanySnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "CRM 客户ID")
    private Long custId;

    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    @ApiModelProperty(value = "公司名称")
    private String companyName;

    @ApiModelProperty(value = "联系人")
    private String contactName;

    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    @ApiModelProperty(value = "公司地址")
    private String address;

    @ApiModelProperty(value = "省份")
    private String provinceName;

    @ApiModelProperty(value = "城市")
    private String cityName;

    @ApiModelProperty(value = "区县")
    private String districtName;

    @ApiModelProperty(value = "客户范围")
    private Integer custRage;

    @ApiModelProperty(value = "建议导入的公司类型编码")
    private String typeCode;

    @ApiModelProperty(value = "CRM 状态")
    private Integer custState;

    @ApiModelProperty(value = "CRM 状态文案")
    private String custStateLabel;

    @ApiModelProperty(value = "新增时间")
    private LocalDateTime addDate;

    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operTime;

    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

    @ApiModelProperty(value = "已存在的本地公司ID")
    private Long existingCompanyId;

    @ApiModelProperty(value = "已存在的本地公司名称")
    private String existingCompanyName;

    @ApiModelProperty(value = "是否可导入")
    private Boolean canImport;

    @ApiModelProperty(value = "不可导入原因")
    private String importDisabledReason;
}
