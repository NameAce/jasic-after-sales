package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单通知事件视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单通知事件视图")
@Data
public class WorkOrderNotifyEventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 业务归属公司ID */
    @ApiModelProperty(value = "业务归属公司ID")
    private Long companyId;

    /** 业务归属公司名称 */
    @ApiModelProperty(value = "业务归属公司名称")
    private String companyName;

    /** 事件类型 */
    @ApiModelProperty(value = "事件类型")
    private String eventType;

    /** 触发节点 */
    @ApiModelProperty(value = "触发节点")
    private String triggerNode;

    /** 接收对象类型 */
    @ApiModelProperty(value = "接收对象类型")
    private String receiverType;

    /** 接收对象ID */
    @ApiModelProperty(value = "接收对象ID")
    private Long receiverId;

    /** 标题快照 */
    @ApiModelProperty(value = "标题快照")
    private String titleSnapshot;

    /** 内容快照 */
    @ApiModelProperty(value = "内容快照")
    private String contentSnapshot;

    /** 发送状态 */
    @ApiModelProperty(value = "发送状态")
    private String sendStatus;

    /** 发送时间 */
    @ApiModelProperty(value = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;

    /** 失败原因 */
    @ApiModelProperty(value = "失败原因")
    private String failReason;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
