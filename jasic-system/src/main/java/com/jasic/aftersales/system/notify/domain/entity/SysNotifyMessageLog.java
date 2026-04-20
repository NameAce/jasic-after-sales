package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息日志实体。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Data
@TableName("sys_notify_message_log")
public class SysNotifyMessageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息ID */
    private Long messageId;

    /** 动作类型 */
    private String actionType;

    /** 动作执行人 */
    private Long actionUserId;

    /** 备注 */
    private String remark;

    /** 快照数据 */
    private String snapshotJson;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
