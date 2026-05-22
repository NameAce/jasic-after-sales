package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 微信绑定确认参数
 *
 * @author Zoro
 * @date 2026/04/02
 */
@ApiModel(description = "微信绑定确认参数")
@Data
public class WechatBindConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 绑定票据 */
    @ApiModelProperty(value = "绑定票据", required = true)
    @NotBlank(message = "绑定票据不能为空")
    private String bindTicket;

    /** 微信登录凭证 */
    @ApiModelProperty(value = "微信登录凭证", required = true)
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;

    /** 微信手机号凭证 */
    @ApiModelProperty(value = "微信手机号凭证")
    private String phoneCode;
}


