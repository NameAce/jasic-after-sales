package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知渠道类型枚举。
 *
 * <p>当前模板配置重构只收口已经确认的外部渠道类型，避免后台在阶段一就暴露
 * 短信、邮件等尚未进入正式链路的能力。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public enum NotifyChannelTypeEnum {

    /** 小程序订阅消息渠道。 */
    MP_SUBSCRIBE("MP_SUBSCRIBE", "小程序订阅消息");

    /**
     * 渠道类型编码。
     */
    private final String code;

    /**
     * 渠道类型说明。
     */
    private final String desc;

    /**
     * 构造通知渠道类型枚举。
     *
     * @param code 渠道类型编码
     * @param desc 渠道类型说明
     */
    NotifyChannelTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知渠道类型。
     *
     * @param code 渠道类型编码
     * @return 命中的渠道类型；未命中时返回 {@code null}
     */
    public static NotifyChannelTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
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
     * 按编码解析通知渠道类型。
     *
     * <p>该方法用于强校验场景，阶段一只允许 `MP_SUBSCRIBE` 进入模板渠道配置链路。</p>
     *
     * @param code 渠道类型编码
     * @return 命中的渠道类型；入参为 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyChannelTypeEnum fromCode(String code) {
        NotifyChannelTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知渠道类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取渠道类型编码。
     *
     * @return 渠道类型编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取渠道类型说明。
     *
     * @return 渠道类型说明
     */
    public String getDesc() {
        return desc;
    }
}
