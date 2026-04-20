package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单派单通知事件参数。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "工单派单通知事件参数")
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

    /** 操作人ID */
    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    /** 派单类型（ASSIGN/TRANSFER） */
    @ApiModelProperty(value = "派单类型（ASSIGN/TRANSFER）")
    private String assignType;

    /** 操作唯一标识 */
    @ApiModelProperty(value = "操作唯一标识")
    private String operationId;
}
