package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知待办状态枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyTodoStatusEnum {

    /** 待处理且未读。 */
    PENDING("PENDING", "待处理"),

    /** 已读但未处理。 */
    READ("READ", "已读"),

    /** 已通过真实业务动作处理。 */
    DONE("DONE", "已处理"),

    /** 已失效。 */
    INVALID("INVALID", "已失效");

    /**
     * ?? NotifyTodoStatusEnum ?????
     *
     * @param code ??
     * @param desc ??
     * @return ????
     */
    private final String code;

    private final String desc;

    NotifyTodoStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * ??By Code?
     *
     * @param code ??
     * @return ????
     */
    public static NotifyTodoStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTodoStatusEnum value : values()) {
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
    public static NotifyTodoStatusEnum fromCode(String code) {
        NotifyTodoStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知待办状态编码：" + code);
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
