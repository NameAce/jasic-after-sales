package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 角色名称（模糊） */
    private String roleName;

    /** 角色标识 */
    private String roleKey;

    /** 状态 */
    private Integer status;

    /** 公司ID */
    private Long companyId;
}
