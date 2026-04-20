package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知业务类型枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyBizTypeEnum {

    /** 工单业务。 */
    WORK_ORDER("WORK_ORDER", "工单");

    private final String code;

    private final String desc;

    NotifyBizTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyBizTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyBizTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyBizTypeEnum fromCode(String code) {
        NotifyBizTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知业务类型编码：" + code);
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
