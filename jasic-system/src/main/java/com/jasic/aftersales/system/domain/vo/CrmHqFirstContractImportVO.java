package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CRM 签约导入列表 VO。
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 签约导入列表 VO")
@Data
public class CrmHqFirstContractImportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 快照ID */
    @ApiModelProperty(value = "快照ID")
    private Long id;

    /** CRM 客户编码 */
    @ApiModelProperty(value = "CRM客户编码")
    private String kunnr;

    /** CRM 企业标识 */
    @ApiModelProperty(value = "CRM企业标识")
    private Long custId;

    /** CRM 企业名称 */
    @ApiModelProperty(value = "CRM企业名称")
    private String crmCompanyName;

    /** 销售组织 */
    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    /** CRM 大区编码 */
    @ApiModelProperty(value = "CRM大区编码")
    private String regionCode;

    /** CRM 大区名称 */
    @ApiModelProperty(value = "CRM大区名称")
    private String regionName;

    /** 有效标识 */
    @ApiModelProperty(value = "有效标识")
    private Integer aliveFlag;

    /** CRM 新增时间 */
    @ApiModelProperty(value = "CRM新增时间")
    private LocalDateTime crmAddTime;

    /** CRM 修改时间 */
    @ApiModelProperty(value = "CRM修改时间")
    private LocalDateTime crmOperTime;

    /** 匹配到的一级公司ID */
    @ApiModelProperty(value = "匹配到的一级公司ID")
    private Long firstCompanyId;

    /** 匹配到的一级公司名称 */
    @ApiModelProperty(value = "匹配到的一级公司名称")
    private String firstCompanyName;

    /** 匹配到的本地大区ID */
    @ApiModelProperty(value = "匹配到的本地大区ID")
    private Long regionId;

    /** 匹配到的本地大区名称 */
    @ApiModelProperty(value = "匹配到的本地大区名称")
    private String localRegionName;

    /** 是否可导入 */
    @ApiModelProperty(value = "是否可导入")
    private Boolean canImport;

    /** 正式签约是否已存在 */
    @ApiModelProperty(value = "正式签约是否已存在")
    private Boolean existingContract;

    /** 当前匹配说明 */
    @ApiModelProperty(value = "当前匹配说明")
    private String matchRemark;
}
