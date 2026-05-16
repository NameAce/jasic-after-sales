package com.jasic.aftersales.system.notify.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知事件详情。
 *
 * @author Codex
 * @date 2026/05/14
 */
@ApiModel(description = "通知事件详情")
@Data
public class NotifyTraceEventDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件ID */
    @ApiModelProperty(value = "事件ID")
    private Long id;

    /** 幂等键 */
    @ApiModelProperty(value = "幂等键")
    private String eventKey;

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

    /** 操作人ID */
    @ApiModelProperty(value = "操作人ID")
    private Long operatorId;

    /** 接收对象ID */
    @ApiModelProperty(value = "接收对象ID")
    private Long receiverId;

    /** 事件状态 */
    @ApiModelProperty(value = "事件状态")
    private String status;

    /** 开始处理时间 */
    @ApiModelProperty(value = "开始处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processingTime;

    /** 重试次数 */
    @ApiModelProperty(value = "重试次数")
    private Integer retryCount;

    /** 下次重试时间 */
    @ApiModelProperty(value = "下次重试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRetryTime;

    /** 最近失败原因 */
    @ApiModelProperty(value = "最近失败原因")
    private String errorMessage;

    /** 事件载荷快照 */
    @ApiModelProperty(value = "事件载荷快照")
    private String payloadJson;

    /** 站内目标聚合摘要 */
    @ApiModelProperty(value = "站内目标聚合摘要")
    private List<NotifyTraceTargetSummaryVO> messageTargetSummaries;

    /** 外部分发目标聚合摘要 */
    @ApiModelProperty(value = "外部分发目标聚合摘要")
    private List<NotifyTraceTargetSummaryVO> dispatchTargetSummaries;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 关联站内消息 */
    @ApiModelProperty(value = "关联站内消息")
    private List<NotifyTraceMessageDetailVO> messages;

    /** 关联分发任务 */
    @ApiModelProperty(value = "关联分发任务")
    private List<NotifyTraceDispatchDetailVO> dispatches;
}
