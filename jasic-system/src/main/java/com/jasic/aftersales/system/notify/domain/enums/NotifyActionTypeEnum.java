package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知消息动作类型枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyActionTypeEnum {

    /** 创建消息。 */
    CREATE("CREATE", "创建"),

    /** 标记已读。 */
    READ("READ", "已读"),

    /** 标记已处理。 */
    DONE("DONE", "已处理"),

    /** 标记失效。 */
    INVALID("INVALID", "失效");

    private final String code;

    private final String desc;

    NotifyActionTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyActionTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyActionTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyActionTypeEnum fromCode(String code) {
        NotifyActionTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知动作类型编码：" + code);
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
