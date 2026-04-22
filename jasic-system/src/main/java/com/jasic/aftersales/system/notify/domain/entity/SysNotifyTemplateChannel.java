package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Notify template channel config entity.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_template_channel")
public class SysNotifyTemplateChannel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String channelType;

    private Integer channelEnabled;

    private String channelScene;

    private String configJson;

    private String remark;
}
