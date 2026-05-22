package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CRM 公司快照返回对象
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司快照返回对象")
@Data
public class CrmBizCompanySnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**custId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 客户ID")
    private Long custId;

    /**companyCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /**companyShortName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    /**companyName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司名称")
    private String companyName;

    /**contactName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "联系人")
    private String contactName;

    /**contactPhone 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    /**address 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司地址")
    private String address;

    /**provinceName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "省份")
    private String provinceName;

    /**cityName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "城市")
    private String cityName;

    /**districtName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "区县")
    private String districtName;

    /**custRage 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "客户范围")
    private Integer custRage;

    /**typeCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "建议导入的公司类型编码")
    private String typeCode;

    /**custState 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 状态")
    private Integer custState;

    /**custStateLabel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "CRM 状态文案")
    private String custStateLabel;

    /**addDate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "新增时间")
    private LocalDateTime addDate;

    /**operTime 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operTime;

    /**lastSyncTime 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

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


