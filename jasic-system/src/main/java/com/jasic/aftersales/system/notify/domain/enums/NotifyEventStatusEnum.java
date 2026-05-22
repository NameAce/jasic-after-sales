package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知事件状态枚举。
 *
 * @author Zoro
 * @date 2026/04/18
 */
public enum NotifyEventStatusEnum {

    /** 新建待消费。 */
    NEW("NEW", "新建"),

    /** 消费处理中。 */
    PROCESSING("PROCESSING", "处理中"),

    /** 消费成功。 */
    SUCCESS("SUCCESS", "成功"),

    /** 消费失败。 */
    FAILED("FAILED", "失败"),

    /** 进入死信。 */
    DEAD("DEAD", "死信");

    /**
     * 通知事件状态编码。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private final String code;

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造通知事件状态实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    NotifyEventStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知事件状态。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    public static NotifyEventStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyEventStatusEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知事件状态。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    @JsonCreator
    public static NotifyEventStatusEnum fromCode(String code) {
        NotifyEventStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知事件状态编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知事件状态编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知事件状态描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}




