package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 账号中心资料修改参数
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class UpdateProfileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 邮箱 */
    private String email;

    /** 当前密码 */
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;
}
