package com.jasic.aftersales.system.notify.support;

import java.util.Collections;
import java.util.List;

/**
 * 通知场景元数据。
 *
 * <p>该对象是 `NotifySceneRegistry` 对外暴露的唯一元数据载体，
 * 用于集中描述一个 `sceneCode` 对应的业务类型、事件类型、通知类型、接收对象、
 * 默认模板、默认路由和变量元数据。
 * 它只承载静态规则，不负责模板落库、启停控制和运行时渲染。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifySceneMeta {

    /**
     * 通知场景编码。
     */
    private final String sceneCode;

    /**
     * 通知场景名称。
     */
    private final String sceneName;

    /**
     * 业务类型编码。
     */
    private final String bizType;

    /**
     * 事件类型编码。
     */
    private final String eventType;

    /**
     * 通知类型编码。
     */
    private final String notifyType;

    /**
     * 通知类型说明。
     */
    private final String notifyTypeDesc;

    /**
     * 接收对象类型编码。
     */
    private final String receiverType;

    /**
     * 接收对象类型说明。
     */
    private final String receiverTypeDesc;

    /**
     * 接收对象说明。
     */
    private final String receiverDesc;

    /**
     * 渠道类型编码。
     */
    private final String channelType;

    /**
     * 渠道类型说明。
     */
    private final String channelTypeDesc;

    /**
     * 默认模板名称。
     */
    private final String defaultTemplateName;

    /**
     * 默认标题模板。
     */
    private final String defaultTitleTemplate;

    /**
     * 默认内容模板。
     */
    private final String defaultContentTemplate;

    /**
     * 默认跳转类型。
     */
    private final String defaultRouteType;

    /**
     * 默认跳转值模板。
     */
    private final String defaultRouteValueTemplate;

    /**
     * 可用变量元数据。
     */
    private final List<NotifyTemplateVariableMeta> variables;

    /**
     * 构造通知场景元数据。
     *
     * @param sceneCode 通知场景编码
     * @param sceneName 通知场景名称
     * @param bizType 业务类型编码
     * @param eventType 事件类型编码
     * @param notifyType 通知类型编码
     * @param notifyTypeDesc 通知类型说明
     * @param receiverType 接收对象类型编码
     * @param receiverTypeDesc 接收对象类型说明
     * @param receiverDesc 接收对象说明
     * @param channelType 渠道类型编码
     * @param channelTypeDesc 渠道类型说明
     * @param defaultTemplateName 默认模板名称
     * @param defaultTitleTemplate 默认标题模板
     * @param defaultContentTemplate 默认内容模板
     * @param defaultRouteType 默认跳转类型
     * @param defaultRouteValueTemplate 默认跳转值模板
     * @param variables 可用变量元数据
     */
    public NotifySceneMeta(String sceneCode, String sceneName, String bizType, String eventType,
                           String notifyType, String notifyTypeDesc, String receiverType,
                           String receiverTypeDesc, String receiverDesc, String channelType,
                           String channelTypeDesc, String defaultTemplateName, String defaultTitleTemplate,
                           String defaultContentTemplate, String defaultRouteType,
                           String defaultRouteValueTemplate, List<NotifyTemplateVariableMeta> variables) {
        this.sceneCode = sceneCode;
        this.sceneName = sceneName;
        this.bizType = bizType;
        this.eventType = eventType;
        this.notifyType = notifyType;
        this.notifyTypeDesc = notifyTypeDesc;
        this.receiverType = receiverType;
        this.receiverTypeDesc = receiverTypeDesc;
        this.receiverDesc = receiverDesc;
        this.channelType = channelType;
        this.channelTypeDesc = channelTypeDesc;
        this.defaultTemplateName = defaultTemplateName;
        this.defaultTitleTemplate = defaultTitleTemplate;
        this.defaultContentTemplate = defaultContentTemplate;
        this.defaultRouteType = defaultRouteType;
        this.defaultRouteValueTemplate = defaultRouteValueTemplate;
        this.variables = variables == null ? Collections.emptyList() : Collections.unmodifiableList(variables);
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public String getSceneName() {
        return sceneName;
    }

    public String getBizType() {
        return bizType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public String getNotifyTypeDesc() {
        return notifyTypeDesc;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public String getReceiverTypeDesc() {
        return receiverTypeDesc;
    }

    public String getReceiverDesc() {
        return receiverDesc;
    }

    public String getChannelType() {
        return channelType;
    }

    public String getChannelTypeDesc() {
        return channelTypeDesc;
    }

    public String getDefaultTemplateName() {
        return defaultTemplateName;
    }

    public String getDefaultTitleTemplate() {
        return defaultTitleTemplate;
    }

    public String getDefaultContentTemplate() {
        return defaultContentTemplate;
    }

    public String getDefaultRouteType() {
        return defaultRouteType;
    }

    public String getDefaultRouteValueTemplate() {
        return defaultRouteValueTemplate;
    }

    public List<NotifyTemplateVariableMeta> getVariables() {
        return variables;
    }
}
