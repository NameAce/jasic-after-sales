package com.jasic.aftersales.system.notify.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知场景编码枚举。
 *
 * <p>该枚举只描述“发生了什么业务通知场景”，不再把 B/C 端渠道类型直接编码进场景值本身。
 * 当前只保留方案确认的 6 个正式场景编码，不再暴露旧页面时期的派生常量名。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public enum NotifySceneCode {

    /** B 端接单通知。 */
    WORK_ORDER_ACCEPT("WORK_ORDER_ACCEPT", "B端接单通知"),

    /** B 端工单转入通知。 */
    WORK_ORDER_TRANSFER_IN("WORK_ORDER_TRANSFER_IN", "B端工单转入通知"),

    /** B 端工单派单通知。 */
    WORK_ORDER_ASSIGNED("WORK_ORDER_ASSIGNED", "B端工单派单通知"),

    /** C 端接单成功提醒。 */
    WORK_ORDER_ACCEPTED("WORK_ORDER_ACCEPTED", "C端接单成功提醒"),

    /** C 端网点转单通知。 */
    WORK_ORDER_TRANSFER_NOTICE("WORK_ORDER_TRANSFER_NOTICE", "C端网点转单通知"),

    /** C 端客户满意度评价通知。 */
    WORK_ORDER_EVALUATION_INVITE("WORK_ORDER_EVALUATION_INVITE", "C端客户满意度评价通知");

    /**
     * 场景编码。
     */
    private final String code;

    /**
     * 场景说明。
     */
    private final String desc;

    /**
     * 构造通知场景编码枚举。
     *
     * @param code 场景编码
     * @param desc 场景说明
     */
    NotifySceneCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按编码查询通知场景。
     *
     * @param code 场景编码
     * @return 命中的通知场景；未命中时返回 {@code null}
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
     * @return 命中的通知场景；传入 {@code null} 时返回 {@code null}
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
     * 获取场景说明。
     *
     * @return 场景说明
     */
    public String getDesc() {
        return desc;
    }
}
