package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知类型枚举。
 *
 * <p>该枚举用于表达模板最终要生成的通知形态。
 * 它明确区分站内消息、站内待办和小程序订阅消息，避免继续沿用旧的消息类型混用语义。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public enum NotifyTypeEnum {

    /** 站内消息。 */
    IN_APP_MESSAGE("IN_APP_MESSAGE", "站内消息"),

    /** 站内待办。 */
    IN_APP_TODO("IN_APP_TODO", "站内待办"),

    /** 小程序订阅消息。 */
    MP_SUBSCRIBE("MP_SUBSCRIBE", "小程序订阅消息");

    /**
     * 通知类型编码。
     */
    private final String code;

    /**
     * 通知类型说明。
     */
    private final String desc;

    /**
     * 构造通知类型枚举。
     *
     * @param code 通知类型编码
     * @param desc 通知类型说明
     */
    NotifyTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知类型。
     *
     * @param code 通知类型编码
     * @return 命中的通知类型；未命中时返回 {@code null}
     */
    public static NotifyTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按编码解析通知类型。
     *
     * @param code 通知类型编码
     * @return 命中的通知类型；入参为 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyTypeEnum fromCode(String code) {
        NotifyTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知类型编码。
     *
     * @return 通知类型编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知类型说明。
     *
     * @return 通知类型说明
     */
    public String getDesc() {
        return desc;
    }
}
