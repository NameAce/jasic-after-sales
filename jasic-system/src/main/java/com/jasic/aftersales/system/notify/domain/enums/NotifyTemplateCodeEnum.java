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
            "工单派单通知",
            NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode(),
            NotifyBizTypeEnum.WORK_ORDER.getCode(),
            NotifyConstants.MESSAGE_TYPE_TODO
    );

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

    @JsonCreator
    public static NotifyTemplateCodeEnum fromCode(String code) {
        NotifyTemplateCodeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("Unsupported notify template code: " + code);
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

    public String getEventType() {
        return eventType;
    }

    public String getBizType() {
        return bizType;
    }

    public String getMessageType() {
        return messageType;
    }
}
