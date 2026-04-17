package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 公司导入预览对象
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司导入预览对象")
@Data
public class CrmBizCompanyImportPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "CRM 客户ID")
    private Long custId;

    @ApiModelProperty(value = "公司名称")
    private String companyName;

    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    @ApiModelProperty(value = "管理员用户名")
    private String adminUsername;

    @ApiModelProperty(value = "公司类型编码")
    private String typeCode;

    @ApiModelProperty(value = "联系人")
    private String contactName;

    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    @ApiModelProperty(value = "详细地址")
    private String detailAddress;

    @ApiModelProperty(value = "省份编码")
    private String provinceCode;

    @ApiModelProperty(value = "省份名称")
    private String provinceName;

    @ApiModelProperty(value = "城市编码")
    private String cityCode;

    @ApiModelProperty(value = "城市名称")
    private String cityName;

    @ApiModelProperty(value = "区县编码")
    private String districtCode;

    @ApiModelProperty(value = "区县名称")
    private String districtName;

    @ApiModelProperty(value = "CRM 原始省份")
    private String crmProvinceName;

    @ApiModelProperty(value = "CRM 原始城市")
    private String crmCityName;

    @ApiModelProperty(value = "CRM 原始区县")
    private String crmDistrictName;

    @ApiModelProperty(value = "是否已匹配标准行政区")
    private Boolean areaMatched;

    @ApiModelProperty(value = "客服电话")
    private String servicePhone;

    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @ApiModelProperty(value = "建议状态")
    private Integer status;

    @ApiModelProperty(value = "CRM 状态")
    private Integer custState;

    @ApiModelProperty(value = "CRM 状态文案")
    private String custStateLabel;

    @ApiModelProperty(value = "已存在的本地公司ID")
    private Long existingCompanyId;

    @ApiModelProperty(value = "已存在的本地公司名称")
    private String existingCompanyName;

    @ApiModelProperty(value = "是否可导入")
    private Boolean canImport;

    @ApiModelProperty(value = "不可导入原因")
    private String importDisabledReason;
}
