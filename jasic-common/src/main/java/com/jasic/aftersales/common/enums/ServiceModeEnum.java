package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 服务方式枚举
 *
 * @author Zoro
 * @date 2026/04/08
 */
public enum ServiceModeEnum {

    /** 寄修 */
    MAIL("MAIL", "寄修"),

    /** 到店维修 */
    STORE("STORE", "到店维修");

    /** 编码 */
    private final String code;

    /** 展示名称 */
    private final String label;

    /**
     * 构造服务模式实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param label label，当前业务处理所需的输入值。
     */
    ServiceModeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据编码获取枚举。
     *
     * @param code 编码
     * @return 枚举值
     */
    public static ServiceModeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (ServiceModeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 供 JSON 反序列化使用。
     *
     * @param code 编码
     * @return 枚举值
     */
    @JsonCreator
    public static ServiceModeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        ServiceModeEnum serviceMode = getByCode(code);
        if (serviceMode == null) {
            throw new IllegalArgumentException("不支持的服务方式编码：" + code);
        }
        return serviceMode;
    }

    /**
     * 是否为寄修。
     *
     * @param code 服务方式编码
     * @return true 表示寄修
     */
    public static boolean isMail(String code) {
        return MAIL.code.equals(code);
    }

    /**
     * 是否为到店维修。
     *
     * @param code 服务方式编码
     * @return true 表示到店维修
     */
    public static boolean isStore(String code) {
        return STORE.code.equals(code);
    }

    /**
     * 根据编码解析展示名称。
     *
     * @param code 编码
     * @return 展示名称；无法识别时返回原值
     */
    public static String resolveLabel(String code) {
        ServiceModeEnum serviceMode = getByCode(code);
        return serviceMode == null ? code : serviceMode.getLabel();
    }

    /**
     * 获取展示名称。
     *
     * @return 展示名称
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取服务模式编码。
     *
     * @return 业务处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }
}




