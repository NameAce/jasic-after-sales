package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jasic.aftersales.system.notify.support.NotifyConstants;

/**
 * Notification template source.
 *
 * @author Codex
 * @date 2026/04/20
 */
public enum NotifyTemplateSourceEnum {

    BUILT_IN(NotifyConstants.TEMPLATE_SOURCE_BUILT_IN, "内置模板"),
    CUSTOM(NotifyConstants.TEMPLATE_SOURCE_CUSTOM, "自定义模板");

    /**
     * 通知模板来源编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知模板来源实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyTemplateSourceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知模板来源。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyTemplateSourceEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTemplateSourceEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知模板来源。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyTemplateSourceEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyTemplateSourceEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify template source: " + code);
        }
        return value;
    }

    /**
     * 获取通知模板来源编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知模板来源描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




