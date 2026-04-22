package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Notify receiver type.
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyReceiverTypeEnum {

    CUSTOMER("CUSTOMER", "瀹㈡埛"),
    SYS_USER("SYS_USER", "绯荤粺鐢ㄦ埛");

    private final String code;

    private final String desc;

    NotifyReceiverTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyReceiverTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyReceiverTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyReceiverTypeEnum fromCode(String code) {
        NotifyReceiverTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify receiver type: " + code);
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
