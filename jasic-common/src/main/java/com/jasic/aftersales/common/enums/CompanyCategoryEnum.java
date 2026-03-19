package com.jasic.aftersales.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 公司业务分类枚举
 * <p>
 * 用于按业务语义查询公司列表，避免前端硬编码 typeCode。
 * 类型编码变更时只需修改后端映射。
 * </p>
 *
 * @author Zoro
 * @date 2026/03/19
 */
@Getter
public enum CompanyCategoryEnum {

    /** 总部公司（subjectType=HQ 的类型） */
    HQ("HQ", "总部"),

    /** 一级网点（一级服务网点） */
    FIRST_LEVEL("FIRST_LEVEL", "一级网点"),

    /** 二级网点（二级服务网点） */
    SECOND_LEVEL("SECOND_LEVEL", "二级网点");

    /** 分类编码（API 参数值） */
    private final String code;

    /** 描述 */
    private final String desc;

    /** 一级网点对应的 typeCode 列表（兼容历史编码） */
    private static final List<String> FIRST_LEVEL_TYPE_CODES = Arrays.asList("SITE_FIRST", "FIRST");

    /** 二级网点对应的 typeCode 列表（兼容历史编码） */
    private static final List<String> SECOND_LEVEL_TYPE_CODES = Arrays.asList("SITE_SECOND", "SECOND");

    CompanyCategoryEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     */
    public static CompanyCategoryEnum getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (CompanyCategoryEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 获取一级网点对应的 typeCode 列表
     */
    public static List<String> getFirstLevelTypeCodes() {
        return Collections.unmodifiableList(FIRST_LEVEL_TYPE_CODES);
    }

    /**
     * 获取二级网点对应的 typeCode 列表
     */
    public static List<String> getSecondLevelTypeCodes() {
        return Collections.unmodifiableList(SECOND_LEVEL_TYPE_CODES);
    }
}
