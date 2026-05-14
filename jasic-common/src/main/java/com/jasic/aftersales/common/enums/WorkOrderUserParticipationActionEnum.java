package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 工单用户级参与事实动作枚举。
 *
 * @author Codex
 * @date 2026/04/14
 */
@Getter
public enum WorkOrderUserParticipationActionEnum {

    /** 维修员接单。 */
    TECH_ACCEPT("TECH_ACCEPT", "维修员接单"),

    /** 报价。 */
    QUOTE("QUOTE", "报价"),

    /** 维修登记。 */
    REPAIR("REPAIR", "维修登记"),

    /** 复检。 */
    REVIEW("REVIEW", "复检");

    /**
     * 工单用户参与动作编码。
     */
    private final String code;

    private final String label;

    /**
     * 构造工单用户参与动作实例。
     *
     * @param code 参数
     * @param label 参数
     */
    WorkOrderUserParticipationActionEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }
}




