package com.jasic.aftersales.customer.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * C端客户地址新增参数
 *
 * @author Codex
 * @date 2026/04/08
 */
@ApiModel(description = "C端客户地址新增参数")
@Data
public class CustomerAddressCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 联系人 */
    @ApiModelProperty(value = "联系人", required = true)
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /** 联系手机号 */
    @ApiModelProperty(value = "联系手机号", required = true)
    @NotBlank(message = "联系手机号不能为空")
    private String contactMobile;

    /** 省 */
    @ApiModelProperty(value = "省", required = true)
    @NotBlank(message = "省不能为空")
    private String province;

    /** 市 */
    @ApiModelProperty(value = "市", required = true)
    @NotBlank(message = "市不能为空")
    private String city;

    /** 区县 */
    @ApiModelProperty(value = "区县")
    private String county;

    /** 详细地址 */
    @ApiModelProperty(value = "详细地址", required = true)
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    /** 是否默认地址（1=是，0=否） */
    @ApiModelProperty(value = "是否默认地址（1=是，0=否）")
    private Integer isDefault;
}
