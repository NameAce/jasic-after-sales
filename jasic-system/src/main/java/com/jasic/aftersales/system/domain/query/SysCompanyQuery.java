package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公司查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysCompanyQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 公司名称（模糊） */
    private String companyName;

    /** 公司类型编码 */
    private String typeCode;

    /** 状态（1=正常，0=停用） */
    private Integer status;
}
