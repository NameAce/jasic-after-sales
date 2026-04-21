package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Notification template entity.
 *
 * @author Codex
 * @date 2026/04/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_template")
public class SysNotifyTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String templateName;

    private String templateSource;

    private String bizType;

    private String eventType;

    private String messageType;

    private Integer notifyEnabled;

    private Integer overrideEnabled;

    private String routeType;

    private String titleTemplate;

    private String summaryTemplate;

    private String routeValueTemplate;

    private String variablesJson;

    private String remark;
}
