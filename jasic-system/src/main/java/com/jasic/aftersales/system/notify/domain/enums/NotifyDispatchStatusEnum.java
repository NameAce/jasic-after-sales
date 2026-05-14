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
     * 通知分发状态编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知分发状态实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyDispatchStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知分发状态。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyDispatchStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
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
     * 根据编码解析通知分发状态。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyDispatchStatusEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyDispatchStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify dispatch status: " + code);
        }
        return value;
    }

    /**
     * 获取通知分发状态编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知分发状态描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




