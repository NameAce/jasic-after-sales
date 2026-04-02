package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 微信绑定确认参数
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class WechatBindConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 绑定码 */
    @NotBlank(message = "绑定码不能为空")
    private String bindCode;

    /** 微信登录 code */
    @NotBlank(message = "微信登录 code 不能为空")
    private String code;

    /** 微信手机号 code */
    private String phoneCode;
}
