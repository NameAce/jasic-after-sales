package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 维修员接单通知事件参数。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "维修员接单通知事件参数")
@Data
public class NotifyAssignedEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long workOrderId;

    /** 工单号 */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /** 旧接收人ID */
    @ApiModelProperty(value = "旧接收人ID")
    private Long oldAssignedUserId;

    /** 新接收人ID */
    @ApiModelProperty(value = "新接收人ID")
    private Long newAssignedUserId;

    /** 接收公司ID */
    @ApiModelProperty(value = "接收公司ID")
    private Long receiverCompanyId;

    /** 操作人ID */
    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    /** 派单类型（ASSIGN/TRANSFER） */
    @ApiModelProperty(value = "派单类型（ASSIGN/TRANSFER）")
    private String assignType;

    /** 客户名称快照 */
    @ApiModelProperty(value = "客户名称快照")
    private String customerName;

    /** 客户联系电话快照 */
    @ApiModelProperty(value = "客户联系电话快照")
    private String customerMobile;

    /** 操作唯一标识 */
    @ApiModelProperty(value = "操作唯一标识")
    private String operationId;
}
