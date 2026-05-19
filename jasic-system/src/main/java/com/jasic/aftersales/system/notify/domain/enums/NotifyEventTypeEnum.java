package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知事件类型枚举。
 *
 * <p>该枚举用于收口“工单通知场景”在事件主表中的业务事件编码。
 * 阶段一先把 6 个保留的小程序订阅通知场景全部登记到统一事件白名单中，
 * 便于后续阶段继续在同一套“通知场景 + 通知目标 + 统一分发”模型下补齐业务触发。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public enum NotifyEventTypeEnum {

    /** B 端待派单通知。 */
    WORK_ORDER_ACCEPT("WORK_ORDER_ACCEPT", "B端待派单通知"),

    /** B 端工单转入通知。 */
    WORK_ORDER_TRANSFER_IN("WORK_ORDER_TRANSFER_IN", "B端工单转入通知"),

    /** B 端维修员接单通知。 */
    WORK_ORDER_ASSIGNED("WORK_ORDER_ASSIGNED", "B端维修员接单通知"),

    /** C 端接单成功提醒。 */
    WORK_ORDER_ACCEPTED("WORK_ORDER_ACCEPTED", "C端接单成功提醒"),

    /** C 端网点转单通知。 */
    WORK_ORDER_TRANSFER_NOTICE("WORK_ORDER_TRANSFER_NOTICE", "C端网点转单通知"),

    /** C 端客户满意度评价通知。 */
    WORK_ORDER_EVALUATION_INVITE("WORK_ORDER_EVALUATION_INVITE", "C端客户满意度评价通知");

    /**
     * 事件类型编码。
     */
    private final String code;

    /**
     * 事件类型说明。
     */
    private final String desc;

    /**
     * 构造通知事件类型枚举。
     *
     * @param code 事件类型编码
     * @param desc 事件类型说明
     */
    NotifyEventTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知事件类型。
     *
     * @param code 事件类型编码
     * @return 命中的事件类型；未命中时返回 {@code null}
     */
    public static NotifyEventTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
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
     * 按编码解析通知事件类型。
     *
     * @param code 事件类型编码
     * @return 命中的事件类型；传入 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyEventTypeEnum fromCode(String code) {
        NotifyEventTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知事件类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取事件类型编码。
     *
     * @return 事件类型编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取事件类型说明。
     *
     * @return 事件类型说明
     */
    public String getDesc() {
        return desc;
    }
}
