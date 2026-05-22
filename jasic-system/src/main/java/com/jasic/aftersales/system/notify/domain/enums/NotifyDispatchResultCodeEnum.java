package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知分发结果码枚举。
 *
 * @author Zoro
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
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private final String code;

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造通知分发结果编码实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    NotifyDispatchResultCodeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知分发结果编码。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    public static NotifyDispatchResultCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
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
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    @JsonCreator
    public static NotifyDispatchResultCodeEnum fromCode(String code) {
        NotifyDispatchResultCodeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify dispatch result code: " + code);
        }
        return value;
    }

    /**
     * 获取通知分发结果编码编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知分发结果编码描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}




