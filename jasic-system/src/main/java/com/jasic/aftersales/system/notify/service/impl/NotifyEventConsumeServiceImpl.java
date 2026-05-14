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

    /**
     * 通知模板服务服务依赖。
     *
     * @return 处理结果
     */
    @Resource
    private NotifyTemplateService notifyTemplateService;

    @Resource
    private NotifyDispatchService notifyDispatchService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 处理consumePendingEvents业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    @Override
    public int consumePendingEvents() {
        // 说明：执行该步骤以保证业务流程正确。
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
                    // 调用getId方法，复用统一能力并保证业务规则一致。
                    consumeSingleEvent(event.getId());
                    return null;
                });
                successCount++;
            } catch (Exception ex) {
                // 调用getEventKey方法，复用统一能力并保证业务规则一致。
                log.error("Consume notify event failed. eventId={}, eventKey={}", event.getId(), event.getEventKey(), ex);
                // 调用getId方法，复用统一能力并保证业务规则一致。
                markEventFailed(event.getId(), ex);
            }
        }
        return successCount;
    }

    /**
     * 消费Single事件。
     *
     * @param eventId event ID
     */
    private void consumeSingleEvent(Long eventId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyEvent event = getRequiredProcessingEvent(eventId);
        if (NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode().equals(event.getEventType())) {
            // 调用consumeAssignedEvent方法，复用统一能力并保证业务规则一致。
            consumeAssignedEvent(event);
        } else if (NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode().equals(event.getEventType())) {
            // 调用consumeEvaluationInviteEvent方法，复用统一能力并保证业务规则一致。
            consumeEvaluationInviteEvent(event);
        } else {
            throw new ServiceException("Unsupported notify eventType: " + event.getEventType());
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        notifyEventService.markSuccess(event.getId());
    }

    /**
     * 消费Assigned事件。
     *
     * @param event 参数
     */
    private void consumeAssignedEvent(SysNotifyEvent event) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyAssignedEventDTO payload = parseAssignedPayload(event);
        if (NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())) {
            // 调用invalidateTransferredTodos方法，复用统一能力并保证业务规则一致。
            invalidateTransferredTodos(event, payload);
        }
        // 调用createPendingMessageIfAbsent方法，复用统一能力并保证业务规则一致。
        createPendingMessageIfAbsent(event, payload);
    }

    /**
     * 消费评价Invite事件。
     *
     * @param event 参数
     */
    private void consumeEvaluationInviteEvent(SysNotifyEvent event) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyEvaluationInviteEventDTO payload = parseEvaluationInvitePayload(event);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        String templateCode = NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE.getCode();
        // 调用isNotifyEnabled方法，复用统一能力并保证业务规则一致。
        boolean notifyEnabled = notifyTemplateService.isNotifyEnabled(templateCode);
        // 调用listChannelConfigs方法，复用统一能力并保证业务规则一致。
        List<NotifyTemplateChannelVO> channels = notifyTemplateService.listChannelConfigs(templateCode);
        if (channels.isEmpty()) {
            createEvaluationDispatch(event, payload, null, notifyEnabled,
                    NotifyDispatchStatusEnum.SKIPPED.getCode(),
                    NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(),
                    "No channel config found for template " + templateCode);
            return;
        }
        for (NotifyTemplateChannelVO channel : channels) {
            // 调用createEvaluationDispatch方法，复用统一能力并保证业务规则一致。
            createEvaluationDispatch(event, payload, channel, notifyEnabled, null, null, null);
        }
    }

    /**
     * 获取RequiredProcessing事件。
     *
     * @param eventId event ID
     * @return 处理结果
     */
    private SysNotifyEvent getRequiredProcessingEvent(Long eventId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyEvent event = notifyEventService.getById(eventId);
        if (event == null) {
            throw new ServiceException("Notify event not found");
        }
        if (!NotifyEventStatusEnum.PROCESSING.getCode().equals(event.getStatus())) {
            throw new ServiceException("Notify event is not processing");
        }
        return event;
    }

    /**
     * parseAssignedPayload。
     *
     * @param event 参数
     * @return 处理结果
     */
    private NotifyAssignedEventDTO parseAssignedPayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            // 说明：执行该步骤以保证业务流程正确。
            throw new ServiceException("Notify event payload cannot be blank");
        }
        NotifyAssignedEventDTO payload;
        try {
            // 调用getPayloadJson方法，复用统一能力并保证业务规则一致。
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
        if (payload.getReceiverCompanyId() == null) {
            throw new ServiceException("Notify assigned payload missing receiverCompanyId");
        }
        return payload;
    }

    /**
     * parse评价InvitePayload。
     *
     * @param event 参数
     * @return 处理结果
     */
    private NotifyEvaluationInviteEventDTO parseEvaluationInvitePayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            // 说明：执行该步骤以保证业务流程正确。
            throw new ServiceException("Notify event payload cannot be blank");
        }
        NotifyEvaluationInviteEventDTO payload;
        try {
            // 调用getPayloadJson方法，复用统一能力并保证业务规则一致。
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

    /**
     * 作废TransferredTodos。
     *
     * @param event 参数
     * @param payload 参数
     */
    private void invalidateTransferredTodos(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        if (payload.getOldAssignedUserId() == null
                || Objects.equals(payload.getOldAssignedUserId(), payload.getNewAssignedUserId())) {
            return;
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysNotifyMessage> messages = notifyMessageService.listActiveTodoByBizAndReceiver(
                NotifyBizTypeEnum.WORK_ORDER.getCode(),
                payload.getWorkOrderId(),
                payload.getOldAssignedUserId(),
                payload.getReceiverCompanyId()
        );
        if (messages.isEmpty()) {
            return;
        }
        // 调用now方法，复用统一能力并保证业务规则一致。
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
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            message.setInvalidReason(NotifyInvalidReasonEnum.TRANSFERRED.getCode());
            // 调用setInvalidTime方法，复用统一能力并保证业务规则一致。
            message.setInvalidTime(invalidTime);
            notifyMessageLogService.createLog(buildMessageLog(
                    message,
                    NotifyActionTypeEnum.INVALID.getCode(),
                    event.getOperatorId(),
                    "Invalidate transferred todo for previous assignee"
            ));
        }
    }

    /**
     * 创建Pending消息IfAbsent。
     *
     * @param event 参数
     * @param payload 参数
     */
    private void createPendingMessageIfAbsent(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        // 说明：执行该步骤以保证业务流程正确。
        if (notifyMessageService.getByEventId(event.getId()) != null) {
            return;
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysUser receiver = sysUserMapper.selectById(event.getReceiverId());
        // 调用getReceiverId方法，复用统一能力并保证业务规则一致。
        String receiverName = resolveReceiverName(receiver, event.getReceiverId());
        NotifyTemplateRenderResult renderResult = notifyTemplateService.render(
                NotifyTemplateCodeEnum.WORK_ORDER_ASSIGNED.getCode(),
                buildAssignedTemplateVariables(event, payload, receiverName)
        );
        if (!renderResult.isNotifyEnabled()) {
            log.info("Notify template disabled, skip todo creation. eventId={}, templateCode={}",
                    // 调用getCode方法，复用统一能力并保证业务规则一致。
                    event.getId(), NotifyTemplateCodeEnum.WORK_ORDER_ASSIGNED.getCode());
            return;
        }
        // 调用SysNotifyMessage方法，复用统一能力并保证业务规则一致。
        SysNotifyMessage message = new SysNotifyMessage();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        message.setEventId(event.getId());
        // 调用setMessageType方法，复用统一能力并保证业务规则一致。
        message.setMessageType(NotifyConstants.MESSAGE_TYPE_TODO);
        // 调用getEventType方法，复用统一能力并保证业务规则一致。
        message.setEventType(event.getEventType());
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        message.setBizType(event.getBizType());
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        message.setBizId(event.getBizId());
        // 调用getBizNo方法，复用统一能力并保证业务规则一致。
        message.setBizNo(event.getBizNo());
        // 调用getReceiverId方法，复用统一能力并保证业务规则一致。
        message.setReceiverId(event.getReceiverId());
        // 调用getReceiverCompanyId方法，复用统一能力并保证业务规则一致。
        message.setReceiverCompanyId(payload.getReceiverCompanyId());
        // 调用setReceiverName方法，复用统一能力并保证业务规则一致。
        message.setReceiverName(receiverName);
        // 调用getTitle方法，复用统一能力并保证业务规则一致。
        message.setTitle(renderResult.getTitle());
        // 调用getSummary方法，复用统一能力并保证业务规则一致。
        message.setSummary(renderResult.getSummary());
        // 调用getRouteType方法，复用统一能力并保证业务规则一致。
        message.setRouteType(renderResult.getRouteType());
        // 调用getRouteValue方法，复用统一能力并保证业务规则一致。
        message.setRouteValue(renderResult.getRouteValue());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        message.setTodoStatus(NotifyTodoStatusEnum.PENDING.getCode());
        // 调用buildAssignedMessageExt方法，复用统一能力并保证业务规则一致。
        message.setExtJson(buildAssignedMessageExt(payload));
        // 调用createMessage方法，复用统一能力并保证业务规则一致。
        Long messageId = notifyMessageService.createMessage(message);
        // 调用setId方法，复用统一能力并保证业务规则一致。
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

    /**
     * 创建评价分发。
     *
     * @param event 参数
     * @param payload 参数
     * @param channel 参数
     * @param notifyEnabled 参数
     * @param forcedStatus 参数
     * @param forcedResultCode 参数
     * @param forcedResultMessage 参数
     */
    private void createEvaluationDispatch(SysNotifyEvent event, NotifyEvaluationInviteEventDTO payload,
                                          NotifyTemplateChannelVO channel, boolean notifyEnabled,
                                          String forcedStatus, String forcedResultCode, String forcedResultMessage) {
        // 说明：执行该步骤以保证业务流程正确。
        String channelType = channel == null ? NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode() : channel.getChannelType();
        // 调用buildEvaluationDispatch方法，复用统一能力并保证业务规则一致。
        SysNotifyDispatch dispatch = buildEvaluationDispatch(event, payload, channel, channelType);
        if (forcedStatus != null) {
            // 调用setDispatchStatus方法，复用统一能力并保证业务规则一致。
            dispatch.setDispatchStatus(forcedStatus);
            // 调用setResultCode方法，复用统一能力并保证业务规则一致。
            dispatch.setResultCode(forcedResultCode);
            // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
            dispatch.setResultMessage(forcedResultMessage);
            // 调用createDispatch方法，复用统一能力并保证业务规则一致。
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        if (!notifyEnabled) {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_TEMPLATE_DISABLED.getCode());
            // 调用getTemplateCode方法，复用统一能力并保证业务规则一致。
            dispatch.setResultMessage("Template " + dispatch.getTemplateCode() + " is disabled");
            // 调用createDispatch方法，复用统一能力并保证业务规则一致。
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        if (channel == null || Objects.equals(channel.getChannelEnabled(), 0)) {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_DISABLED.getCode());
            // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
            dispatch.setResultMessage("Channel " + channelType + " is disabled");
            // 调用createDispatch方法，复用统一能力并保证业务规则一致。
            notifyDispatchService.createDispatch(dispatch);
            return;
        }
        // 调用buildChannelConfig方法，复用统一能力并保证业务规则一致。
        NotifyTemplateChannelConfig config = buildChannelConfig(channel);
        // 调用getTemplateCode方法，复用统一能力并保证业务规则一致。
        dispatch.setPayloadJson(buildEvaluationDispatchPayload(dispatch.getTemplateCode(), channelType, config, payload));
        if (StrUtil.isBlank(dispatch.getReceiverAddress())) {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode());
            // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
            dispatch.setResultMessage("Customer openid is missing");
        } else if (config == null
                || StrUtil.isBlank(config.getScene())
                || StrUtil.isBlank(config.getTemplateId())
                || StrUtil.isBlank(config.getPagePathTemplate())
                || config.getFieldMapping() == null
                || config.getFieldMapping().isEmpty()) {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.SKIPPED.getCode());
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setResultCode(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode());
            // 调用setResultMessage方法，复用统一能力并保证业务规则一致。
            dispatch.setResultMessage("Mini program channel config is incomplete");
        } else {
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            dispatch.setDispatchStatus(NotifyDispatchStatusEnum.PENDING.getCode());
        }
        // 调用createDispatch方法，复用统一能力并保证业务规则一致。
        notifyDispatchService.createDispatch(dispatch);
    }

    /**
     * 构建评价分发。
     *
     * @param event 参数
     * @param payload 参数
     * @param channel 参数
     * @param channelType 参数
     * @return 处理结果
     */
    private SysNotifyDispatch buildEvaluationDispatch(SysNotifyEvent event, NotifyEvaluationInviteEventDTO payload,
                                                      NotifyTemplateChannelVO channel, String channelType) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        dispatch.setEventId(event.getId());
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        dispatch.setTemplateCode(NotifyTemplateCodeEnum.WORK_ORDER_EVALUATION_INVITE.getCode());
        // 调用setChannelType方法，复用统一能力并保证业务规则一致。
        dispatch.setChannelType(channelType);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        dispatch.setReceiverType(NotifyReceiverTypeEnum.CUSTOMER.getCode());
        // 调用getCustomerId方法，复用统一能力并保证业务规则一致。
        dispatch.setReceiverId(payload.getCustomerId());
        // 调用getCustomerOpenid方法，复用统一能力并保证业务规则一致。
        dispatch.setReceiverAddress(StrUtil.trimToNull(payload.getCustomerOpenid()));
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        dispatch.setBizType(event.getBizType());
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        dispatch.setBizId(event.getBizId());
        // 调用getBizNo方法，复用统一能力并保证业务规则一致。
        dispatch.setBizNo(event.getBizNo());
        // 调用setRetryCount方法，复用统一能力并保证业务规则一致。
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

    /**
     * 构建渠道配置。
     *
     * @param channel 参数
     * @return 处理结果
     */
    private NotifyTemplateChannelConfig buildChannelConfig(NotifyTemplateChannelVO channel) {
        if (channel == null) {
            return null;
        }
        // 说明：执行该步骤以保证业务流程正确。
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        // 调用getChannelScene方法，复用统一能力并保证业务规则一致。
        config.setScene(channel.getChannelScene());
        // 调用getTemplateId方法，复用统一能力并保证业务规则一致。
        config.setTemplateId(channel.getTemplateId());
        // 调用getPagePathTemplate方法，复用统一能力并保证业务规则一致。
        config.setPagePathTemplate(channel.getPagePathTemplate());
        // 调用getFieldMapping方法，复用统一能力并保证业务规则一致。
        config.setFieldMapping(channel.getFieldMapping());
        return config;
    }

    /**
     * 构建评价分发Payload。
     *
     * @param templateCode 参数
     * @param channelType 参数
     * @param channelConfig 参数
     * @param payload 参数
     * @return 处理结果
     */
    private String buildEvaluationDispatchPayload(String templateCode, String channelType,
                                                  NotifyTemplateChannelConfig channelConfig,
                                                  NotifyEvaluationInviteEventDTO payload) {
        // 说明：执行该步骤以保证业务流程正确。
        NotifyDispatchPayload dispatchPayload = new NotifyDispatchPayload();
        // 调用setTemplateCode方法，复用统一能力并保证业务规则一致。
        dispatchPayload.setTemplateCode(templateCode);
        // 调用setChannelType方法，复用统一能力并保证业务规则一致。
        dispatchPayload.setChannelType(channelType);
        // 调用setChannelConfig方法，复用统一能力并保证业务规则一致。
        dispatchPayload.setChannelConfig(channelConfig);
        // 调用buildEvaluationVariables方法，复用统一能力并保证业务规则一致。
        dispatchPayload.setVariables(buildEvaluationVariables(payload));
        return JSONUtil.toJsonStr(dispatchPayload);
    }

    /**
     * 解析接收人名称。
     *
     * @param receiver 参数
     * @param receiverId receiver ID
     * @return 处理结果
     */
    private String resolveReceiverName(SysUser receiver, Long receiverId) {
        if (receiver == null) {
            return String.valueOf(receiverId);
        }
        // 调用getRealName方法，复用统一能力并保证业务规则一致。
        String realName = StrUtil.trim(receiver.getRealName());
        if (StrUtil.isNotBlank(realName)) {
            return realName;
        }
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        String username = StrUtil.trim(receiver.getUsername());
        return StrUtil.isNotBlank(username) ? username : String.valueOf(receiverId);
    }

    /**
     * 构建Assigned模板Variables。
     *
     * @param event 参数
     * @param payload 参数
     * @param receiverName 参数
     * @return 处理结果
     */
    private Map<String, Object> buildAssignedTemplateVariables(SysNotifyEvent event, NotifyAssignedEventDTO payload,
                                                               String receiverName) {
        Map<String, Object> variables = new LinkedHashMap<>();
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        variables.put("bizId", event.getBizId());
        // 调用getBizNo方法，复用统一能力并保证业务规则一致。
        variables.put("bizNo", event.getBizNo());
        // 调用getReceiverId方法，复用统一能力并保证业务规则一致。
        variables.put("receiverId", event.getReceiverId());
        // 调用put方法，复用统一能力并保证业务规则一致。
        variables.put("receiverName", receiverName);
        // 调用getOperatorId方法，复用统一能力并保证业务规则一致。
        variables.put("operatorId", event.getOperatorId());
        // 调用getOldAssignedUserId方法，复用统一能力并保证业务规则一致。
        variables.put("oldAssignedUserId", payload.getOldAssignedUserId());
        // 调用getNewAssignedUserId方法，复用统一能力并保证业务规则一致。
        variables.put("newAssignedUserId", payload.getNewAssignedUserId());
        // 调用getAssignType方法，复用统一能力并保证业务规则一致。
        variables.put("assignType", payload.getAssignType());
        // 调用getOperationId方法，复用统一能力并保证业务规则一致。
        variables.put("operationId", payload.getOperationId());
        return variables;
    }

    /**
     * 构建评价Variables。
     *
     * @param payload 参数
     * @return 处理结果
     */
    private Map<String, Object> buildEvaluationVariables(NotifyEvaluationInviteEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        // 调用getWorkOrderId方法，复用统一能力并保证业务规则一致。
        variables.put("workOrderId", payload.getWorkOrderId());
        // 调用getOrderNo方法，复用统一能力并保证业务规则一致。
        variables.put("orderNo", payload.getOrderNo());
        // 调用getCustomerId方法，复用统一能力并保证业务规则一致。
        variables.put("customerId", payload.getCustomerId());
        // 调用getCustomerMobile方法，复用统一能力并保证业务规则一致。
        variables.put("customerMobile", payload.getCustomerMobile());
        // 调用getCustomerOpenid方法，复用统一能力并保证业务规则一致。
        variables.put("customerOpenid", payload.getCustomerOpenid());
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        variables.put("companyId", payload.getCompanyId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        variables.put("companyName", payload.getCompanyName());
        // 调用getClosedTime方法，复用统一能力并保证业务规则一致。
        variables.put("closedTime", payload.getClosedTime());
        return variables;
    }

    /**
     * 构建Assigned消息Ext。
     *
     * @param payload 参数
     * @return 处理结果
     */
    private String buildAssignedMessageExt(NotifyAssignedEventDTO payload) {
        Map<String, Object> ext = new LinkedHashMap<>();
        // 调用getAssignType方法，复用统一能力并保证业务规则一致。
        ext.put("assignType", payload.getAssignType());
        // 调用getOperationId方法，复用统一能力并保证业务规则一致。
        ext.put("operationId", payload.getOperationId());
        // 调用getOldAssignedUserId方法，复用统一能力并保证业务规则一致。
        ext.put("oldAssignedUserId", payload.getOldAssignedUserId());
        // 调用getNewAssignedUserId方法，复用统一能力并保证业务规则一致。
        ext.put("newAssignedUserId", payload.getNewAssignedUserId());
        return JSONUtil.toJsonStr(ext);
    }

    /**
     * 构建消息日志。
     *
     * @param message 参数
     * @param actionType 参数
     * @param actionUserId action User ID
     * @param remark 参数
     * @return 处理结果
     */
    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType,
                                                Long actionUserId, String remark) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyMessageLog logEntity = new SysNotifyMessageLog();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        logEntity.setMessageId(message.getId());
        // 调用setActionType方法，复用统一能力并保证业务规则一致。
        logEntity.setActionType(actionType);
        // 调用setActionUserId方法，复用统一能力并保证业务规则一致。
        logEntity.setActionUserId(actionUserId);
        // 调用setRemark方法，复用统一能力并保证业务规则一致。
        logEntity.setRemark(remark);
        // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
        logEntity.setSnapshotJson(JSONUtil.toJsonStr(message));
        return logEntity;
    }

    /**
     * mark事件Failed。
     *
     * @param eventId event ID
     * @param ex 参数
     */
    private void markEventFailed(Long eventId, Exception ex) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyEvent current = notifyEventService.getById(eventId);
        // 调用getRetryCount方法，复用统一能力并保证业务规则一致。
        int currentRetryCount = current == null || current.getRetryCount() == null ? 0 : current.getRetryCount();
        notifyEventService.markFailed(
                eventId,
                currentRetryCount + 1,
                LocalDateTime.now().plusMinutes(NotifyConstants.EVENT_RETRY_DELAY_MINUTES),
                buildErrorMessage(ex)
        );
    }

    /**
     * 构建Error消息。
     *
     * @param ex 参数
     * @return 处理结果
     */
    private String buildErrorMessage(Exception ex) {
        // 调用getMessage方法，复用统一能力并保证业务规则一致。
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            // 调用getSimpleName方法，复用统一能力并保证业务规则一致。
            message = ex.getClass().getSimpleName();
        }
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }
}




