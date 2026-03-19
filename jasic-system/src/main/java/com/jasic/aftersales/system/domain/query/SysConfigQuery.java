package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数设置查询参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 参数名称 */
    private String configName;

    /** 参数键名 */
    private String configKey;

    /** 是否内置 */
    private Integer configType;
}
