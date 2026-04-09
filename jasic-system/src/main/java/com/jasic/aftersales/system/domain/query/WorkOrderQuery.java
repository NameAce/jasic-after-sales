package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单查询参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 查看范围（CURRENT/HISTORY/ALL） */
    @ApiModelProperty(value = "查看范围", allowableValues = "CURRENT,HISTORY,ALL")
    private String viewScope;

    /** 当前公司ID */
    @ApiModelProperty(value = "当前公司ID")
    private Long companyId;

    /** 当前用户ID（服务端注入） */
    @ApiModelProperty(value = "当前用户ID（服务端注入）")
    private Long currentUserId;

    /** 当前主体类型（服务端注入） */
    @ApiModelProperty(value = "当前主体类型（服务端注入）")
    private String subjectType;

    /** 当前有效数据范围（服务端注入） */
    @ApiModelProperty(value = "当前有效数据范围（服务端注入）")
    private String dataScope;

    /** 关联服务公司范围（服务端注入） */
    @ApiModelProperty(value = "关联服务公司范围（服务端注入）")
    private List<Long> relatedCompanyIds;

    /** 工单号（模糊） */
    @ApiModelProperty(value = "工单号（模糊）")
    private String orderNo;

    /** 客户姓名（模糊） */
    @ApiModelProperty(value = "客户姓名（模糊）")
    private String customerName;

    /** 客户手机号 */
    @ApiModelProperty(value = "客户手机号")
    private String customerMobile;

    /** 条码（模糊） */
    @ApiModelProperty(value = "条码（模糊）")
    private String barcode;

    /** 主状态 */
    @ApiModelProperty(value = "主状态", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    /** 是否转单 */
    @ApiModelProperty(value = "是否转单")
    private Integer hasTransfer;
}
