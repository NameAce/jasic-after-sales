package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 当前用户权限项VO
 *
 * @author Zoro
 * @date 2026/04/08
 */
@ApiModel(description = "当前用户权限项VO")
@Data
public class SysPermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    @ApiModelProperty(value = "菜单ID")
    private Long id;

    /** 菜单名称 */
    @ApiModelProperty(value = "菜单名称")
    private String menuName;

    /** 上级菜单ID */
    @ApiModelProperty(value = "上级菜单ID")
    private Long parentId;

    /** 菜单类型 */
    @ApiModelProperty(value = "菜单类型")
    private String menuType;

    /** 权限标识 */
    @ApiModelProperty(value = "权限标识")
    private String perms;
}
