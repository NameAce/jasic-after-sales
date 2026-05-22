package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 账号中心修改密码参数
 *
 * @author Zoro
 * @date 2026/04/02
 */
@ApiModel(description = "账号中心修改密码参数")
@Data
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前密码 */
    @ApiModelProperty(value = "当前密码", required = true)
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    /** 新密码 */
    @ApiModelProperty(value = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
