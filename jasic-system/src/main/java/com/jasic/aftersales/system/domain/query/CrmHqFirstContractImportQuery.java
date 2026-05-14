package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CRM 签约导入查询参数。
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 签约导入查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmHqFirstContractImportQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 当前选择的总部公司ID */
    @ApiModelProperty(value = "当前选择的总部公司ID", required = true)
    private Long hqCompanyId;

    /** 目标总部公司ID */
    @ApiModelProperty(value = "目标总部公司ID", required = true)
    private Long targetCompanyId;

    /** 一级公司ID */
    @ApiModelProperty(value = "一级公司ID")
    private Long firstCompanyId;

    /** 大区ID */
    @ApiModelProperty(value = "大区ID")
    private Long regionId;

    /** CRM customer code */
    @ApiModelProperty(value = "CRM customer code")
    private String kunnr;

    /** 是否展示异常数据 */
    @ApiModelProperty(value = "是否展示异常数据")
    private Boolean showAbnormal;
}
