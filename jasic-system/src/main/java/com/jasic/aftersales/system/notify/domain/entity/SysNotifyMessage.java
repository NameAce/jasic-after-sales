package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知消息实体。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_message")
public class SysNotifyMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源事件ID */
    private Long eventId;

    /** 消息类型 */
    private String messageType;

    /** 事件类型 */
    private String eventType;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 接收人ID */
    private Long receiverId;

    /** 接收人名称快照 */
    private String receiverName;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 跳转类型 */
    private String routeType;

    /** 跳转值 */
    private String routeValue;

    /** 待办状态 */
    private String todoStatus;

    /** 失效原因 */
    private String invalidReason;

    /** 已读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    /** 已处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime doneTime;

    /** 失效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime invalidTime;

    /** 扩展信息 */
    private String extJson;
}
