package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文件上传用户类型枚举
 *
 * @author Codex
 * @date 2026/04/07
 */
public enum SysFileUploadUserTypeEnum {

    /** 系统用户 */
    SYSTEM("SYSTEM", "系统用户"),

    /** 客户用户 */
    CUSTOMER("CUSTOMER", "客户用户");

    /**
     * ?? SysFileUploadUserTypeEnum ?????
     *
     * @param code ??
     * @param desc ??
     * @return ????
     */
    private final String code;

    private final String desc;

    SysFileUploadUserTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * ??By Code?
     *
     * @param code ??
     * @return ????
     */
    public static SysFileUploadUserTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (SysFileUploadUserTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * ?? fromCode ?????
     *
     * @param code ??
     * @return ????
     */
    @JsonCreator
    public static SysFileUploadUserTypeEnum fromCode(String code) {
        SysFileUploadUserTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的文件上传用户类型编码：" + code);
        }
        return value;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    public String getDesc() {
        return desc;
    }
}
