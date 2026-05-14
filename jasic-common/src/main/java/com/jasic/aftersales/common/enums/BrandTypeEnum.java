package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 品牌类型枚举
 *
 * @author Codex
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
     * ?????
     *
     * @return ?????
     */
    @JsonValue
    public String getCode() {
        return code;
    }
}
