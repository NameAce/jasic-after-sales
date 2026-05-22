package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 公司导入预览对象
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司导入预览对象")
@Data
public class CrmBizCompanyImportPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**custId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 客户ID")
    private Long custId;

    /**companyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司名称")
    private String companyName;

    /**companyShortName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    /**companyCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /**adminUsername 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "管理员用户名")
    private String adminUsername;

    /**typeCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司类型编码")
    private String typeCode;

    /**contactName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "联系人")
    private String contactName;

    /**contactPhone 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    /**detailAddress 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "详细地址")
    private String detailAddress;

    /**provinceCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "省份编码")
    private String provinceCode;

    /**provinceName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "省份名称")
    private String provinceName;

    /**cityCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "城市编码")
    private String cityCode;

    /**cityName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "城市名称")
    private String cityName;

    /**districtCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "区县编码")
    private String districtCode;

    /**districtName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "区县名称")
    private String districtName;

    /**crmProvinceName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 原始省份")
    private String crmProvinceName;

    /**crmCityName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 原始城市")
    private String crmCityName;

    /**crmDistrictName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 原始区县")
    private String crmDistrictName;

    /**areaMatched 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否已匹配标准行政区")
    private Boolean areaMatched;

    /**servicePhone 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客服电话")
    private String servicePhone;

    /**sourceType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    /**状态，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "建议状态")
    private Integer status;

    /**custState 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 状态")
    private Integer custState;

    /**custStateLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 状态文案")
    private String custStateLabel;

    /**existingCompanyId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "已存在的本地公司ID")
    private Long existingCompanyId;

    /**existingCompanyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "已存在的本地公司名称")
    private String existingCompanyName;

    /**canImport 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否可导入")
    private Boolean canImport;

    /**importDisabledReason 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "不可导入原因")
    private String importDisabledReason;
}


