package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * CRM 公司快照返回对象
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司快照返回对象")
@Data
public class CrmBizCompanySnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** CRM 客户ID */
    @ApiModelProperty(value = "CRM 客户ID")
    private Long custId;

    /** 公司编码 */
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /** 公司名称 */
    @ApiModelProperty(value = "公司名称")
    private String companyName;

    /** 联系人 */
    @ApiModelProperty(value = "联系人")
    private String contactName;

    /** 联系电话 */
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    /** 公司地址 */
    @ApiModelProperty(value = "公司地址")
    private String address;

    /** CRM 状态 */
    @ApiModelProperty(value = "CRM 状态")
    private Integer custState;

    /** CRM 状态文案 */
    @ApiModelProperty(value = "CRM 状态文案")
    private String custStateLabel;

    /** 新增时间 */
    @ApiModelProperty(value = "新增时间")
    private LocalDateTime addDate;

    /** 操作时间 */
    @ApiModelProperty(value = "操作时间")
    private LocalDateTime operTime;

    /** 最近同步时间 */
    @ApiModelProperty(value = "最近同步时间")
    private LocalDateTime lastSyncTime;

    /** 已存在的本地公司ID */
    @ApiModelProperty(value = "已存在的本地公司ID")
    private Long existingCompanyId;

    /** 已存在的本地公司名称 */
    @ApiModelProperty(value = "已存在的本地公司名称")
    private String existingCompanyName;
}
