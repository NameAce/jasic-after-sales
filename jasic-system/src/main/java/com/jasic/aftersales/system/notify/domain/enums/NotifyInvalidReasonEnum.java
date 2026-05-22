package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知待办失效原因枚举。
 *
 * @author Zoro
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

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造通知无效原因实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    NotifyInvalidReasonEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知无效原因。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    public static NotifyInvalidReasonEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
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
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    @JsonCreator
    public static NotifyInvalidReasonEnum fromCode(String code) {
        NotifyInvalidReasonEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知失效原因编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知无效原因编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知无效原因描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}




