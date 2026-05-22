package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 公司地址簿视图。
 *
 * @author Zoro
 * @date 2026/04/11
 */
@ApiModel(description = "公司地址簿视图")
@Data
public class CompanyAddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 地址ID */
    @ApiModelProperty(value = "地址ID")
    private Long id;

    /** 公司ID */
    @ApiModelProperty(value = "公司ID")
    private Long companyId;

    /** 联系人 */
    @ApiModelProperty(value = "联系人")
    private String contactName;

    /** 联系电话 */
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    /** 详细地址 */
    @ApiModelProperty(value = "详细地址")
    private String address;

    /** 是否默认地址（1=是，0=否） */
    @ApiModelProperty(value = "是否默认地址（1=是，0=否）")
    private Integer isDefault;
}
