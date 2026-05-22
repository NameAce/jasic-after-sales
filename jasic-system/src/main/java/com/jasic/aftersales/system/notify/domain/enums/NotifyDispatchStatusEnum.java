package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知分发状态枚举。
 *
 * @author Zoro
 * @date 2026/04/21
 */
public enum NotifyDispatchStatusEnum {

    PENDING("PENDING", "Pending"),
    PROCESSING("PROCESSING", "Processing"),
    SUCCESS("SUCCESS", "Success"),
    FAILED("FAILED", "Failed"),
    SKIPPED("SKIPPED", "Skipped"),

    /** 进入死信。 */
    DEAD("DEAD", "Dead");

    /**
     * 通知分发状态编码。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private final String code;

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造通知分发状态实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    NotifyDispatchStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知分发状态。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
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
     * 根据编码解析通知分发状态。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
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
     * 获取通知分发状态编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知分发状态描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}




