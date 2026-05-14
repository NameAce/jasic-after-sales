package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知待办失效原因枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyInvalidReasonEnum {

    /** 工单转派导致失效。 */
    TRANSFERRED("TRANSFERRED", "已转派"),

    /** 工单关闭导致失效。 */
    WORK_ORDER_CLOSED("WORK_ORDER_CLOSED", "工单已关闭"),

    /** 工单完成导致失效。 */
    WORK_ORDER_COMPLETED("WORK_ORDER_COMPLETED", "工单已完成"),

    /** 工单作废导致失效。 */
    WORK_ORDER_CANCELLED("WORK_ORDER_CANCELLED", "工单已作废");

    /**
     * 通知无效原因编码。
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知无效原因实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyInvalidReasonEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知无效原因。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyInvalidReasonEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyInvalidReasonEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知无效原因。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyInvalidReasonEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyInvalidReasonEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知失效原因编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知无效原因编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知无效原因描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




