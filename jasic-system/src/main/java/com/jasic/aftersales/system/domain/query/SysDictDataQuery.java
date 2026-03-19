package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据查询参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictDataQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 字典类型 */
    private String dictType;

    /** 字典标签 */
    private String dictLabel;

    /** 状态 */
    private Integer status;
}
