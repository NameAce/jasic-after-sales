package com.jasic.aftersales.system.notify.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jasic.aftersales.system.notify.support.NotifyConstants;

/**
 * Supported notification template codes.
 *
 * @author Codex
 * @date 2026/04/20
 */
public enum NotifyTemplateCodeEnum {

    WORK_ORDER_ASSIGNED(
            NotifyConstants.TEMPLATE_CODE_WORK_ORDER_ASSIGNED,
            "Work order assigned",
            NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode(),
            NotifyBizTypeEnum.WORK_ORDER.getCode(),
            NotifyConstants.MESSAGE_TYPE_TODO
    ),

    WORK_ORDER_EVALUATION_INVITE(
            NotifyConstants.TEMPLATE_CODE_WORK_ORDER_EVALUATION_INVITE,
            "Work order evaluation invite",
            NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode(),
            NotifyBizTypeEnum.WORK_ORDER.getCode(),
            NotifyConstants.MESSAGE_TYPE_EXTERNAL_NOTIFY
    );

    /**
     * 通知模板编码编码。
     *
     * @param code 参数
     * @param desc 参数
     * @param eventType 参数
     * @param bizType 参数
     * @param messageType 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    private final String eventType;

    private final String bizType;

    private final String messageType;

    /**
     * 构造通知模板编码实例。
     *
     * @param code 参数
     * @param desc 参数
     * @param eventType 参数
     * @param bizType 参数
     * @param messageType 参数
     */
    NotifyTemplateCodeEnum(String code, String desc, String eventType, String bizType, String messageType) {
        this.code = code;
        this.desc = desc;
        this.eventType = eventType;
        this.bizType = bizType;
        this.messageType = messageType;
    }

    /**
     * 根据编码查询通知模板编码。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static NotifyTemplateCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (NotifyTemplateCodeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 获取By事件类型。
     *
     * @param eventType 参数
     * @return 处理结果
     */
    public static NotifyTemplateCodeEnum getByEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedEventType = eventType.trim();
        if (normalizedEventType.isEmpty()) {
            return null;
        }
        for (NotifyTemplateCodeEnum value : values()) {
            if (value.eventType.equals(normalizedEventType)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析通知模板编码。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static NotifyTemplateCodeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        NotifyTemplateCodeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify template code: " + code);
        }
        return value;
    }

    /**
     * 获取通知模板编码编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取通知模板编码描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取事件类型。
     *
     * @return 处理结果
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 获取业务类型。
     *
     * @return 处理结果
     */
    public String getBizType() {
        return bizType;
    }

    /**
     * 获取消息类型。
     *
     * @return 处理结果
     */
    public String getMessageType() {
        return messageType;
    }
}




