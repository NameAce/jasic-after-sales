package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.service.NotifyEventConsumeService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.mapper.SysUserMapper;
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
 * 通知事件消费 Service 实现。
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
                log.error("消费通知事件失败，eventId={}, eventKey={}", event.getId(), event.getEventKey(), ex);
                markEventFailed(event.getId(), ex);
            }
        }
        return successCount;
    }

    private void consumeSingleEvent(Long eventId) {
        SysNotifyEvent event = getRequiredProcessingEvent(eventId);
        NotifyAssignedEventDTO payload = parseAssignedPayload(event);
        if (NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())) {
            invalidateTransferredTodos(event, payload);
        }
        createPendingMessageIfAbsent(event, payload);
        notifyEventService.markSuccess(event.getId());
    }

    private SysNotifyEvent getRequiredProcessingEvent(Long eventId) {
        SysNotifyEvent event = notifyEventService.getById(eventId);
        if (event == null) {
            throw new ServiceException("通知事件不存在");
        }
        if (!NotifyEventStatusEnum.PROCESSING.getCode().equals(event.getStatus())) {
            throw new ServiceException("通知事件未处于处理中状态");
        }
        if (!NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode().equals(event.getEventType())) {
            throw new ServiceException("暂不支持的通知事件类型：" + event.getEventType());
        }
        return event;
    }

    private NotifyAssignedEventDTO parseAssignedPayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("通知事件载荷不能为空");
        }
        NotifyAssignedEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyAssignedEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("通知事件载荷解析失败");
        }
        if (payload == null) {
            throw new ServiceException("通知事件载荷解析结果为空");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("通知事件缺少工单ID");
        }
        if (payload.getNewAssignedUserId() == null) {
            throw new ServiceException("通知事件缺少新接收人");
        }
        if (StrUtil.isBlank(payload.getAssignType())) {
            throw new ServiceException("通知事件缺少派单类型");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("通知事件载荷工单ID与事件主体不一致");
        }
        if (!Objects.equals(payload.getNewAssignedUserId(), event.getReceiverId())) {
            throw new ServiceException("通知事件载荷接收人与事件接收人不一致");
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
                    "工单转派，旧接收人待办失效"
            ));
        }
    }

    private void createPendingMessageIfAbsent(SysNotifyEvent event, NotifyAssignedEventDTO payload) {
        if (notifyMessageService.getByEventId(event.getId()) != null) {
            return;
        }
        SysUser receiver = sysUserMapper.selectById(event.getReceiverId());
        SysNotifyMessage message = new SysNotifyMessage();
        message.setEventId(event.getId());
        message.setMessageType(NotifyConstants.MESSAGE_TYPE_TODO);
        message.setEventType(event.getEventType());
        message.setBizType(event.getBizType());
        message.setBizId(event.getBizId());
        message.setBizNo(event.getBizNo());
        message.setReceiverId(event.getReceiverId());
        message.setReceiverName(resolveReceiverName(receiver, event.getReceiverId()));
        message.setTitle(NotifyConstants.TODO_TITLE_ASSIGNED);
        message.setSummary(String.format("工单 %s 已派发给你，请尽快处理", event.getBizNo()));
        message.setRouteType(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL);
        message.setRouteValue(String.valueOf(event.getBizId()));
        message.setTodoStatus(NotifyTodoStatusEnum.PENDING.getCode());
        message.setExtJson(buildMessageExt(payload));
        Long messageId = notifyMessageService.createMessage(message);
        message.setId(messageId);
        notifyMessageLogService.createLog(buildMessageLog(
                message,
                NotifyActionTypeEnum.CREATE.getCode(),
                event.getOperatorId(),
                buildCreateRemark(payload)
        ));
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

    private String buildMessageExt(NotifyAssignedEventDTO payload) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("assignType", payload.getAssignType());
        ext.put("operationId", payload.getOperationId());
        ext.put("oldAssignedUserId", payload.getOldAssignedUserId());
        ext.put("newAssignedUserId", payload.getNewAssignedUserId());
        return JSONUtil.toJsonStr(ext);
    }

    private String buildCreateRemark(NotifyAssignedEventDTO payload) {
        if (NotifyConstants.ASSIGN_TYPE_TRANSFER.equals(payload.getAssignType())) {
            return "工单转派后为新接收人生成待办";
        }
        return "工单首次派单生成待办";
    }

    private SysNotifyMessageLog buildMessageLog(SysNotifyMessage message, String actionType, Long actionUserId, String remark) {
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
