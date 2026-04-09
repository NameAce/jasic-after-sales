package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 公司查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "公司查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysCompanyQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 公司名称（模糊） */
    @ApiModelProperty(value = "公司名称（模糊）")
    private String companyName;

    /** 公司类型编码 */
    @ApiModelProperty(value = "公司类型编码")
    private String typeCode;

    /**
     * 业务分类（HQ=总部, FIRST_LEVEL=一级网点, SECOND_LEVEL=二级网点）
     * 与 typeCode 互斥，优先使用 category
     */
    @ApiModelProperty(value = "业务分类（HQ=总部, FIRST_LEVEL=一级网点, SECOND_LEVEL=二级网点） 与 typeCode 互斥，优先使用 category")
    private String category;

    /** 状态（1=正常，0=停用） */
    @ApiModelProperty(value = "状态（1=正常，0=停用）")
    private Integer status;
}
