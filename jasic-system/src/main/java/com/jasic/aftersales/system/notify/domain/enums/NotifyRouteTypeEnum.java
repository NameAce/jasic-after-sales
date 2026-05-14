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

    WORK_ORDER_DETAIL(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL, "Work order detail"),
    WORK_ORDER_EVALUATE(NotifyConstants.ROUTE_TYPE_WORK_ORDER_EVALUATE, "Work order evaluate");

    /**
     * 通知路由类型编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造通知路由类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    NotifyRouteTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知路由类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyRouteTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
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

    /**
     * 根据编码解析通知路由类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyRouteTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyRouteTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify route type: " + code);
        }
        return value;
    }

    /**
     * 获取通知路由类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知路由类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




