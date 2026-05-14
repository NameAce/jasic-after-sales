package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * B端小程序账号认领绑定参数
 *
 * @author Codex
 * @date 2026/04/10
 */
@ApiModel(description = "B端小程序账号认领绑定参数")
@Data
public class MpBindLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 微信登录凭证 */
    @ApiModelProperty(value = "微信登录凭证", required = true)
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    /** 用户名或手机号 */
    @ApiModelProperty(value = "用户名或手机号", required = true)
    @NotBlank(message = "用户名或手机号不能为空")
    private String usernameOrPhone;

    /** 登录密码 */
    @ApiModelProperty(value = "登录密码", required = true)
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /** 微信手机号凭证 */
    @ApiModelProperty(value = "微信手机号凭证")
    private String phoneCode;
}


