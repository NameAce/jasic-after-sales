package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 数据范围枚举
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Getter
public enum DataScopeEnum {

    /** 全部数据 */
    ALL("ALL", "全部数据", 4),

    /** 当前公司数据 */
    COMPANY("COMPANY", "当前公司数据", 3),

    /** 大区数据 */
    REGION("REGION", "大区数据", 2),

    /** 仅本人数据 */
    SELF("SELF", "仅本人数据", 1);

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    /** 权重，值越大表示范围越大 */
        private final int weight;

    /**
     * 构造数据范围实例。
     */
    DataScopeEnum(String code, String desc, int weight) {
        this.code = code;
        this.desc = desc;
        this.weight = weight;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static DataScopeEnum getByCode(String code) {
        for (DataScopeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return SELF;
    }

    /**
     * 按主体类型将数据范围收敛到合法值。
     *
     * @param scopeCode   数据范围编码
     * @param subjectType 主体类型编码
     * @return 合法化后的数据范围
     */
    public static DataScopeEnum normalize(String scopeCode, String subjectType) {
        return getByCode(scopeCode).normalizeForSubject(subjectType);
    }

    /**
     * 按主体类型将当前数据范围收敛到合法值。
     *
     * @param subjectType 主体类型编码
     * @return 合法化后的数据范围
     */
    public DataScopeEnum normalizeForSubject(String subjectType) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        SubjectTypeEnum subjectTypeEnum = SubjectTypeEnum.getByCode(subjectType);
        if (subjectTypeEnum == null) {
            return SELF;
        }
        switch (subjectTypeEnum) {
            case PLATFORM:
                return ALL;
            case HQ:
                return (this == ALL || this == REGION || this == SELF) ? this : SELF;
            case SERVICE:
                return (this == ALL || this == COMPANY || this == SELF) ? this : SELF;
            default:
                return SELF;
        }
    }

    /**
     * 取两个范围中更大的那个，多角色冲突时使用。
     *
     * @param other 另一个范围
     * @return 更大的范围
     */
    public DataScopeEnum max(DataScopeEnum other) {
        if (other == null) {
            return this;
        }
        return this.weight >= other.weight ? this : other;
    }
}





