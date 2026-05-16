package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知分发结果码枚举。
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyDispatchResultCodeEnum {

    SKIPPED_CHANNEL_DISABLED("SKIPPED_CHANNEL_DISABLED", "Channel disabled"),
    SKIPPED_TEMPLATE_DISABLED("SKIPPED_TEMPLATE_DISABLED", "Template disabled"),
    SKIPPED_CHANNEL_CONFIG_MISSING("SKIPPED_CHANNEL_CONFIG_MISSING", "Channel config missing"),
    SKIPPED_RECEIVER_MISSING("SKIPPED_RECEIVER_MISSING", "Receiver missing"),
    SKIPPED_OPENID_MISSING("SKIPPED_OPENID_MISSING", "Openid missing"),
    SKIPPED_USER_NOT_SUBSCRIBED("SKIPPED_USER_NOT_SUBSCRIBED", "User not subscribed"),
    FAILED_CHANNEL_REQUEST("FAILED_CHANNEL_REQUEST", "Channel request failed"),
    FAILED_CHANNEL_RESPONSE("FAILED_CHANNEL_RESPONSE", "Channel response invalid"),
    FAILED_RENDER_ERROR("FAILED_RENDER_ERROR", "Render failed"),

    /** 超过重试上限后进入死信。 */
    DEAD_RETRY_EXCEEDED("DEAD_RETRY_EXCEEDED", "Dead retry exceeded"),

    /** 人工关闭后进入死信。 */
    DEAD_MANUAL_CLOSED("DEAD_MANUAL_CLOSED", "Dead manual closed"),

    /** 未识别异常统一落到兜底失败码。 */
    FAILED_UNKNOWN("FAILED_UNKNOWN", "Unknown failure");

    /**
     * 通知分发结果编码编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知分发结果编码实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyDispatchResultCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知分发结果编码。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyDispatchResultCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyDispatchResultCodeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知分发结果编码。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyDispatchResultCodeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyDispatchResultCodeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify dispatch result code: " + code);
        }
        return value;
    }

    /**
     * 获取通知分发结果编码编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知分发结果编码描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




