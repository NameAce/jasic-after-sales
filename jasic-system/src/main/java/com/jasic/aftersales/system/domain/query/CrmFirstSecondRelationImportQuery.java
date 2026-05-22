package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CRM 一级二级关系导入查询参数
 *
 * @author Zoro
 * @date 2026/04/17
 */
@ApiModel(description = "CRM 一级二级关系导入查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmFirstSecondRelationImportQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 目标总部公司ID */
    @ApiModelProperty(value = "目标总部公司ID", required = true)
    private Long targetCompanyId;

    /** 一级公司 ID */
    @ApiModelProperty(value = "一级公司ID")
    private Long firstCompanyId;

    /** 二级公司 ID */
    @ApiModelProperty(value = "二级公司ID")
    private Long secondCompanyId;

    /** 一级公司编码 */
    @ApiModelProperty(value = "一级公司编码")
    private String firstCompanyCode;

    /** 二级公司编码 */
    @ApiModelProperty(value = "二级公司编码")
    private String secondCompanyCode;

    /** 是否展示异常数据 */
    @ApiModelProperty(value = "是否展示异常数据")
    private Boolean showAbnormal;
}
