package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 通知类型枚举。
 *
 * <p>该枚举用于表达通知场景下的具体通知目标类型。
 * 阶段二开始，小程序订阅消息按 B/C 端拆成两个独立目标，
 * 避免继续把“通知目标”和“渠道类型”混为一个概念。</p>
 *
 * @author Zoro
 * @date 2026/05/15
 */
public enum NotifyTypeEnum {

    /** 站内消息。*/
    IN_APP_MESSAGE("IN_APP_MESSAGE", "站内消息"),

    /** 站内待办。*/
    IN_APP_TODO("IN_APP_TODO", "站内待办"),

    /** 小程序订阅消息(B端)。*/
    MP_SUBSCRIBE_B("MP_SUBSCRIBE_B", "小程序订阅消息(B端)"),

    /** 小程序订阅消息(C端)。*/
    MP_SUBSCRIBE_C("MP_SUBSCRIBE_C", "小程序订阅消息(C端)");

    /**
     * 通知类型编码。
     */
    private final String code;

    /**
     * 通知类型说明。
     */
    private final String desc;

    /**构造 NotifyTypeEnum 实例，初始化当前对象在业务流程中需要持有的基础数据。
@param code 业务编码，用于匹配枚举、配置或外部系统数据。
@param desc desc 字段参数。*/
    NotifyTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**getByCode 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param code 业务编码，用于匹配枚举、配置或外部系统数据。
@return 查询或解析得到的业务对象。*/
    public static NotifyTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断是否为站内目标。
     *
     * @return `true` 表示当前目标落站内消息表
     */
    public boolean isInAppTarget() {
        return this == IN_APP_MESSAGE || this == IN_APP_TODO;
    }

    /**
     * 判断是否为小程序订阅消息目标。
     *
     * @return `true` 表示当前目标属于小程序订阅消息
     */
    public boolean isMiniProgramSubscribeTarget() {
        return this == MP_SUBSCRIBE_B || this == MP_SUBSCRIBE_C;
    }

    /**
     * 判断指定编码是否为小程序订阅消息目标。
     *
     * @param code 目标类型编码
     * @return `true` 表示属于小程序订阅消息目标
     */
    public static boolean isMiniProgramSubscribeTarget(String code) {
        NotifyTypeEnum value = getByCode(code);
        return value != null && value.isMiniProgramSubscribeTarget();
    }

    /**fromCode 处理逻辑，服务于当前类的业务编排和数据转换。
@param code 业务编码，用于匹配枚举、配置或外部系统数据。
@return 处理后的业务结果。*/
    @JsonCreator
    public static NotifyTypeEnum fromCode(String code) {
        NotifyTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的通知类型编码：" + code);
        }
        return value;
    }

    /**getCode 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @JsonValue
    public String getCode() {
        return code;
    }

    /**getDesc 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    public String getDesc() {
        return desc;
    }
}
