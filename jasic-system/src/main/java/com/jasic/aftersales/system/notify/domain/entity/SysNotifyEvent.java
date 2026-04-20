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
 * 通知事件实体。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_event")
public class SysNotifyEvent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等键 */
    private String eventKey;

    /** 事件类型 */
    private String eventType;

    /** 业务类型 */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 操作人ID */
    private Long operatorId;

    /** 接收人ID */
    private Long receiverId;

    /** 事件载荷 */
    private String payloadJson;

    /** 事件状态 */
    private String status;

    /** 重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRetryTime;

    /** 最近一次失败信息 */
    private String errorMessage;
}
