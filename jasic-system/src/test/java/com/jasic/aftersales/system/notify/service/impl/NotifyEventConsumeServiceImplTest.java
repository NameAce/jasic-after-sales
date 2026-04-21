package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
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
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotifyEventConsumeServiceImplTest {

    @Test
    public void shouldCreatePendingMessageForFirstAssign() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();

        SysNotifyEvent event = buildEvent(1L, 88L, 200L, 100L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 200L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(eventService, messageService, logService, new FakeNotifyTemplateService(), buildUserMapper(200L, "缁翠慨鍛楢"));

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.SUCCESS.getCode(), eventService.events.get(1L).getStatus());
        Assert.assertEquals(1, messageService.createdMessages.size());
        SysNotifyMessage message = messageService.createdMessages.get(0);
        Assert.assertEquals(Long.valueOf(1L), message.getEventId());
        Assert.assertEquals(NotifyTodoStatusEnum.PENDING.getCode(), message.getTodoStatus());
        Assert.assertEquals("缁翠慨鍛楢", message.getReceiverName());
        Assert.assertEquals(String.valueOf(event.getBizId()), message.getRouteValue());
        Assert.assertEquals(1, logService.logs.size());
        Assert.assertEquals(NotifyActionTypeEnum.CREATE.getCode(), logService.logs.get(0).getActionType());
    }

    @Test
    public void shouldInvalidateOldTodosAndCreateNewTodoOnTransfer() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();

        SysNotifyEvent event = buildEvent(2L, 89L, 201L, 101L, NotifyConstants.ASSIGN_TYPE_TRANSFER, 200L, 201L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        SysNotifyMessage oldPending = buildActiveMessage(11L, 89L, 200L, NotifyTodoStatusEnum.PENDING.getCode());
        SysNotifyMessage oldRead = buildActiveMessage(12L, 89L, 200L, NotifyTodoStatusEnum.READ.getCode());
        messageService.activeTodosByReceiver.put(200L, new ArrayList<SysNotifyMessage>() {{
            add(oldPending);
            add(oldRead);
        }});

        NotifyEventConsumeServiceImpl service = createService(eventService, messageService, logService, new FakeNotifyTemplateService(), buildUserMapper(201L, "缁翠慨鍛楤"));

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldPending.getTodoStatus());
        Assert.assertEquals(NotifyInvalidReasonEnum.TRANSFERRED.getCode(), oldPending.getInvalidReason());
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldRead.getTodoStatus());
        Assert.assertEquals(NotifyInvalidReasonEnum.TRANSFERRED.getCode(), oldRead.getInvalidReason());
        Assert.assertEquals(1, messageService.createdMessages.size());
        Assert.assertEquals(Long.valueOf(201L), messageService.createdMessages.get(0).getReceiverId());
        Assert.assertEquals(3, logService.logs.size());
        Assert.assertEquals(NotifyActionTypeEnum.INVALID.getCode(), logService.logs.get(0).getActionType());
        Assert.assertEquals(NotifyActionTypeEnum.INVALID.getCode(), logService.logs.get(1).getActionType());
        Assert.assertEquals(NotifyActionTypeEnum.CREATE.getCode(), logService.logs.get(2).getActionType());
    }

    @Test
    public void shouldNotCreateDuplicateMessageWhenEventAlreadyHasMessage() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();

        SysNotifyEvent event = buildEvent(3L, 90L, 202L, 102L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 202L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());
        messageService.messageByEventId.put(event.getId(), buildActiveMessage(21L, 90L, 202L, NotifyTodoStatusEnum.PENDING.getCode()));

        NotifyEventConsumeServiceImpl service = createService(eventService, messageService, logService, new FakeNotifyTemplateService(), buildUserMapper(202L, "缁翠慨鍛楥"));

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertTrue(messageService.createdMessages.isEmpty());
        Assert.assertTrue(logService.logs.isEmpty());
        Assert.assertEquals(NotifyEventStatusEnum.SUCCESS.getCode(), eventService.events.get(3L).getStatus());
    }

    @Test
    public void shouldSkipMessageCreationWhenTemplateDisabled() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyTemplateService templateService = new FakeNotifyTemplateService();
        templateService.notifyEnabled = false;

        SysNotifyEvent event = buildEvent(5L, 92L, 204L, 104L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 204L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(eventService, messageService, logService, templateService, buildUserMapper(204L, "测试用户"));

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertTrue(messageService.createdMessages.isEmpty());
        Assert.assertTrue(logService.logs.isEmpty());
        Assert.assertEquals(NotifyEventStatusEnum.SUCCESS.getCode(), eventService.events.get(5L).getStatus());
    }

    @Test
    public void shouldMarkEventFailedAndScheduleRetryWhenConsumptionThrows() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();

        SysNotifyEvent event = buildEvent(4L, 91L, 203L, 103L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, null);
        event.setRetryCount(2);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(eventService, messageService, logService, new FakeNotifyTemplateService(), buildUserMapper(null, null));

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(0, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.FAILED.getCode(), eventService.events.get(4L).getStatus());
        Assert.assertEquals(Integer.valueOf(3), eventService.events.get(4L).getRetryCount());
        Assert.assertNotNull(eventService.events.get(4L).getNextRetryTime());
        Assert.assertNotNull(eventService.events.get(4L).getErrorMessage());
        Assert.assertTrue(messageService.createdMessages.isEmpty());
    }

    private NotifyEventConsumeServiceImpl createService(FakeNotifyEventService eventService,
                                                        FakeNotifyMessageService messageService,
                                                        FakeNotifyMessageLogService logService,
                                                        FakeNotifyTemplateService templateService,
                                                        SysUserMapper userMapper) throws Exception {
        NotifyEventConsumeServiceImpl service = new NotifyEventConsumeServiceImpl();
        setField(service, "notifyEventService", eventService);
        setField(service, "notifyMessageService", messageService);
        setField(service, "notifyMessageLogService", logService);
        setField(service, "notifyTemplateService", templateService);
        setField(service, "sysUserMapper", userMapper);
        setField(service, "transactionTemplate", new TransactionTemplate(new NoopTransactionManager()));
        return service;
    }

    private SysNotifyEvent buildEvent(Long eventId, Long bizId, Long receiverId, Long operatorId,
                                      String assignType, Long oldAssignedUserId, Long newAssignedUserId) {
        NotifyAssignedEventDTO payload = new NotifyAssignedEventDTO();
        payload.setWorkOrderId(bizId);
        payload.setOrderNo("WO-" + bizId);
        payload.setOldAssignedUserId(oldAssignedUserId);
        payload.setNewAssignedUserId(newAssignedUserId);
        payload.setOperatorId(operatorId);
        payload.setAssignType(assignType);
        payload.setOperationId("op-" + eventId);

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode());
        event.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        event.setBizId(bizId);
        event.setBizNo("WO-" + bizId);
        event.setOperatorId(operatorId);
        event.setReceiverId(receiverId);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        event.setRetryCount(0);
        return event;
    }

    private SysNotifyMessage buildActiveMessage(Long id, Long bizId, Long receiverId, String todoStatus) {
        SysNotifyMessage message = new SysNotifyMessage();
        message.setId(id);
        message.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        message.setBizId(bizId);
        message.setBizNo("WO-" + bizId);
        message.setReceiverId(receiverId);
        message.setTodoStatus(todoStatus);
        return message;
    }

    private SysUserMapper buildUserMapper(Long userId, String realName) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    if (userId == null || realName == null) {
                        return null;
                    }
                    SysUser user = new SysUser();
                    user.setId(userId);
                    user.setRealName(realName);
                    return user;
                }
                return null;
            }
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeNotifyEventService implements NotifyEventService {
        private final Map<Long, SysNotifyEvent> events = new LinkedHashMap<>();
        private final List<Long> pendingEventIds = new ArrayList<>();

        @Override
        public Long createEvent(SysNotifyEvent notifyEvent) {
            events.put(notifyEvent.getId(), notifyEvent);
            return notifyEvent.getId();
        }

        @Override
        public SysNotifyEvent getById(Long id) {
            return events.get(id);
        }

        @Override
        public SysNotifyEvent getByEventKey(String eventKey) {
            return null;
        }

        @Override
        public List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit) {
            List<SysNotifyEvent> result = new ArrayList<>();
            for (Long eventId : pendingEventIds) {
                result.add(events.get(eventId));
            }
            return result;
        }

        @Override
        public List<SysNotifyEvent> listByQuery(com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery query) {
            return Collections.emptyList();
        }

        @Override
        public void updateStatus(Long eventId, String status) {
            events.get(eventId).setStatus(status);
        }

        @Override
        public boolean markProcessing(Long eventId) {
            events.get(eventId).setStatus(NotifyEventStatusEnum.PROCESSING.getCode());
            return true;
        }

        @Override
        public void markSuccess(Long eventId) {
            events.get(eventId).setStatus(NotifyEventStatusEnum.SUCCESS.getCode());
        }

        @Override
        public void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
            SysNotifyEvent event = events.get(eventId);
            event.setStatus(NotifyEventStatusEnum.FAILED.getCode());
            event.setRetryCount(retryCount);
            event.setNextRetryTime(nextRetryTime);
            event.setErrorMessage(errorMessage);
        }

        @Override
        public void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        }
    }

    private static class FakeNotifyMessageService implements NotifyMessageService {
        private final Map<Long, SysNotifyMessage> messageByEventId = new LinkedHashMap<>();
        private final Map<Long, List<SysNotifyMessage>> activeTodosByReceiver = new LinkedHashMap<>();
        private final List<SysNotifyMessage> createdMessages = new ArrayList<>();
        private long nextMessageId = 1000L;

        @Override
        public Long createMessage(SysNotifyMessage notifyMessage) {
            notifyMessage.setId(nextMessageId++);
            createdMessages.add(notifyMessage);
            messageByEventId.put(notifyMessage.getEventId(), notifyMessage);
            return notifyMessage.getId();
        }

        @Override
        public SysNotifyMessage getById(Long id) {
            return null;
        }

        @Override
        public SysNotifyMessage getByEventId(Long eventId) {
            return messageByEventId.get(eventId);
        }

        @Override
        public List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId) {
            return activeTodosByReceiver.getOrDefault(receiverId, Collections.emptyList());
        }

        @Override
        public boolean invalidateMessage(Long messageId, String invalidReason, LocalDateTime invalidTime) {
            for (List<SysNotifyMessage> messages : activeTodosByReceiver.values()) {
                for (SysNotifyMessage message : messages) {
                    if (messageId.equals(message.getId())) {
                        message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
                        message.setInvalidReason(invalidReason);
                        message.setInvalidTime(invalidTime);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void markRead(Long id, Long receiverId) {
        }

        @Override
        public void markReadByBiz(com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO dto) {
        }

        @Override
        public void completeTodoByBizAndReceiver(com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO dto) {
        }

        @Override
        public void invalidateTodoByBiz(com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO dto) {
        }

        @Override
        public com.jasic.aftersales.common.core.domain.PageResult<com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO> listPage(com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery query) {
            return null;
        }

        @Override
        public Long countTodo(Long receiverId) {
            return 0L;
        }
    }

    private static class FakeNotifyTemplateService implements NotifyTemplateService {
        private boolean notifyEnabled = true;

        @Override
        public com.jasic.aftersales.common.core.domain.PageResult<com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO> listPage(
                com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery query) {
            return null;
        }

        @Override
        public com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO getById(Long id) {
            return null;
        }

        @Override
        public Long saveCustom(com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO dto) {
            return null;
        }

        @Override
        public void updateCustom(com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO dto) {
        }

        @Override
        public void removeCustom(Long id) {
        }

        @Override
        public com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO preview(
                com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO dto) {
            return null;
        }

        @Override
        public NotifyTemplateRenderResult render(String templateCode, Map<String, Object> variables) {
            NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
            result.setNotifyEnabled(notifyEnabled);
            result.setTemplateCode(templateCode);
            result.setTemplateSource("BUILT_IN");
            result.setTitle("你有新的工单待处理");
            result.setSummary("工单" + variables.get("bizNo") + "已派发给你，请尽快处理");
            result.setRouteType(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL);
            result.setRouteValue(String.valueOf(variables.get("bizId")));
            return result;
        }

        @Override
        public void refreshCache() {
        }
    }

    private static class FakeNotifyMessageLogService implements NotifyMessageLogService {
        private final List<SysNotifyMessageLog> logs = new ArrayList<>();

        @Override
        public Long createLog(SysNotifyMessageLog notifyMessageLog) {
            logs.add(notifyMessageLog);
            return (long) logs.size();
        }

        @Override
        public List<SysNotifyMessageLog> listByQuery(com.jasic.aftersales.system.notify.domain.query.NotifyMessageLogQuery query) {
            return Collections.emptyList();
        }
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
