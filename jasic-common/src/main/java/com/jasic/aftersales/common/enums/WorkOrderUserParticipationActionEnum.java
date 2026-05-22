package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 工单用户级参与事实动作枚举。
 *
 * @author Zoro
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

    /**label 字段，用于当前类内部业务处理。*/
    private final String label;

    /**
     * 构造工单用户参与动作实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param label label，当前业务处理所需的输入值。
     */
    WorkOrderUserParticipationActionEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }
}




