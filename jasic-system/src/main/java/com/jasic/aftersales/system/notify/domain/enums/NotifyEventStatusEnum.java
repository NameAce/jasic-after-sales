package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知事件状态枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyEventStatusEnum {

    /** 新建待消费。 */
    NEW("NEW", "新建"),

    /** 消费处理中。 */
    PROCESSING("PROCESSING", "处理中"),

    /** 消费成功。 */
    SUCCESS("SUCCESS", "成功"),

    /** 消费失败。 */
    FAILED("FAILED", "失败");

    private final String code;

    private final String desc;

    NotifyEventStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyEventStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyEventStatusEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyEventStatusEnum fromCode(String code) {
        NotifyEventStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知事件状态编码：" + code);
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
