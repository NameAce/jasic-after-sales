package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Notify event type enum.
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyEventTypeEnum {

    WORK_ORDER_ASSIGNED("WORK_ORDER_ASSIGNED", "Work order assigned"),
    WORK_ORDER_EVALUATION_INVITE("WORK_ORDER_EVALUATION_INVITE", "Work order evaluation invite");

    /**
     * 通知事件类型编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知事件类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyEventTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知事件类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyEventTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
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

    /**
     * 根据编码解析通知事件类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyEventTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyEventTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify event type: " + code);
        }
        return value;
    }

    /**
     * 获取通知事件类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知事件类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




