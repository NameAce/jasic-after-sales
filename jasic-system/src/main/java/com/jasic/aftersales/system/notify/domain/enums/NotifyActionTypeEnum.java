package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知消息动作类型枚举。
 *
 * @author Zoro
 * @date 2026/04/18
 */
public enum NotifyActionTypeEnum {

    /** 创建消息。 */
    CREATE("CREATE", "创建"),

    /** 标记已读。 */
    READ("READ", "已读"),

    /** 标记已处理。 */
    DONE("DONE", "已处理"),

    /** 标记失效。 */
    INVALID("INVALID", "失效");

    /**
     * 通知动作类型编码。
     */
    private final String code;

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造通知动作类型实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    NotifyActionTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知动作类型。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    public static NotifyActionTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyActionTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知动作类型。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    @JsonCreator
    public static NotifyActionTypeEnum fromCode(String code) {
        NotifyActionTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知动作类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知动作类型编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知动作类型描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}




