package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知消息动作类型枚举。
 *
 * @author Codex
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

    private final String desc;

    /**
     * 构造通知动作类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyActionTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知动作类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyActionTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
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
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyActionTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyActionTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知动作类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知动作类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知动作类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




