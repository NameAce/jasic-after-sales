package com.jasic.aftersales.customer.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * C端客户地址修改参数
 *
 * @author Zoro
 * @date 2026/04/08
 */
@ApiModel(description = "C端客户地址修改参数")
@Data
public class CustomerAddressUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 地址ID */
    @ApiModelProperty(value = "地址ID", required = true)
    @NotNull(message = "地址ID不能为空")
    private Long id;

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
}
