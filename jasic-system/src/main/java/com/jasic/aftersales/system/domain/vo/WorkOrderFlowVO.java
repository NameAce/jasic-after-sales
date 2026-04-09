package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单流转历史视图
 *
 * @author Codex
 * @date 2026/03/27
 */
@ApiModel(description = "工单流转历史视图")
@Data
public class WorkOrderFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流转记录ID */
    @ApiModelProperty(value = "流转记录ID")
    private Long id;

    /** 动作类型 */
    @ApiModelProperty(value = "动作类型")
    private String actionType;

    /** 动作名称 */
    @ApiModelProperty(value = "动作名称")
    private String actionName;

    /** 动作前主状态 */
    @ApiModelProperty(value = "动作前主状态")
    private String beforeStatus;

    /** 动作前主状态名称 */
    @ApiModelProperty(value = "动作前主状态名称")
    private String beforeStatusName;

    /** 动作后主状态 */
    @ApiModelProperty(value = "动作后主状态")
    private String afterStatus;

    /** 动作后主状态名称 */
    @ApiModelProperty(value = "动作后主状态名称")
    private String afterStatusName;

    /** 来源公司ID */
    @ApiModelProperty(value = "来源公司ID")
    private Long fromCompanyId;

    /** 来源公司名称 */
    @ApiModelProperty(value = "来源公司名称")
    private String fromCompanyName;

    /** 目标公司ID */
    @ApiModelProperty(value = "目标公司ID")
    private Long toCompanyId;

    /** 目标公司名称 */
    @ApiModelProperty(value = "目标公司名称")
    private String toCompanyName;

    /** 操作公司ID */
    @ApiModelProperty(value = "操作公司ID")
    private Long operatorCompanyId;

    /** 操作公司名称 */
    @ApiModelProperty(value = "操作公司名称")
    private String operatorCompanyName;

    /** 操作人ID */
    @ApiModelProperty(value = "操作人ID")
    private Long operatorUserId;

    /** 操作人名称 */
    @ApiModelProperty(value = "操作人名称")
    private String operatorUserName;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
