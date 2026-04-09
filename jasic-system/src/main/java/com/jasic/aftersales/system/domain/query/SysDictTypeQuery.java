package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 字典类型查询参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@ApiModel(description = "字典类型查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictTypeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 字典名称 */
    @ApiModelProperty(value = "字典名称")
    private String dictName;

    /** 字典类型 */
    @ApiModelProperty(value = "字典类型")
    private String dictType;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;
}
