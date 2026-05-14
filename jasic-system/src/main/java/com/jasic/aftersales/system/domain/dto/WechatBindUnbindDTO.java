package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 微信解绑参数
 *
 * @author Codex
 * @date 2026/04/10
 */
@ApiModel(description = "微信解绑参数")
@Data
public class WechatBindUnbindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前密码 */
    @ApiModelProperty(value = "当前密码", required = true)
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;
}


