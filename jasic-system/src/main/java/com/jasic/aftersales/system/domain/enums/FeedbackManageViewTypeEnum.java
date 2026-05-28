package com.jasic.aftersales.system.domain.enums;

import lombok.Getter;

/**
 * 反馈后台管理视图类型枚举。
 *
 * @author Codex
 * @date 2026/05/28
 */
@Getter
public enum FeedbackManageViewTypeEnum {

    /** 未受理视图 */
    UNACCEPTED("UNACCEPTED", "未受理"),

    /** 已受理视图 */
    ACCEPTED("ACCEPTED", "已受理"),

    /** 全部视图 */
    ALL("ALL", "全部");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    FeedbackManageViewTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 解析后台视图类型。
     *
     * @param code 前端或调用方传入的视图编码
     * @return 匹配到的枚举；未匹配时返回 null
     */
    public static FeedbackManageViewTypeEnum getByCode(String code) {
        for (FeedbackManageViewTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
