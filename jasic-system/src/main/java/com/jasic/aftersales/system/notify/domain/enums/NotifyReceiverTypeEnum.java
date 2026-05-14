package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Notify receiver type.
 *
 * @author Codex
 * @date 2026/04/21
 */
public enum NotifyReceiverTypeEnum {

    CUSTOMER("CUSTOMER", "瀹㈡埛"),
    SYS_USER("SYS_USER", "绯荤粺鐢ㄦ埛");

    /**
     * 通知接收人类型编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知接收人类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyReceiverTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知接收人类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyReceiverTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyReceiverTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知接收人类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyReceiverTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyReceiverTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify receiver type: " + code);
        }
        return value;
    }

    /**
     * 获取通知接收人类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知接收人类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




