package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知待办状态枚举。
 *
 * @author Codex
 * @date 2026/04/18
 */
public enum NotifyTodoStatusEnum {

    /** 待处理且未读。 */
    PENDING("PENDING", "待处理"),

    /** 已读但未处理。 */
    READ("READ", "已读"),

    /** 已通过真实业务动作处理。 */
    DONE("DONE", "已处理"),

    /** 已失效。 */
    INVALID("INVALID", "已失效");

    /**
     * 通知待办状态编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知待办状态实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyTodoStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知待办状态。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyTodoStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTodoStatusEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知待办状态。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyTodoStatusEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyTodoStatusEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知待办状态编码：" + code);
        }
        return value;
    }

    /**
     * 获取通知待办状态编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知待办状态描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




