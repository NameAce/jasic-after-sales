package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知渠道场景枚举。
 *
 * <p>该枚举只表达外部渠道所属的小程序端口。
 * 站内消息和站内待办不使用该枚举，模板上的 `channel_scene` 允许为空。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public enum NotifyChannelSceneEnum {

    /** B端小程序。 */
    B("B", "B端小程序"),

    /** C端小程序。 */
    C("C", "C端小程序");

    /**
     * 渠道场景编码。
     */
    private final String code;

    /**
     * 渠道场景说明。
     */
    private final String desc;

    /**
     * 构造通知渠道场景枚举。
     *
     * @param code 渠道场景编码
     * @param desc 渠道场景说明
     */
    NotifyChannelSceneEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知渠道场景。
     *
     * @param code 渠道场景编码
     * @return 命中的渠道场景；未命中时返回 {@code null}
     */
    public static NotifyChannelSceneEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyChannelSceneEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按编码解析通知渠道场景。
     *
     * @param code 渠道场景编码
     * @return 命中的渠道场景；入参为 {@code null} 时返回 {@code null}
     */
    @JsonCreator
    public static NotifyChannelSceneEnum fromCode(String code) {
        NotifyChannelSceneEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知渠道场景编码：" + code);
        }
        return value;
    }

    /**
     * 获取渠道场景编码。
     *
     * @return 渠道场景编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取渠道场景说明。
     *
     * @return 渠道场景说明
     */
    public String getDesc() {
        return desc;
    }
}
