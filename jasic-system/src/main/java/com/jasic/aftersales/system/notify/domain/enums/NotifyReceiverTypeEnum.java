package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知接收对象类型枚举。
 *
 * <p>该枚举只用于模板配置页展示和排障辅助，不直接决定实际接收人解析规则。
 * 实际接收对象仍由具体事件 handler 根据业务快照解析。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public enum NotifyReceiverTypeEnum {

    /** 维修员。 */
    REPAIRER("REPAIRER", "维修员"),

    /** C端客户。 */
    CUSTOMER("CUSTOMER", "C端客户");

    /**
     * 接收对象类型编码。
     */
    private final String code;

    /**
     * 接收对象类型说明。
     */
    private final String desc;

    /**
     * 构造通知接收对象类型枚举。
     *
     * @param code 接收对象类型编码
     * @param desc 接收对象类型说明
     */
    NotifyReceiverTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知接收对象类型。
     *
     * @param code 接收对象类型编码
     * @return 命中的接收对象类型；未命中时返回 {@code null}
     */
    public static NotifyReceiverTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
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
     * 按编码解析通知接收对象类型。
     *
     * @param code 接收对象类型编码
     * @return 命中的接收对象类型；入参为 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyReceiverTypeEnum fromCode(String code) {
        NotifyReceiverTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知接收对象类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取接收对象类型编码。
     *
     * @return 接收对象类型编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取接收对象类型说明。
     *
     * @return 接收对象类型说明
     */
    public String getDesc() {
        return desc;
    }
}
