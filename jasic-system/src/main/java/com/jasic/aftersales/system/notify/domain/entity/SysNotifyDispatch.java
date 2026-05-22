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
 * 通知分发实体。
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_dispatch")
public class SysNotifyDispatch extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**主键ID，对应数据库中的同名或映射字段。*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**eventId 字段，对应数据库中的同名或映射字段。*/
    private Long eventId;

    /**sceneCode 字段，对应数据库中的同名或映射字段。*/
    private String sceneCode;

    /**targetType 字段，对应数据库中的同名或映射字段。*/
    private String targetType;

    /**templateCode 字段，对应数据库中的同名或映射字段。*/
    private String templateCode;

    /**channelType 字段，对应数据库中的同名或映射字段。*/
    private String channelType;

    /**receiverType 字段，对应数据库中的同名或映射字段。*/
    private String receiverType;

    /**receiverId 字段，对应数据库中的同名或映射字段。*/
    private Long receiverId;

    /**receiverAddress 字段，对应数据库中的同名或映射字段。*/
    private String receiverAddress;

    /**bizType 字段，对应数据库中的同名或映射字段。*/
    private String bizType;

    /**bizId 字段，对应数据库中的同名或映射字段。*/
    private Long bizId;

    /**bizNo 字段，对应数据库中的同名或映射字段。*/
    private String bizNo;

    /**dispatchStatus 字段，对应数据库中的同名或映射字段。*/
    private String dispatchStatus;

    /**processingTime 字段，对应数据库中的同名或映射字段。*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processingTime;

    /**resultCode 字段，对应数据库中的同名或映射字段。*/
    private String resultCode;

    /**resultMessage 字段，对应数据库中的同名或映射字段。*/
    private String resultMessage;

    /**retryCount 字段，对应数据库中的同名或映射字段。*/
    private Integer retryCount;

    /**nextRetryTime 字段，对应数据库中的同名或映射字段。*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextRetryTime;

    /**payloadJson 字段，对应数据库中的同名或映射字段。*/
    private String payloadJson;

    /**channelResponseJson 字段，对应数据库中的同名或映射字段。*/
    private String channelResponseJson;

    /**sentTime 字段，对应数据库中的同名或映射字段。*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;
}
