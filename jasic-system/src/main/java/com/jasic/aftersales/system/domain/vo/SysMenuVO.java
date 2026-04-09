package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 菜单VO（含子菜单树结构）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "菜单VO（含子菜单树结构）")
@Data
public class SysMenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    @ApiModelProperty(value = "菜单ID")
    private Long id;

    /** 所属主体类型（PLATFORM/HQ/SERVICE） */
    @ApiModelProperty(value = "所属主体类型（PLATFORM/HQ/SERVICE）")
    private String subjectType;

    /** 菜单名称 */
    @ApiModelProperty(value = "菜单名称")
    private String menuName;

    /** 上级菜单ID */
    @ApiModelProperty(value = "上级菜单ID")
    private Long parentId;

    /** 类型（M/C/F） */
    @ApiModelProperty(value = "类型（M/C/F）")
    private String menuType;

    /** 路由地址 */
    @ApiModelProperty(value = "路由地址")
    private String path;

    /** 组件路径 */
    @ApiModelProperty(value = "组件路径")
    private String component;

    /** 权限标识 */
    @ApiModelProperty(value = "权限标识")
    private String perms;

    /** 图标 */
    @ApiModelProperty(value = "图标")
    private String icon;

    /** 排序 */
    @ApiModelProperty(value = "排序")
    private Integer orderNum;

    /** 是否可见 */
    @ApiModelProperty(value = "是否可见")
    private Integer isVisible;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /** 子菜单 */
    @ApiModelProperty(value = "子菜单")
    private List<SysMenuVO> children;
}
