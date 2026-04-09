package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一级-二级从属关系查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "一级-二级从属关系查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class FirstSecondRelationQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 一级网点公司ID */
    @ApiModelProperty(value = "一级网点公司ID")
    private Long firstCompanyId;

    /** 二级网点公司ID */
    @ApiModelProperty(value = "二级网点公司ID")
    private Long secondCompanyId;
}
