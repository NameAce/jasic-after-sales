package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知事件类型枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyEventTypeEnum {

    /** 工单派单事件。 */
    WORK_ORDER_ASSIGNED("WORK_ORDER_ASSIGNED", "工单已派单");

    private final String code;

    private final String desc;

    NotifyEventTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyEventTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyEventTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyEventTypeEnum fromCode(String code) {
        NotifyEventTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知事件类型编码：" + code);
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
