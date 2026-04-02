package com.jasic.aftersales.customer.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * C端微信登录参数
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class CustomerWechatLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 微信登录 code */
    @NotBlank(message = "微信登录 code 不能为空")
    private String code;

    /** 微信手机号 code */
    private String phoneCode;
}
