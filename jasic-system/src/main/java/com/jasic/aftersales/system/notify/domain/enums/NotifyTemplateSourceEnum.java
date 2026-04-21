package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jasic.aftersales.system.notify.support.NotifyConstants;

/**
 * Notification template source.
 *
 * @author Codex
 * @date 2026/04/20
 */
public enum NotifyTemplateSourceEnum {

    BUILT_IN(NotifyConstants.TEMPLATE_SOURCE_BUILT_IN, "内置模板"),
    CUSTOM(NotifyConstants.TEMPLATE_SOURCE_CUSTOM, "自定义模板");

    private final String code;

    private final String desc;

    NotifyTemplateSourceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyTemplateSourceEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTemplateSourceEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyTemplateSourceEnum fromCode(String code) {
        NotifyTemplateSourceEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify template source: " + code);
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
