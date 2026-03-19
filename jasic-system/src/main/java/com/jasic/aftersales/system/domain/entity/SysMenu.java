package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属主体类型（PLATFORM/HQ/SERVICE） */
    private String subjectType;

    /** 菜单名称 */
    private String menuName;

    /** 上级菜单ID（0为顶级） */
    private Long parentId;

    /** 类型（M=目录，C=菜单，F=按钮） */
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识（如 system:user:list） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer orderNum;

    /** 是否可见（1=是，0=否） */
    private Integer isVisible;

    /** 状态（1=正常，0=停用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
