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
     * ?? NotifyTemplateCodeEnum ?????
     *
     * @param code ??
     * @param desc ??
     * @param eventType ??
     * @param bizType ????
     * @param messageType ??
     * @return ????
     */
    private final String code;

    private final String desc;

    private final String eventType;

    private final String bizType;

    private final String messageType;

    NotifyTemplateCodeEnum(String code, String desc, String eventType, String bizType, String messageType) {
        this.code = code;
        this.desc = desc;
        this.eventType = eventType;
        this.bizType = bizType;
        this.messageType = messageType;
    }

    /**
     * ??By Code?
     *
     * @param code ??
     * @return ????
     */
    public static NotifyTemplateCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
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
     * ??By Event Type?
     *
     * @param eventType ??
     * @return ????
     */
    public static NotifyTemplateCodeEnum getByEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
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
     * ?? fromCode ?????
     *
     * @param code ??
     * @return ????
     */
    @JsonCreator
    public static NotifyTemplateCodeEnum fromCode(String code) {
        NotifyTemplateCodeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify template code: " + code);
        }
        return value;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    public String getDesc() {
        return desc;
    }

    /**
     * ??Event Type?
     *
     * @return ?????
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * ??Biz Type?
     *
     * @return ?????
     */
    public String getBizType() {
        return bizType;
    }

    /**
     * ??Message Type?
     *
     * @return ?????
     */
    public String getMessageType() {
        return messageType;
    }
}
