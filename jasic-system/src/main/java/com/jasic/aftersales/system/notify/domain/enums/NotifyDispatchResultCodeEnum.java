package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Notify dispatch result code.
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyDispatchResultCodeEnum {

    SKIPPED_CHANNEL_DISABLED("SKIPPED_CHANNEL_DISABLED", "Channel disabled"),
    SKIPPED_TEMPLATE_DISABLED("SKIPPED_TEMPLATE_DISABLED", "Template disabled"),
    SKIPPED_CHANNEL_CONFIG_MISSING("SKIPPED_CHANNEL_CONFIG_MISSING", "Channel config missing"),
    SKIPPED_RECEIVER_MISSING("SKIPPED_RECEIVER_MISSING", "Receiver missing"),
    SKIPPED_OPENID_MISSING("SKIPPED_OPENID_MISSING", "Openid missing"),
    SKIPPED_USER_NOT_SUBSCRIBED("SKIPPED_USER_NOT_SUBSCRIBED", "User not subscribed"),
    FAILED_CHANNEL_REQUEST("FAILED_CHANNEL_REQUEST", "Channel request failed"),
    FAILED_CHANNEL_RESPONSE("FAILED_CHANNEL_RESPONSE", "Channel response invalid"),
    FAILED_RENDER_ERROR("FAILED_RENDER_ERROR", "Render failed");

    private final String code;

    private final String desc;

    NotifyDispatchResultCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyDispatchResultCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyDispatchResultCodeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyDispatchResultCodeEnum fromCode(String code) {
        NotifyDispatchResultCodeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify dispatch result code: " + code);
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
