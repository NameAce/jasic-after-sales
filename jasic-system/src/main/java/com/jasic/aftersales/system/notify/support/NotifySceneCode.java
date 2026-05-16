package com.jasic.aftersales.system.notify.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知场景编码枚举。
 *
 * <p>本枚举在阶段一开始切换为“统一通知场景”语义，`sceneCode` 只描述业务事件本身，
 * 不再把站内待办、小程序订阅等通知目标含义编码进场景值里。
 * 为了减少阶段一对现有运行时代码的冲击，历史常量名暂时保留，但其实际编码已经收口到新的统一场景编码。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public enum NotifySceneCode {

    /**
     * 历史常量名沿用旧“工单派单待办”命名，实际编码已统一为工单派单场景。
     */
    WORK_ORDER_ASSIGNED_TODO("WORK_ORDER_ASSIGNED", "工单派单"),

    /**
     * 历史常量名沿用旧“小程序评价邀请”命名，实际编码已统一为客户评价邀请场景。
     */
    WORK_ORDER_EVALUATION_INVITE_MP_C("WORK_ORDER_EVALUATION_INVITE", "客户评价邀请");

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
     * @return 命中的通知场景；传入空值时返回 {@code null}
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
