package com.jasic.aftersales.system.domain.enums;

import lombok.Getter;

/**
 * 反馈提交来源类型枚举。
 *
 * <p>该枚举表达“来源是如何形成的”，与提交主体类型拆分存储，避免后续统计和排障时语义混淆。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@Getter
public enum FeedbackSubmitSourceTypeEnum {

    /** 终端用户提交且命中最新工单来源 */
    CUSTOMER_WORK_ORDER("CUSTOMER_WORK_ORDER", "终端用户工单派生"),

    /** 终端用户直接提交，未命中有效工单来源 */
    CUSTOMER_DIRECT("CUSTOMER_DIRECT", "终端用户直接提交"),

    /** 网点用户提交 */
    SERVICE_COMPANY("SERVICE_COMPANY", "网点提交");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String desc;

    FeedbackSubmitSourceTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
