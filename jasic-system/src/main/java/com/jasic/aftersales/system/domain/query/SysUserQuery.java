package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 用户名（模糊） */
    private String username;

    /** 真实姓名（模糊） */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 状态 */
    private Integer status;

    /** 公司ID（查询某公司下的用户） */
    private Long companyId;
}
