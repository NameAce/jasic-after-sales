package com.jasic.aftersales.system.domain.enums;

import lombok.Getter;

/**
 * 反馈提交主体类型枚举。
 *
 * @author Codex
 * @date 2026/05/28
 */
@Getter
public enum FeedbackSubmitterTypeEnum {

    /** 终端用户 */
    CUSTOMER("CUSTOMER", "终端用户"),

    /** 网点用户 */
    SERVICE_COMPANY_USER("SERVICE_COMPANY_USER", "网点用户");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    FeedbackSubmitterTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
