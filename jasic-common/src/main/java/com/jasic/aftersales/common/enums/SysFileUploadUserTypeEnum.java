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
     * 系统文件上传用户类型编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造系统文件上传用户类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    SysFileUploadUserTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询文件上传用户类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static SysFileUploadUserTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
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
     * 根据编码解析文件上传用户类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static SysFileUploadUserTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        SysFileUploadUserTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的文件上传用户类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取文件上传用户类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取文件上传用户类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




