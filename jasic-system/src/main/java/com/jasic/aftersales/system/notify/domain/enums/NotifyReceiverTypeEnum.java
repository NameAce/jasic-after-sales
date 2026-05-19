package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知接收对象类型枚举。
 *
 * <p>该枚举用于描述“通知目标默认发给哪一类对象”，方便后台配置页展示和排障追踪。
 * 实际接收人解析仍由具体事件处理器结合业务快照计算。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public enum NotifyReceiverTypeEnum {

    /** 当前目标网点下可派单用户。*/
    ASSIGN_USER("ASSIGN_USER", "B端派单用户"),

    /** 当前目标网点下可接单用户。 */
    ACCEPT_USER("ACCEPT_USER", "B端接单用户"),

    /** 被派单工程师本人。 */
    REPAIRER("REPAIRER", "维修工程师"),

    /** C 端客户本人。 */
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
     * @return 命中的接收对象类型；传入 {@code null} 时返回 {@code null}
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
