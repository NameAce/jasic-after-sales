package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板渠道配置实体。
 *
 * <p>该实体对应 `sys_notify_template_channel`，负责保存某个通知场景的外部渠道参数快照。
 * 当前阶段主要服务 `MP_SUBSCRIBE` 小程序订阅消息，不负责模板主数据、接收人规则和真实发送执行。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_template_channel")
public class SysNotifyTemplateChannel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知场景编码。
     */
    private String sceneCode;

    /**
     * 渠道类型编码。
     */
    private String channelType;

    /**
     * 渠道启停状态：1 启用，0 停用。
     */
    private Integer channelEnabled;

    /**
     * 渠道参数 JSON。
     */
    private String configJson;

    /**
     * 备注。
     */
    private String remark;
}
