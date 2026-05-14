package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Notify dispatch status.
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyDispatchStatusEnum {

    PENDING("PENDING", "Pending"),
    PROCESSING("PROCESSING", "Processing"),
    SUCCESS("SUCCESS", "Success"),
    FAILED("FAILED", "Failed"),
    SKIPPED("SKIPPED", "Skipped");

    /**
     * ?? NotifyDispatchStatusEnum ?????
     *
     * @param code ??
     * @param desc ??
     * @return ????
     */
    private final String code;

    private final String desc;

    NotifyDispatchStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * ??By Code?
     *
     * @param code ??
     * @return ????
     */
    public static NotifyDispatchStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyDispatchStatusEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * ?? fromCode ?????
     *
     * @param code ??
     * @return ????
     */
    @JsonCreator
    public static NotifyDispatchStatusEnum fromCode(String code) {
        NotifyDispatchStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify dispatch status: " + code);
        }
        return value;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    public String getDesc() {
        return desc;
    }
}
