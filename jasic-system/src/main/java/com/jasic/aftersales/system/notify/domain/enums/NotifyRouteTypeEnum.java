package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jasic.aftersales.system.notify.support.NotifyConstants;

/**
 * Supported notification route types.
 *
 * @author Zoro
 * @date 2026/04/20
 */
public enum NotifyRouteTypeEnum {

    WORK_ORDER_DETAIL(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL, "Work order detail"),
    WORK_ORDER_EVALUATE(NotifyConstants.ROUTE_TYPE_WORK_ORDER_EVALUATE, "Work order evaluate");

    /**
     * 通知路由类型编码。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private final String code;

    /**desc 字段，用于当前类内部业务处理。*/
    private final String desc;

    /**
     * 构造通知路由类型实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param desc desc，当前业务处理所需的输入值。
     */
    NotifyRouteTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询通知路由类型。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
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

    /**
     * 根据编码解析通知路由类型。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    @JsonCreator
    public static NotifyRouteTypeEnum fromCode(String code) {
        NotifyRouteTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify route type: " + code);
        }
        return value;
    }

    /**
     * 获取通知路由类型编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知路由类型描述。
     *
     * @return 业务处理结果
     */
    public String getDesc() {
        return desc;
    }
}




