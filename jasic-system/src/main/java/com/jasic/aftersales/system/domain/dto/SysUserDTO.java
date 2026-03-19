package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 用户新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID（修改时必传） */
    private Long id;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码（新增时必传） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别 */
    private Integer sex;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 关联公司ID列表 */
    private List<Long> companyIds;

    /** 角色ID列表（当前公司下） */
    private List<Long> roleIds;
}
