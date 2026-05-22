package com.jasic.aftersales.customer.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * C端客户地址视图
 *
 * @author Zoro
 * @date 2026/04/08
 */
@ApiModel(description = "C端客户地址视图")
@Data
public class CustomerAddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 地址ID */
    @ApiModelProperty(value = "地址ID")
    private Long id;

    /** 联系人 */
    @ApiModelProperty(value = "联系人")
    private String contactName;

    /** 联系手机号 */
    @ApiModelProperty(value = "联系手机号")
    private String contactMobile;

    /** 省 */
    @ApiModelProperty(value = "省")
    private String province;

    /** 市 */
    @ApiModelProperty(value = "市")
    private String city;

    /** 区县 */
    @ApiModelProperty(value = "区县")
    private String county;

    /** 详细地址 */
    @ApiModelProperty(value = "详细地址")
    private String detailAddress;

    /** 完整地址 */
    @ApiModelProperty(value = "完整地址")
    private String fullAddress;

    /** 是否默认地址（1=是，0=否） */
    @ApiModelProperty(value = "是否默认地址（1=是，0=否）")
    private Integer isDefault;
}
