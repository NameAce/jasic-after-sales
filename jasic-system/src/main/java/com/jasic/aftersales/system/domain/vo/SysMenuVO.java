package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单VO（含子菜单树结构）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysMenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    private Long id;

    /** 所属主体类型（PLATFORM/HQ/SERVICE） */
    private String subjectType;

    /** 菜单名称 */
    private String menuName;

    /** 上级菜单ID */
    private Long parentId;

    /** 类型（M/C/F） */
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

    /** 子菜单 */
    private List<SysMenuVO> children;
}
