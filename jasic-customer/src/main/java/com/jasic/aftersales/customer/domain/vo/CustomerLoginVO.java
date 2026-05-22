package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端登录结果
 *
 * @author Zoro
 * @date 2026/04/06
 */
@ApiModel(description = "C端登录结果")
@Data
public class CustomerLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** token */
    @ApiModelProperty(value = "token")
    private String token;

    /** 用户信息 */
    @ApiModelProperty(value = "用户信息")
    private CustomerUserInfoVO userInfo;
}
