package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 品牌类型枚举
 *
 * @author Zoro
 * @date 2026/04/08
 */
public enum BrandTypeEnum {

    /** 佳士品牌 */
    JASIC("JASIC", "佳士品牌"),

    /** 非佳士品牌 */
    NON_JASIC("NON_JASIC", "非佳士品牌");

    /** 编码 */
    private final String code;

    /** 展示名称 */
    private final String label;

    /**
     * 构造品牌类型实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param label label，当前业务处理所需的输入值。
     */
    BrandTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据编码获取枚举。
     *
     * @param code 编码
     * @return 枚举值
     */
    public static BrandTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (BrandTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 供 JSON 反序列化使用。
     *
     * @param code 编码
     * @return 枚举值
     */
    @JsonCreator
    public static BrandTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        BrandTypeEnum brandType = getByCode(code);
        if (brandType == null) {
            throw new IllegalArgumentException("不支持的品牌类型编码：" + code);
        }
        return brandType;
    }

    /**
     * 是否为佳士品牌。
     *
     * @return 是否佳士品牌
     */
    public boolean isJasic() {
        return this == JASIC;
    }

    /**
     * 是否为非佳士品牌。
     *
     * @return 是否非佳士品牌
     */
    public boolean isNonJasic() {
        return this == NON_JASIC;
    }

    /**
     * 获取展示名称。
     *
     * @return 展示名称
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取品牌类型编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }
}




