package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单发布结果
 *
 * @author Zoro
 * @date 2026/03/31
 */
@Data
public class SysMenuPublishResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    private Long menuId;

    /** 新增公司类型菜单上限数量 */
    private Integer addedTypeCodeCount;

    /** 新增角色模板菜单数量 */
    private Integer addedTemplateCount;

    /** 新增角色菜单数量 */
    private Integer updatedRoleCount;

    /** 踢下线用户数量 */
    private Integer kickedUserCount;

    /** 跳过公司数量 */
    private Integer skippedCompanyCount;
}
