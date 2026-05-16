package com.jasic.aftersales.system.notify.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知事件关联站内消息详情。
 *
 * @author Codex
 * @date 2026/05/14
 */
@ApiModel(description = "通知事件关联站内消息详情")
@Data
public class NotifyTraceMessageDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @ApiModelProperty(value = "消息ID")
    private Long id;

    /** 来源事件ID */
    @ApiModelProperty(value = "来源事件ID")
    private Long eventId;

    /** 消息类型 */
    @ApiModelProperty(value = "消息类型")
    private String messageType;

    /** 事件类型 */
    @ApiModelProperty(value = "事件类型")
    private String eventType;

    /** 通知场景编码 */
    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    /** 通知场景名称 */
    @ApiModelProperty(value = "通知场景名称")
    private String sceneName;

    /** 通知目标类型 */
    @ApiModelProperty(value = "通知目标类型")
    private String targetType;

    /** 通知目标类型说明 */
    @ApiModelProperty(value = "通知目标类型说明")
    private String targetTypeDesc;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 业务编号 */
    @ApiModelProperty(value = "业务编号")
    private String bizNo;

    /** 接收人ID */
    @ApiModelProperty(value = "接收人ID")
    private Long receiverId;

    /** 接收公司ID */
    @ApiModelProperty(value = "接收公司ID")
    private Long receiverCompanyId;

    /** 接收人名称 */
    @ApiModelProperty(value = "接收人名称")
    private String receiverName;

    /** 标题 */
    @ApiModelProperty(value = "标题")
    private String title;

    /** 摘要 */
    @ApiModelProperty(value = "摘要")
    private String summary;

    /** 跳转类型 */
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /** 跳转值 */
    @ApiModelProperty(value = "跳转值")
    private String routeValue;

    /** 待办状态 */
    @ApiModelProperty(value = "待办状态")
    private String todoStatus;

    /** 失效原因 */
    @ApiModelProperty(value = "失效原因")
    private String invalidReason;

    /** 已读时间 */
    @ApiModelProperty(value = "已读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    /** 已处理时间 */
    @ApiModelProperty(value = "已处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime doneTime;

    /** 失效时间 */
    @ApiModelProperty(value = "失效时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime invalidTime;

    /** 扩展快照 */
    @ApiModelProperty(value = "扩展快照")
    private String extJson;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
