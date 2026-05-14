package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知事件状态枚举。
 *
 * @author Codex
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
    FAILED("FAILED", "失败");

    /**
     * 通知事件状态编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知事件状态实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyEventStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知事件状态。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyEventStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
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
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyEventStatusEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyEventStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知事件状态编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知事件状态编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知事件状态描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




