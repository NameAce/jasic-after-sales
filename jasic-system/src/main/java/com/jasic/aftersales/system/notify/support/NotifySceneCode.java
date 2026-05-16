package com.jasic.aftersales.system.notify.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知场景编码枚举。
 *
 * <p>该枚举用于沉淀当前系统允许维护和运行的通知场景编码，
 * 让后台模板配置、运行时 handler 和排障日志都引用同一组 `sceneCode` 常量。
 * 它只表达场景身份，不负责模板内容、接收人解析和渠道发送。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public enum NotifySceneCode {

    /** 工单派单后给维修员生成站内待办。 */
    WORK_ORDER_ASSIGNED_TODO("WORK_ORDER_ASSIGNED_TODO", "工单派单 - 站内待办 - 维修员"),

    /** 工单完成后给 C 端客户发送评价邀请小程序订阅消息。 */
    WORK_ORDER_EVALUATION_INVITE_MP_C("WORK_ORDER_EVALUATION_INVITE_MP_C", "客户评价邀请 - 小程序订阅消息 - C端客户");

    /**
     * 场景编码。
     */
    private final String code;

    /**
     * 场景名称。
     */
    private final String desc;

    /**
     * 构造通知场景编码枚举。
     *
     * @param code 场景编码
     * @param desc 场景名称
     */
    NotifySceneCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知场景。
     *
     * @param code 场景编码
     * @return 命中的场景；未命中时返回 {@code null}
     */
    public static NotifySceneCode getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifySceneCode value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按编码强校验解析通知场景。
     *
     * @param code 场景编码
     * @return 命中的场景；传入空值时返回 {@code null}
     */
    @JsonCreator
    public static NotifySceneCode fromCode(String code) {
        NotifySceneCode value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知场景编码：" + code);
        }
        return value;
    }

    /**
     * 获取场景编码。
     *
     * @return 场景编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取场景名称。
     *
     * @return 场景名称
     */
    public String getDesc() {
        return desc;
    }
}
