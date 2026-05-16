package com.jasic.aftersales.system.notify.support;

import java.util.Collections;
import java.util.List;

/**
 * 通知场景元数据。
 *
 * <p>该对象是 `NotifySceneRegistry` 对外暴露的统一只读元数据载体。
 * 场景本身只描述业务事件语义，具体有哪些通知目标、每个目标的默认模板与渠道参数，
 * 统一由 `targetMetas` 维护。
 * 为兼容阶段一尚未完全改造完的旧运行时代码，本对象保留了一组“主目标”快捷字段，
 * 它们始终映射到 `defaultTargetType` 对应的目标元数据。</p>
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
     * 事件编码。
     */
    private final String eventCode;

    /**
     * 兼容旧运行时的默认通知目标类型。
     */
    private final String defaultTargetType;

    /**
     * 可用变量元数据。
     */
    private final List<NotifyTemplateVariableMeta> variables;

    /**
     * 当前场景下支持的通知目标元数据。
     */
    private final List<NotifySceneTargetMeta> targetMetas;

    /**
     * 构造通知场景元数据。
     *
     * @param sceneCode 场景编码
     * @param sceneName 场景名称
     * @param bizType 业务类型编码
     * @param eventCode 事件编码
     * @param defaultTargetType 默认通知目标类型
     * @param variables 可用变量元数据
     * @param targetMetas 场景支持的通知目标元数据
     */
    public NotifySceneMeta(String sceneCode, String sceneName, String bizType, String eventCode,
                           String defaultTargetType, List<NotifyTemplateVariableMeta> variables,
                           List<NotifySceneTargetMeta> targetMetas) {
        this.sceneCode = sceneCode;
        this.sceneName = sceneName;
        this.bizType = bizType;
        this.eventCode = eventCode;
        this.defaultTargetType = defaultTargetType;
        this.variables = variables == null ? Collections.emptyList() : Collections.unmodifiableList(variables);
        this.targetMetas = targetMetas == null ? Collections.emptyList() : Collections.unmodifiableList(targetMetas);
    }

    /**
     * 按通知目标类型查询元数据。
     *
     * @param targetType 通知目标类型
     * @return 命中的目标元数据；未命中时返回 {@code null}
     */
    public NotifySceneTargetMeta getTargetMeta(String targetType) {
        if (targetType == null) {
            return null;
        }
        String normalizedTargetType = targetType.trim();
        for (NotifySceneTargetMeta targetMeta : targetMetas) {
            if (targetMeta.getTargetType().equals(normalizedTargetType)) {
                return targetMeta;
            }
        }
        return null;
    }

    /**
     * 获取默认通知目标元数据。
     *
     * @return 默认通知目标元数据；不存在时返回 {@code null}
     */
    public NotifySceneTargetMeta getDefaultTargetMeta() {
        return getTargetMeta(defaultTargetType);
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

    /**
     * 获取事件编码。
     *
     * @return 事件编码
     */
    public String getEventCode() {
        return eventCode;
    }

    /**
     * 兼容旧运行时的事件类型读取。
     *
     * @return 事件编码
     */
    public String getEventType() {
        return eventCode;
    }

    /**
     * 获取默认通知目标类型。
     *
     * @return 默认通知目标类型
     */
    public String getDefaultTargetType() {
        return defaultTargetType;
    }

    /**
     * 兼容旧模板配置接口的通知类型读取。
     *
     * @return 默认通知目标类型
     */
    public String getNotifyType() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getTargetType();
    }

    /**
     * 兼容旧模板配置接口的通知类型描述读取。
     *
     * @return 默认通知目标类型描述
     */
    public String getNotifyTypeDesc() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getTargetTypeDesc();
    }

    /**
     * 兼容旧模板配置接口的接收对象类型读取。
     *
     * @return 默认通知目标接收对象类型
     */
    public String getReceiverType() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getReceiverType();
    }

    /**
     * 兼容旧模板配置接口的接收对象类型描述读取。
     *
     * @return 默认通知目标接收对象类型描述
     */
    public String getReceiverTypeDesc() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getReceiverTypeDesc();
    }

    /**
     * 兼容旧模板配置接口的接收对象说明读取。
     *
     * @return 默认通知目标接收对象说明
     */
    public String getReceiverDesc() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getReceiverDesc();
    }

    /**
     * 兼容旧渠道配置接口的渠道类型读取。
     *
     * @return 默认通知目标渠道类型
     */
    public String getChannelType() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getChannelType();
    }

    /**
     * 兼容旧渠道配置接口的渠道类型描述读取。
     *
     * @return 默认通知目标渠道类型描述
     */
    public String getChannelTypeDesc() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getChannelTypeDesc();
    }

    /**
     * 兼容旧模板配置接口的默认模板名称读取。
     *
     * @return 默认模板名称
     */
    public String getDefaultTemplateName() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getDefaultTemplateName();
    }

    /**
     * 兼容旧模板配置接口的默认标题模板读取。
     *
     * @return 默认标题模板
     */
    public String getDefaultTitleTemplate() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getDefaultTitleTemplate();
    }

    /**
     * 兼容旧模板配置接口的默认内容模板读取。
     *
     * @return 默认内容模板
     */
    public String getDefaultContentTemplate() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getDefaultContentTemplate();
    }

    /**
     * 兼容旧模板配置接口的默认跳转类型读取。
     *
     * @return 默认跳转类型
     */
    public String getDefaultRouteType() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getDefaultRouteType();
    }

    /**
     * 兼容旧模板配置接口的默认跳转值模板读取。
     *
     * @return 默认跳转值模板
     */
    public String getDefaultRouteValueTemplate() {
        NotifySceneTargetMeta targetMeta = getDefaultTargetMeta();
        return targetMeta == null ? null : targetMeta.getDefaultRouteValueTemplate();
    }

    public List<NotifyTemplateVariableMeta> getVariables() {
        return variables;
    }

    public List<NotifySceneTargetMeta> getTargetMetas() {
        return targetMetas;
    }
}
