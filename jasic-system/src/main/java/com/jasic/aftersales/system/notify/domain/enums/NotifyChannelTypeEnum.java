package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported notify channel types.
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyChannelTypeEnum {

    MP_SUBSCRIBE("MP_SUBSCRIBE", "Mini program subscribe"),
    SMS("SMS", "SMS"),
    EMAIL("EMAIL", "Email");

    private final String code;

    private final String desc;

    NotifyChannelTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyChannelTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyChannelTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyChannelTypeEnum fromCode(String code) {
        NotifyChannelTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify channel type: " + code);
        }
        return value;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
