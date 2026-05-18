package com.jasic.aftersales.system.notify.support;

import java.util.Collections;
import java.util.List;

/**
 * 通知场景元数据。
 *
 * <p>该对象由 `NotifySceneRegistry` 对外暴露，负责描述当前场景的业务语义、默认目标类型、
 * 可用变量以及全部目标元数据。运行时必须显式按目标类型读取目标级配置，不再透出旧模板接口兼容字段。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifySceneMeta {

    /** 通知场景编码。 */
    private final String sceneCode;

    /** 通知场景名称。 */
    private final String sceneName;

    /** 业务类型编码。 */
    private final String bizType;

    /** 事件编码。 */
    private final String eventCode;

    /** 场景默认目标类型。 */
    private final String defaultTargetType;

    /** 可用变量元数据。 */
    private final List<NotifyTemplateVariableMeta> variables;

    /** 当前场景支持的通知目标元数据。 */
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

    public String getEventCode() {
        return eventCode;
    }

    public String getDefaultTargetType() {
        return defaultTargetType;
    }

    public List<NotifyTemplateVariableMeta> getVariables() {
        return variables;
    }

    public List<NotifySceneTargetMeta> getTargetMetas() {
        return targetMetas;
    }
}
