package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictTypeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 字典名称 */
    private String dictName;

    /** 字典类型 */
    private String dictType;

    /** 状态 */
    private Integer status;
}
