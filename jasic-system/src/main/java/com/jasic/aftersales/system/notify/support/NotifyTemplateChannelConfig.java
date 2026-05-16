package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知模板渠道配置快照。
 *
 * <p>该对象用于把 `sys_notify_template_channel.config_json` 反序列化成结构化对象，
 * 便于后台编辑和分发任务构建复用相同字段定义。当前阶段主要服务小程序订阅消息渠道。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@Data
public class NotifyTemplateChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 第三方模板 ID。
     */
    private String templateId;

    /**
     * 页面路径模板。
     */
    private String pagePathTemplate;

    /**
     * 字段映射列表。
     */
    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();
}
