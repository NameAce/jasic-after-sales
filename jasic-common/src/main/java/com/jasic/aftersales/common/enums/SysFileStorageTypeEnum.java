package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文件存储类型枚举
 *
 * @author Zoro
 * @date 2026/04/07
 */
public enum SysFileStorageTypeEnum {

    /** 阿里云 OSS */
        OSS("OSS", "阿里云OSS");

    /**
     * 文件存储类型编码。
     */
    private final String code;

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造系统文件存储类型实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    SysFileStorageTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询文件存储类型。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    public static SysFileStorageTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (SysFileStorageTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析文件存储类型。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    @JsonCreator
    public static SysFileStorageTypeEnum fromCode(String code) {
        SysFileStorageTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的文件存储类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取文件存储类型编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取文件存储类型描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}





