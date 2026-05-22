package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 重置密码参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "重置密码参数")
@Data
public class ResetPwdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @ApiModelProperty(value = "用户ID", required = true)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**targetCompanyId 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "目标公司ID")
    private Long targetCompanyId;

    /** 新密码 */
    @ApiModelProperty(value = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
