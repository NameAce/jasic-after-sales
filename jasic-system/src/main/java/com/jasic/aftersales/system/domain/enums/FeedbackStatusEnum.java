package com.jasic.aftersales.system.domain.enums;

import lombok.Getter;

/**
 * 反馈状态枚举。
 *
 * @author Codex
 * @date 2026/05/28
 */
@Getter
public enum FeedbackStatusEnum {

    /** 未受理 */
    UNACCEPTED("UNACCEPTED", "未受理"),

    /** 已受理 */
    ACCEPTED("ACCEPTED", "已受理");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    FeedbackStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
