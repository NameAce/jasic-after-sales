package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知业务类型枚举。
 *
 * <p>用于约束通知事件、消息和分发排障里允许出现的业务边界。
 * 当前阶段只收口售后工单通知，不负责扩展未知业务类型。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public enum NotifyBizTypeEnum {

    /** 工单业务。 */
    WORK_ORDER("WORK_ORDER", "工单");

    /**
     * 业务类型编码。
     */
    private final String code;

    /**
     * 业务类型说明。
     */
    private final String desc;

    /**
     * 构造通知业务类型枚举。
     *
     * @param code 业务类型编码
     * @param desc 业务类型说明
     */
    NotifyBizTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知业务类型。
     *
     * <p>该方法只做轻量解析，不对未知编码抛异常，便于查询和校验场景按需判断。</p>
     *
     * @param code 业务类型编码
     * @return 命中的业务类型；未命中时返回 {@code null}
     */
    public static NotifyBizTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyBizTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按编码解析通知业务类型。
     *
     * <p>该方法用于 JSON 反序列化和强校验场景，遇到未知编码时直接抛错，
     * 避免非法业务类型进入通知事件链路。</p>
     *
     * @param code 业务类型编码
     * @return 命中的业务类型；入参为 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyBizTypeEnum fromCode(String code) {
        NotifyBizTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知业务类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取业务类型编码。
     *
     * @return 业务类型编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取业务类型说明。
     *
     * @return 业务类型说明
     */
    public String getDesc() {
        return desc;
    }
}
