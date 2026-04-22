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
 * Notify dispatch entity.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_dispatch")
public class SysNotifyDispatch extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private String templateCode;

    private String channelType;

    private String receiverType;

    private Long receiverId;

    private String receiverAddress;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String dispatchStatus;

    private String resultCode;

    private String resultMessage;

    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRetryTime;

    private String payloadJson;

    private String channelResponseJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;
}
