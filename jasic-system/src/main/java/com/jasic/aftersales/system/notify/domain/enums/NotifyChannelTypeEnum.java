package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported notify channel types.
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyChannelTypeEnum {

    MP_SUBSCRIBE("MP_SUBSCRIBE", "Mini program subscribe"),
    SMS("SMS", "SMS"),
    EMAIL("EMAIL", "Email");

    /**
     * 通知渠道类型编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知渠道类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyChannelTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知渠道类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyChannelTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyChannelTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知渠道类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyChannelTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyChannelTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify channel type: " + code);
        }
        return value;
    }

    /**
     * 获取通知渠道类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知渠道类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




