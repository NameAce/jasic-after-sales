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
    ALL("ALL", "全部数据", 3),

    /** 大区数据 */
    REGION("REGION", "大区数据", 2),

    /** 仅本人数据 */
    SELF("SELF", "仅本人数据", 1);

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    /** 权重（越小范围越小，冲突时取最小） */
    private final int weight;

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
     * 取两个范围中更小的那个（多角色冲突时使用）
     *
     * @param other 另一个范围
     * @return 更小的范围
     */
    public DataScopeEnum min(DataScopeEnum other) {
        if (other == null) {
            return this;
        }
        return this.weight <= other.weight ? this : other;
    }
}
