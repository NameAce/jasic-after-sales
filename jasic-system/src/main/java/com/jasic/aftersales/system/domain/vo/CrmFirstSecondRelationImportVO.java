package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CRM 一级二级关系导入列表 VO
 *
 * @author Codex
 * @date 2026/04/17
 */
@ApiModel(description = "CRM 一级二级关系导入列表 VO")
@Data
public class CrmFirstSecondRelationImportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 快照 ID */
    @ApiModelProperty(value = "快照ID")
    private Long id;

    /** 一级 CRM 企业 ID */
    @ApiModelProperty(value = "一级CRM企业ID")
    private Long firstCustId;

    /** 一级公司编码 */
    @ApiModelProperty(value = "一级公司编码")
    private String firstCompanyCode;

    /** 一级公司名称 */
    @ApiModelProperty(value = "一级公司名称")
    private String firstCompanyName;

    /** 二级 CRM 企业 ID */
    @ApiModelProperty(value = "二级CRM企业ID")
    private Long secondCustId;

    /** 二级公司编码 */
    @ApiModelProperty(value = "二级公司编码")
    private String secondCompanyCode;

    /** 二级公司名称 */
    @ApiModelProperty(value = "二级公司名称")
    private String secondCompanyName;

    /** 匹配到的一级本地公司 ID */
    @ApiModelProperty(value = "匹配到的一级本地公司ID")
    private Long firstCompanyId;

    /** 匹配到的一级本地公司名称 */
    @ApiModelProperty(value = "匹配到的一级本地公司名称")
    private String localFirstCompanyName;

    /** 匹配到的二级本地公司 ID */
    @ApiModelProperty(value = "匹配到的二级本地公司ID")
    private Long secondCompanyId;

    /** 匹配到的二级本地公司名称 */
    @ApiModelProperty(value = "匹配到的二级本地公司名称")
    private String localSecondCompanyName;

    /** CRM 操作时间 */
    @ApiModelProperty(value = "CRM操作时间")
    private LocalDateTime crmOperTime;

    /** 是否可导入 */
    @ApiModelProperty(value = "是否可导入")
    private Boolean canImport;

    /** 是否已存在正式关系 */
    @ApiModelProperty(value = "是否已存在正式关系")
    private Boolean existingRelation;

    /** 是否存在冲突 */
    @ApiModelProperty(value = "是否存在冲突")
    private Boolean conflictingRelation;

    /** 当前匹配说明 */
    @ApiModelProperty(value = "当前匹配说明")
    private String matchRemark;
}
