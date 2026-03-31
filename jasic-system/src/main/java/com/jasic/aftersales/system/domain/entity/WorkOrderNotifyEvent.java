package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单通知事件实体
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_notify_event")
public class WorkOrderNotifyEvent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 业务归属公司ID */
    private Long companyId;

    /** 事件类型 */
    private String eventType;

    /** 触发节点 */
    private String triggerNode;

    /** 接收对象类型 */
    private String receiverType;

    /** 接收对象ID */
    private Long receiverId;

    /** 标题快照 */
    private String titleSnapshot;

    /** 内容快照 */
    private String contentSnapshot;

    /** 发送状态 */
    private String sendStatus;

    /** 发送时间 */
    private LocalDateTime sendTime;

    /** 失败原因 */
    private String failReason;
}
