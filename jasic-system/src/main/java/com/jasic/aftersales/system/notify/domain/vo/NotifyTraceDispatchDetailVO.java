package com.jasic.aftersales.system.notify.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知分发详情。
 *
 * @author Codex
 * @date 2026/05/14
 */
@ApiModel(description = "通知分发详情")
@Data
public class NotifyTraceDispatchDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分发任务ID */
    @ApiModelProperty(value = "分发任务ID")
    private Long id;

    /** 来源事件ID */
    @ApiModelProperty(value = "来源事件ID")
    private Long eventId;

    /** 通知场景编码 */
    @ApiModelProperty(value = "通知场景编码")
    private String templateCode;

    /** 通知场景名称 */
    @ApiModelProperty(value = "通知场景名称")
    private String sceneName;

    /** 模板名称快照 */
    @ApiModelProperty(value = "模板名称快照")
    private String templateName;

    /** 渠道类型 */
    @ApiModelProperty(value = "渠道类型")
    private String channelType;

    /** 渠道状态：1启用，0停用 */
    @ApiModelProperty(value = "渠道状态：1启用，0停用")
    private Integer channelEnabled;

    /** 接收对象类型 */
    @ApiModelProperty(value = "接收对象类型")
    private String receiverType;

    /** 接收对象ID */
    @ApiModelProperty(value = "接收对象ID")
    private Long receiverId;

    /** 接收地址 */
    @ApiModelProperty(value = "接收地址")
    private String receiverAddress;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 业务编号 */
    @ApiModelProperty(value = "业务编号")
    private String bizNo;

    /** 分发状态 */
    @ApiModelProperty(value = "分发状态")
    private String dispatchStatus;

    /** 开始处理时间 */
    @ApiModelProperty(value = "开始处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processingTime;

    /** 结果编码 */
    @ApiModelProperty(value = "结果编码")
    private String resultCode;

    /** 结果说明 */
    @ApiModelProperty(value = "结果说明")
    private String resultMessage;

    /** 重试次数 */
    @ApiModelProperty(value = "重试次数")
    private Integer retryCount;

    /** 下次重试时间 */
    @ApiModelProperty(value = "下次重试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRetryTime;

    /** 分发载荷快照 */
    @ApiModelProperty(value = "分发载荷快照")
    private String payloadJson;

    /** 渠道响应快照 */
    @ApiModelProperty(value = "渠道响应快照")
    private String channelResponseJson;

    /** 发送成功时间 */
    @ApiModelProperty(value = "发送成功时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
