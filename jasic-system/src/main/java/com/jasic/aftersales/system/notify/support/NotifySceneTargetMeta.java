package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;

/**
 * 通知场景下的目标元数据。
 *
 * <p>该对象描述“同一个通知场景下，某个目标应该如何默认配置”。
 * 它只负责系统注册态的只读元数据，不负责数据库持久化，也不负责运行时发送。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifySceneTargetMeta {

    /**
     * 通知目标类型。
     */
    private final String targetType;

    /**
     * 通知目标类型描述。
     */
    private final String targetTypeDesc;

    /**
     * 接收对象类型。
     */
    private final String receiverType;

    /**
     * 接收对象类型描述。
     */
    private final String receiverTypeDesc;

    /**
     * 接收对象说明。
     */
    private final String receiverDesc;

    /**
     * 默认是否启用。
     */
    private final Integer defaultEnabled;

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
     * 渠道类型。
     *
     * <p>只有外部通知目标才会配置该字段；站内目标保持为空。</p>
     */
    private final String channelType;

    /**
     * 渠道类型描述。
     */
    private final String channelTypeDesc;

    /**
     * 默认渠道配置。
     *
     * <p>当前仅 `MP_SUBSCRIBE` 使用该字段。</p>
     */
    private final NotifyTemplateChannelConfig defaultChannelConfig;

    /**
     * 构造通知目标元数据。
     *
     * @param targetType 通知目标类型
     * @param targetTypeDesc 通知目标类型描述
     * @param receiverType 接收对象类型
     * @param receiverTypeDesc 接收对象类型描述
     * @param receiverDesc 接收对象说明
     * @param defaultEnabled 默认是否启用
     * @param defaultTemplateName 默认模板名称
     * @param defaultTitleTemplate 默认标题模板
     * @param defaultContentTemplate 默认内容模板
     * @param defaultRouteType 默认跳转类型
     * @param defaultRouteValueTemplate 默认跳转值模板
     * @param channelType 渠道类型
     * @param channelTypeDesc 渠道类型描述
     * @param defaultChannelConfig 默认渠道配置
     */
    public NotifySceneTargetMeta(String targetType, String targetTypeDesc, String receiverType,
                                 String receiverTypeDesc, String receiverDesc, Integer defaultEnabled,
                                 String defaultTemplateName, String defaultTitleTemplate,
                                 String defaultContentTemplate, String defaultRouteType,
                                 String defaultRouteValueTemplate, String channelType,
                                 String channelTypeDesc, NotifyTemplateChannelConfig defaultChannelConfig) {
        this.targetType = targetType;
        this.targetTypeDesc = targetTypeDesc;
        this.receiverType = receiverType;
        this.receiverTypeDesc = receiverTypeDesc;
        this.receiverDesc = receiverDesc;
        this.defaultEnabled = defaultEnabled;
        this.defaultTemplateName = defaultTemplateName;
        this.defaultTitleTemplate = defaultTitleTemplate;
        this.defaultContentTemplate = defaultContentTemplate;
        this.defaultRouteType = defaultRouteType;
        this.defaultRouteValueTemplate = defaultRouteValueTemplate;
        this.channelType = channelType;
        this.channelTypeDesc = channelTypeDesc;
        this.defaultChannelConfig = defaultChannelConfig;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetTypeDesc() {
        return targetTypeDesc;
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

    public Integer getDefaultEnabled() {
        return defaultEnabled;
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

    public String getChannelType() {
        return channelType;
    }

    public String getChannelTypeDesc() {
        return channelTypeDesc;
    }

    public NotifyTemplateChannelConfig getDefaultChannelConfig() {
        return defaultChannelConfig;
    }

    /**
     * 判断当前目标是否为外部渠道目标。
     *
     * @return `true` 表示该目标需要依赖外部渠道配置
     */
    public boolean isExternalTarget() {
        return NotifyChannelTypeEnum.getByCode(channelType) != null;
    }
}
