package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * CRM 公司导入预览对象
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司导入预览对象")
@Data
public class CrmBizCompanyImportPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** CRM 客户ID */
    @ApiModelProperty(value = "CRM 客户ID")
    private Long custId;

    /** 公司名称 */
    @ApiModelProperty(value = "公司名称")
    private String companyName;

    /** 公司编码 */
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /** 联系人 */
    @ApiModelProperty(value = "联系人")
    private String contactName;

    /** 联系电话 */
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    /** 公司地址 */
    @ApiModelProperty(value = "公司地址")
    private String address;

    /** 建议状态 */
    @ApiModelProperty(value = "建议状态")
    private Integer status;

    /** CRM 状态 */
    @ApiModelProperty(value = "CRM 状态")
    private Integer custState;

    /** CRM 状态文案 */
    @ApiModelProperty(value = "CRM 状态文案")
    private String custStateLabel;

    /** 已存在的本地公司ID */
    @ApiModelProperty(value = "已存在的本地公司ID")
    private Long existingCompanyId;

    /** 已存在的本地公司名称 */
    @ApiModelProperty(value = "已存在的本地公司名称")
    private String existingCompanyName;
}
