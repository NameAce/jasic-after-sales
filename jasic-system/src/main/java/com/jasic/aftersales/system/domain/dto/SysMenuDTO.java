package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 菜单新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysMenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID（修改时必传） */
    private Long id;

    /** 所属主体类型（PLATFORM/HQ/SERVICE） */
    @NotBlank(message = "主体类型不能为空")
    private String subjectType;

    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    /** 上级菜单ID */
    private Long parentId;

    /** 类型（M=目录，C=菜单，F=按钮） */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识 */
    private String perms;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer orderNum;

    /** 是否可见 */
    private Integer isVisible;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
