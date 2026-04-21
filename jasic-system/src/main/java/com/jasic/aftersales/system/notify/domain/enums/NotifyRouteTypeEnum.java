package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jasic.aftersales.system.notify.support.NotifyConstants;

/**
 * Supported notification route types.
 *
 * @author Codex
 * @date 2026/04/20
 */
public enum NotifyRouteTypeEnum {

    WORK_ORDER_DETAIL(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL, "工单详情");

    private final String code;

    private final String desc;

    NotifyRouteTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotifyRouteTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyRouteTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    @JsonCreator
    public static NotifyRouteTypeEnum fromCode(String code) {
        NotifyRouteTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify route type: " + code);
        }
        return value;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
