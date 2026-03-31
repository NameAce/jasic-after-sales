package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单发布角色模板选项
 *
 * @author Zoro
 * @date 2026/03/31
 */
@Data
public class SysMenuPublishTemplateOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long id;

    /** 公司类型编码 */
    private String typeCode;

    /** 角色名称 */
    private String roleName;

    /** 角色标识 */
    private String roleKey;

    /** 是否管理员模板 */
    private Integer isAdmin;
}
