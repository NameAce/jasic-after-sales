package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 用户信息VO
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "用户信息VO")
@Data
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @ApiModelProperty(value = "用户ID")
    private Long id;

    /** 用户名 */
    @ApiModelProperty(value = "用户名")
    private String username;

    /** 真实姓名 */
    @ApiModelProperty(value = "真实姓名")
    private String realName;

    /** 手机号 */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /** 邮箱 */
    @ApiModelProperty(value = "邮箱")
    private String email;

    /** 头像 */
    @ApiModelProperty(value = "头像")
    private String avatar;

    /** 性别 */
    @ApiModelProperty(value = "性别")
    private Integer sex;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 当前公司ID */
    @ApiModelProperty(value = "当前公司ID")
    private Long currentCompanyId;

    /** 当前公司名称 */
    @ApiModelProperty(value = "当前公司名称")
    private String currentCompanyName;

    /** 当前公司类型编码 */
    @ApiModelProperty(value = "当前公司类型编码")
    private String currentTypeCode;

    /** 当前主体类型 */
    @ApiModelProperty(value = "当前主体类型")
    private String currentSubjectType;

    /** 权限标识集合 */
    @ApiModelProperty(value = "权限标识集合")
    private Set<String> perms;

    /** 当前公司下的权限项集合 */
    @ApiModelProperty(value = "当前公司下的权限项集合")
    private List<SysPermissionVO> permissionVos;

    /** 角色列表 */
    @ApiModelProperty(value = "角色列表")
    private List<SysRoleVO> roles;

    /** 关联公司列表 */
    @ApiModelProperty(value = "关联公司列表")
    private List<SysCompanySimpleVO> companies;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
