package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * B端小程序登录参数
 *
 * @author Zoro
 * @date 2026/04/02
 */
@ApiModel(description = "B端小程序登录参数")
@Data
public class MpLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 微信登录凭证 */
    @ApiModelProperty(value = "微信登录凭证", required = true)
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;
}
