package com.jasic.aftersales.system.notify.support;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知分发载荷快照。
 *
 * <p>该对象在事件消费阶段写入 `sys_notify_dispatch.payload_json`，
 * 用于固化真实发送时依赖的场景、模板、渠道和变量快照。
 * 后续分发重试只读取这里的快照，不再回查当前模板或渠道配置，避免重试时被后台新配置影响。</p>
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Data
public class NotifyDispatchPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实际通知场景编码。
     */
    private String sceneCode;

    /**
     * 通知场景名称快照。
     */
    private String sceneName;

    /**
     * 通知目标类型快照。
     */
    private String targetType;

    /**
     * 兼容发送器读取的模板身份字段。
     *
     * <p>Phase 2 后该字段写入实际 sceneCode，避免外部分发继续依赖旧模板编码或旧组合字段。</p>
     */
    private String templateCode;

    /**
     * 模板名称快照。
     */
    private String templateName;

    /**
     * 渲染后的标题快照。
     */
    private String title;

    /**
     * 渲染后的内容快照。
     */
    private String content;

    /**
     * 渲染后的跳转类型。
     */
    private String routeType;

    /**
     * 渲染后的跳转值。
     */
    private String routeValue;

    /**
     * 实际外部分发渠道类型。
     */
    private String channelType;

    /**
     * 渠道启停状态快照：1 启用，0 停用，未命中具体渠道记录时为空。
     */
    private Integer channelEnabled;

    /**
     * 渠道配置快照。
     */
    private NotifyTemplateChannelConfig channelConfig;

    /**
     * 事件消费时用于渲染模板和渠道字段映射的变量快照。
     */
    private Map<String, Object> variables = new LinkedHashMap<>();
}
