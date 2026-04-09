package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 登录请求参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "登录请求参数")
@Data
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名或手机号 */
    @ApiModelProperty(value = "用户名或手机号", required = true)
    @NotBlank(message = "用户名或手机号不能为空")
    private String username;

    /** 密码 */
    @ApiModelProperty(value = "密码", required = true)
    @NotBlank(message = "密码不能为空")
    private String password;
}
