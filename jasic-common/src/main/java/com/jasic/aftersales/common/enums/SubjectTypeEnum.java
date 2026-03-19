package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 主体类型枚举
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Getter
public enum SubjectTypeEnum {

    /** 平台 */
    PLATFORM("PLATFORM", "平台"),

    /** 总部 */
    HQ("HQ", "总部"),

    /** 服务网点 */
    SERVICE("SERVICE", "服务网点");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    SubjectTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static SubjectTypeEnum getByCode(String code) {
        for (SubjectTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
