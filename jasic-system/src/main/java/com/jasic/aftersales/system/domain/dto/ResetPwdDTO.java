package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 重置密码参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class ResetPwdDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
