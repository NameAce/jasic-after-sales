package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知触发场景枚举。
 *
 * <p>该枚举用于模板配置重构后的 `trigger_scene` 白名单，负责约束模板配置允许绑定的业务触发节点。
 * 它只表达“在哪个业务场景触发通知”，不负责事件消费实现细节。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public enum NotifyTriggerSceneEnum {

    /** 工单派单。 */
    WORK_ORDER_ASSIGNED("WORK_ORDER_ASSIGNED", "工单派单"),

    /** 工单评价邀请。 */
    WORK_ORDER_EVALUATION_INVITE("WORK_ORDER_EVALUATION_INVITE", "工单评价邀请");

    /**
     * 触发场景编码。
     */
    private final String code;

    /**
     * 触发场景说明。
     */
    private final String desc;

    /**
     * 构造通知触发场景枚举。
     *
     * @param code 触发场景编码
     * @param desc 触发场景说明
     */
    NotifyTriggerSceneEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知触发场景。
     *
     * @param code 触发场景编码
     * @return 命中的触发场景；未命中时返回 {@code null}
     */
    public static NotifyTriggerSceneEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTriggerSceneEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按编码解析通知触发场景。
     *
     * @param code 触发场景编码
     * @return 命中的触发场景；入参为 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyTriggerSceneEnum fromCode(String code) {
        NotifyTriggerSceneEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知触发场景编码：" + code);
        }
        return value;
    }

    /**
     * 获取触发场景编码。
     *
     * @return 触发场景编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取触发场景说明。
     *
     * @return 触发场景说明
     */
    public String getDesc() {
        return desc;
    }
}
