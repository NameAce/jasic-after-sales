package com.jasic.aftersales.system.notify.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知记录排障分页项。
 *
 * @author Zoro
 * @date 2026/05/14
 */
@ApiModel(description = "通知记录排障分页项")
@Data
public class NotifyTracePageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件ID */
    @ApiModelProperty(value = "事件ID")
    private Long eventId;

    /** 事件类型 */
    @ApiModelProperty(value = "事件类型")
    private String eventType;

    /** 通知场景编码 */
    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    /** 通知场景名称 */
    @ApiModelProperty(value = "通知场景名称")
    private String sceneName;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 业务编号 */
    @ApiModelProperty(value = "业务编号")
    private String bizNo;

    /** 事件状态 */
    @ApiModelProperty(value = "事件状态")
    private String eventStatus;

    /** 事件重试次数 */
    @ApiModelProperty(value = "事件重试次数")
    private Integer eventRetryCount;

    /** 事件最近错误 */
    @ApiModelProperty(value = "事件最近错误")
    private String eventErrorMessage;

    /** 站内产物数量 */
    @ApiModelProperty(value = "站内产物数量")
    private Integer messageCount;

    /** 外部分发任务数量 */
    @ApiModelProperty(value = "外部分发任务数量")
    private Integer dispatchCount;

    /** 站内目标聚合摘要 */
    @ApiModelProperty(value = "站内目标聚合摘要")
    private java.util.List<NotifyTraceTargetSummaryVO> messageTargetSummaries;

    /** 外部分发目标聚合摘要 */
    @ApiModelProperty(value = "外部分发目标聚合摘要")
    private java.util.List<NotifyTraceTargetSummaryVO> dispatchTargetSummaries;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
