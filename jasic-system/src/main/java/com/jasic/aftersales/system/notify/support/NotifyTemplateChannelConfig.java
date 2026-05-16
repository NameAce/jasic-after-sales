package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知模板渠道配置快照。
 *
 * <p>该对象用于把 `notify_scene_target.config_json` 反序列化成结构化对象，
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
     * 小程序渠道场景。
     *
     * <p>取值固定为 `B/C`，用于显式声明当前订阅消息应该走哪个小程序实例发送，
     * 避免运行时继续依赖接收对象类型或场景编码做隐式推断。</p>
     */
    private String channelScene;

    /**
     * 页面路径模板。
     */
    private String pagePathTemplate;

    /**
     * 字段映射列表。
     */
    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();
}
