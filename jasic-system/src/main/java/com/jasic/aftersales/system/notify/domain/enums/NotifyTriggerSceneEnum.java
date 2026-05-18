package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知触发场景枚举。
 *
 * <p>该枚举用于约束模板配置、场景配置和历史兼容接口可绑定的工单通知场景。
 * 本轮只保留 6 个已经确认的小程序通知场景，明确排除“B 端评价提醒”。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public enum NotifyTriggerSceneEnum {

    /** B 端接单通知。 */
    WORK_ORDER_ACCEPT("WORK_ORDER_ACCEPT", "B端接单通知"),

    /** B 端工单转入通知。 */
    WORK_ORDER_TRANSFER_IN("WORK_ORDER_TRANSFER_IN", "B端工单转入通知"),

    /** B 端工单派单通知。 */
    WORK_ORDER_ASSIGNED("WORK_ORDER_ASSIGNED", "B端工单派单通知"),

    /** C 端接单成功提醒。 */
    WORK_ORDER_ACCEPTED("WORK_ORDER_ACCEPTED", "C端接单成功提醒"),

    /** C 端网点转单通知。 */
    WORK_ORDER_TRANSFER_NOTICE("WORK_ORDER_TRANSFER_NOTICE", "C端网点转单通知"),

    /** C 端客户满意度评价通知。 */
    WORK_ORDER_EVALUATION_INVITE("WORK_ORDER_EVALUATION_INVITE", "C端客户满意度评价通知");

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
     * @return 命中的触发场景；传入 {@code null} 时返回 {@code null}
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
