package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文件访问级别枚举
 *
 * @author Codex
 * @date 2026/04/07
 */
public enum SysFileAccessLevelEnum {

    /** 私有文件 */
        PRIVATE("PRIVATE", "私有文件");

    /**
     * 文件访问Level编码。
     */
    private final String code;

    private final String desc;

    /**
     * 构造系统文件访问Level实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    SysFileAccessLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询文件访问Level。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static SysFileAccessLevelEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (SysFileAccessLevelEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析文件访问Level。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static SysFileAccessLevelEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        SysFileAccessLevelEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的文件访问级别编码：" + code);
        }
        return value;
    }

    /**
     * 获取文件访问Level编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取文件访问Level描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}





