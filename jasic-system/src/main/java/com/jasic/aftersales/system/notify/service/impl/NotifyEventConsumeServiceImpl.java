package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTemplateCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventConsumeService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Notify event consume service implementation.
 *
 * @author Codex
 * @date 2026/04/18
 */
@Slf4j
@Service
public class NotifyEventConsumeServiceImpl implements NotifyEventConsumeService {

    @Resource
    private NotifyEventService notifyEventService;

    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyMessageLogService notifyMessageLogService;

    @Resource
    private NotifyTemplateService notifyTemplateService;

    @Resource
    private NotifyDispatchService notifyDispatchService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public int consumePendingEvents() {
        List<SysNotifyEvent> events = notifyEventService.listConsumableEvents(
                LocalDateTime.now(),
                NotifyConstants.EVENT_CONSUME_BATCH_SIZE
        );
        int successCount = 0;
        for (SysNotifyEvent event : events) {
            if (!notifyEventService.markProcessing(event.getId())) {
                continue;
            }
            try {
                transactionTemplate.execute(status -> {
                    consumeSingleEvent(event.getId());
                    return null;
                });
                successCount++;
            } catch (Exception ex) {
                log.error("Consume notify event failed. eventId={}, eventKey={}", event.getId(), event.getEventKey(), ex);
                markEventFailed(event.getId(), ex);
            }
        }
        return successCount;
    }

    private void consumeSingleEvent(Long eventId) {
        SysNotifyEvent event = getRequiredProcessingEvent(eventId);
        if (NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode().equals(event.getEventType())) {
            consumeAssignedEvent(event);
        } else if (NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode().equals(event.getEventType())) {
            consumeEvaluationInviteEvent(event);
        } else {
            throw new ServiceException("Unsupported notify eventType: " + event.getEventType());
        }
        notifyEventService.markSuccess(event.getId());
    }

    private void consumeAssignedEvent(SysNotifyEvent event) {
        NotifyAssignedEventDTO payload = parseAssignedPayload(event);
        if (NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())) {
            invalidateTransferredTodos(event, payload);
        }
        createPendingMessageIfAbsent(event, payload);
    }

    private void consumeEvaluationInviteEvent(SysNotifyEvent event) {
        NotifyEvaluationInviteEventDTO payload = parseEvaluationInvitePayload(event);
        String templateCode = NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE.getCode();
        boolean notifyEnabled = notifyTemplateService.isNotifyEnabled(templateCode);
        List<NotifyTemplateChannelVO> channels = notifyTemplateService.listChannelConfigs(templateCode);
        if (channels.isEmpty()) {
            createEvaluationDispatch(event, payload, null, notifyEnabled,
                    NotifyDispatchStatusEnum.SKIPPED.getCode(),
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "No channel config found for template " + templateCode);
            return;
        }
        for (NotifyTemplateChannelVO channel : channels) {
            createEvaluationDispatch(event, payload, channel, notifyEnabled, null, null, null);
        }
    }

    private SysNotifyEvent getRequiredProcessingEvent(Long eventId) {
        SysNotifyEvent event = notifyEventService.getById(eventId);
        if (event == null) {
            throw new ServiceException("Notify event not found");
        }
        if (!NotifyEventStatusEnum.PROCESSING.getCode().equals(event.getStatus())) {
            throw new ServiceException("Notify event is not processing");
        }
        return event;
    }

    private NotifyAssignedEventDTO parseAssignedPayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("Notify event payload cannot be blank");
        }
        NotifyAssignedEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyAssignedEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("Notify assigned payload parse failed");
        }
        if (payload == null) {
            throw new ServiceException("Notify assigned payload parse result is null");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("Notify assigned payload missing workOrderId");
        }
        if (payload.getNewAssignedUserId() == null) {
            throw new ServiceException("Notify assigned payload missing newAssignedUserId");
        }
        if (StrUtil.isBlank(payload.getAssignType())) {
            throw new ServiceException("Notify assigned payload missing assignType");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("Notify assigned payload workOrderId mismatch");
        }
        if (!Objects.equals(payload.getNewAssignedUserId(), event.getReceiverId())) {
            throw new ServiceException("Notify assigned payload receiver mismatch");
        }
        return payload;
    }

    private NotifyEvaluationInviteEventDTO parseEvaluationInvitePayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("Notify event payload cannot be blank");
        }
        NotifyEvaluationInviteEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyEvaluationInviteEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("Notify evaluation invite payload parse failed");
        }
        if (payload == null) {
            throw new ServiceException("Notify evaluation invite payload parse result is null");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("Notify evaluation invite payload missing workOrderId");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("Notify evaluation invite payload workOrderId mismatch");
        }
        return payload;
    }

    private void invalidateTransferredTodos(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        if (payload.getOldAssignedUserId() == null
                || Objects.equals(payload.getOldAssignedUserId(), payload.getNewAssignedUserId())) {
            return;
        }
        List<SysNotifyMessage> messages = notifyMessageService.listActiveTodoByBizAndReceiver(
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                payload.getWorkOrderId(),
                payload.getOldAssignedUserId()
        );
        if (messages.isEmpty()) {
            return;
        }
        LocalDateTime invalidTime = LocalDateTime.now();
        for (SysNotifyMessage message : messages) {
            boolean updated = notifyMessageService.invalidateMessage(
                    message.getId(),
                    NotifyInvalidReasonEnum.TRANSFERRED.getCode(),
                    invalidTime
            );
            if (!updated) {
                continue;
            }
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            message.setInvalidReason(NotifyInvalidReasonEnum.TRANSFERRED.getCode());
            message.setInvalidTime(invalidTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.INVALID.getCode(),
                    event.getOperatorId(),
                    "Invalidate transferred todo for previous assignee"
            ));
        }
    }

    private void createPendingMessageIfAbsent(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        if (notifyMessageService.getByEventId(event.getId()) != null) {
            return;
        }
        SysUser receiver = sysUserMapper.selectById(event.getReceiverId());
        String receiverName = resolveReceiverName(receiver, event.getReceiverId());
        NotifyTemplateRenderResult renderResult = notifyTemplateService.render(
                NotifyTemplateCodeEnum.WORK_ORDER_ASSIGNED.getCode(),
                buildAssignedTemplateVariables(event, payload, receiverName)
        );
        if (!renderResult.isNotifyEnabled()) {
            log.info("Notify template disabled, skip todo creation. eventId={}, templateCode={}",
                    event.getId(), NotifyTemplateCodeEnum.WORK_ORDER_ASSIGNED.getCode());
            return;
        }
        SysNotifyMessage message = new SysNotifyMessage();
        message.setEventId(event.getId());
        message.setMessageType(NotifyConstants.MESSAGE_TYPE_TODO);
        message.setEventType(event.getEventType());
        message.setBizType(event.getBizType());
        message.setBizId(event.getBizId());
        message.setBizNo(event.getBizNo());
        message.setReceiverId(event.getReceiverId());
        message.setReceiverName(receiverName);
        message.setTitle(renderResult.getTitle());
        message.setSummary(renderResult.getSummary());
        message.setRouteType(renderResult.getRouteType());
        message.setRouteValue(renderResult.getRouteValue());
        message.setTodoStatus(NotifyTodoStatusEnum.PENDING.getCode());
        message.setExtJson(buildAssignedMessageExt(payload));
        Long messageId = notifyMessageService.createMessage(message);
        message.setId(messageId);
        notifyMessageLogService.createLog(buildMessageLog(
                message,
                NotifyActionTypeEnum.CREATE.getCode(),
                event.getOperatorId(),
                NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())
                        ? "Create todo for transfer target"
                        : "Create todo for assigned technician"
        ));
    }

    private void createEvaluationDispatch(SysNotifyEvent event, NotifyEvaluationInviteEventDTO payload,
                                          NotifyTemplateChannelVO channel, boolean notifyEnabled,
                                          String forcedStatus, String forcedResultCode, String forcedResultMessage) {
        String channelType = channel == null ? NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode() : channel.getChannelType();
        SysNotifyDispatch dispatch = buildEvaluationDispatch(event, payload, channel, channelType);
        if (forcedStatus != null) {
            dispatch.setDispatchStatus(forcedStatus);
            dispatch.setResultCode(forcedResultCode);
            dispatch.setResultMessage(forcedResultMessage);
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        if (!notifyEnabled) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_TEMPLATE_DISABLED.getCode());
            dispatch.setResultMessage("Template " + dispatch.getTemplateCode() + " is disabled");
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        if (channel == null || Objects.equals(channel.getChannelEnabled(), 0)) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_DISABLED.getCode());
            dispatch.setResultMessage("Channel " + channelType + " is disabled");
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        NotifyTemplateChannelConfig config = buildChannelConfig(channel);
        dispatch.setPayloadJson(buildEvaluationDispatchPayload(dispatch.getTemplateCode(), channelType, config, payload));
        if (StrUtil.isBlank(dispatch.getReceiverAddress())) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode());
            dispatch.setResultMessage("Customer openid is missing");
        } else if (config == null
                || StrUtil.isBlank(config.getScene())
                || StrUtil.isBlank(config.getTemplateId())
                || StrUtil.isBlank(config.getPagePathTemplate())
                || config.getFieldMapping() == null
                || config.getFieldMapping().isEmpty()) {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode());
            dispatch.setResultMessage("Mini program channel config is incomplete");
        } else {
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.PENDING.getCode());
        }
        notifyDispatchService.createDispatch(dispatch);
    }

    private SysNotifyDispatch buildEvaluationDispatch(SysNotifyEvent event, NotifyEvaluationInviteEventDTO payload,
                                                      NotifyTemplateChannelVO channel, String channelType) {
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setEventId(event.getId());
        dispatch.setTemplateCode(NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE.getCode());
        dispatch.setChannelType(channelType);
        dispatch.setReceiverType(NotifyReceiverTypeEnum.CUSTOMER.getCode());
        dispatch.setReceiverId(payload.getCustomerId());
        dispatch.setReceiverAddress(StrUtil.trimToNull(payload.getCustomerOpenid()));
        dispatch.setBizType(event.getBizType());
        dispatch.setBizId(event.getBizId());
        dispatch.setBizNo(event.getBizNo());
        dispatch.setRetryCount(0);
        if (channel != null) {
            dispatch.setPayloadJson(buildEvaluationDispatchPayload(
                    dispatch.getTemplateCode(),
                    channelType,
                    buildChannelConfig(channel),
                    payload
            ));
        }
        return dispatch;
    }

    private NotifyTemplateChannelConfig buildChannelConfig(NotifyTemplateChannelVO channel) {
        if (channel == null) {
            return null;
        }
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setScene(channel.getChannelScene());
        config.setTemplateId(channel.getTemplateId());
        config.setPagePathTemplate(channel.getPagePathTemplate());
        config.setFieldMapping(channel.getFieldMapping());
        return config;
    }

    private String buildEvaluationDispatchPayload(String templateCode, String channelType,
                                                  NotifyTemplateChannelConfig channelConfig,
                                                  NotifyEvaluationInviteEventDTO payload) {
        NotifyDispatchPayload dispatchPayload = new NotifyDispatchPayload();
        dispatchPayload.setTemplateCode(templateCode);
        dispatchPayload.setChannelType(channelType);
        dispatchPayload.setChannelConfig(channelConfig);
        dispatchPayload.setVariables(buildEvaluationVariables(payload));
        return JSONUtil.toJsonStr(dispatchPayload);
    }

    private String resolveReceiverName(SysUser receiver, Long receiverId) {
        if (receiver == null) {
            return String.valueOf(receiverId);
        }
        String realName = StrUtil.trim(receiver.getRealName());
        if (StrUtil.isNotBlank(realName)) {
            return realName;
        }
        String username = StrUtil.trim(receiver.getUsername());
        return StrUtil.isNotBlank(username) ? username : String.valueOf(receiverId);
    }

    private Map<String, Object> buildAssignedTemplateVariables(SysNotifyEvent event, NotifyAssignedEventDTO payload,
                                                               String receiverName) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("bizId", event.getBizId());
        variables.put("bizNo", event.getBizNo());
        variables.put("receiverId", event.getReceiverId());
        variables.put("receiverName", receiverName);
        variables.put("operatorId", event.getOperatorId());
        variables.put("oldAssignedUserId", payload.getOldAssignedUserId());
        variables.put("newAssignedUserId", payload.getNewAssignedUserId());
        variables.put("assignType", payload.getAssignType());
        variables.put("operationId", payload.getOperationId());
        return variables;
    }

    private Map<String, Object> buildEvaluationVariables(NotifyEvaluationInviteEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", payload.getWorkOrderId());
        variables.put("orderNo", payload.getOrderNo());
        variables.put("customerId", payload.getCustomerId());
        variables.put("customerMobile", payload.getCustomerMobile());
        variables.put("customerOpenid", payload.getCustomerOpenid());
        variables.put("companyId", payload.getCompanyId());
        variables.put("companyName", payload.getCompanyName());
        variables.put("closedTime", payload.getClosedTime());
        return variables;
    }

    private String buildAssignedMessageExt(NotifyAssignedEventDTO payload) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("assignType", payload.getAssignType());
        ext.put("operationId", payload.getOperationId());
        ext.put("oldAssignedUserId", payload.getOldAssignedUserId());
        ext.put("newAssignedUserId", payload.getNewAssignedUserId());
        return JSONUtil.toJsonStr(ext);
    }

    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType,
                                                Long actionUserId, String remark) {
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        logEntity.setMessageId(message.getId());
        logEntity.setActionType(actionType);
        logEntity.setActionUserId(actionUserId);
        logEntity.setRemark(remark);
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }

    private void markEventFailed(Long eventId, Exception ex) {
        SysNotifyEvent current = notifyEventService.getById(eventId);
        int currentRetryCount = current == null || current.getRetryCount() == null ? 0 : current.getRetryCount();
        notifyEventService.markFailed(
                eventId,
                currentRetryCount + 1,
                LocalDateTime.now().plusMinutes(NotifyConstants.EVENT_RETRY_DELAY_MINUTES),
                buildErrorMessage(ex)
        );
    }

    private String buildErrorMessage(Exception ex) {
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            message = ex.getClass().getSimpleName();
        }
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }
}
