package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
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
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
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
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
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
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();

        SysNotifyEvent event = buildAssignedEvent(1L, 88L, 200L, 100L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 200L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                new FakeNotifyTemplateService(),
                dispatchService,
                buildUserMapper(200L, "维修员A")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.SUCCESS.getCode(), eventService.events.get(1L).getStatus());
        Assert.assertEquals(1, messageService.createdMessages.size());
        Assert.assertEquals(1, logService.logs.size());
        Assert.assertTrue(dispatchService.createdDispatches.isEmpty());
    }

    @Test
    public void shouldInvalidateOldTodosAndCreateNewTodoOnTransfer() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();

        SysNotifyEvent event = buildAssignedEvent(2L, 89L, 201L, 101L, NotifyConstants.ASSIGN_TYPE_TRANSFER, 200L, 201L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        SysNotifyMessage oldPending = buildActiveMessage(11L, 89L, 200L, NotifyTodoStatusEnum.PENDING.getCode());
        SysNotifyMessage oldRead = buildActiveMessage(12L, 89L, 200L, NotifyTodoStatusEnum.READ.getCode());
        messageService.activeTodosByReceiver.put(200L, new ArrayList<SysNotifyMessage>() {{
            add(oldPending);
            add(oldRead);
        }});

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                new FakeNotifyTemplateService(),
                dispatchService,
                buildUserMapper(201L, "维修员B")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldPending.getTodoStatus());
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldRead.getTodoStatus());
        Assert.assertEquals(3, logService.logs.size());
    }

    @Test
    public void shouldCreateSkippedDispatchWhenEvaluationInviteChannelConfigIsIncomplete() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateService templateService = new FakeNotifyTemplateService();
        templateService.channelConfigs.add(buildEvaluationChannel(null));

        SysNotifyEvent event = buildEvaluationEvent(3L, 90L, 9001L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.SKIPPED.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(), dispatch.getResultCode());
    }

    @Test
    public void shouldCreatePendingDispatchWhenEvaluationInviteReady() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateService templateService = new FakeNotifyTemplateService();
        templateService.channelConfigs.add(buildEvaluationChannel("wx-template-001"));

        SysNotifyEvent event = buildEvaluationEvent(4L, 91L, 9002L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.PENDING.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(), dispatch.getChannelType());
        Assert.assertTrue(dispatch.getPayloadJson().contains("pages/order/evaluate"));
    }

    @Test
    public void shouldMarkEventFailedAndScheduleRetryWhenConsumptionThrows() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateService templateService = new FakeNotifyTemplateService();

        SysNotifyEvent event = buildAssignedEvent(5L, 92L, 204L, 104L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, null);
        event.setRetryCount(2);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(0, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.FAILED.getCode(), eventService.events.get(5L).getStatus());
        Assert.assertEquals(Integer.valueOf(3), eventService.events.get(5L).getRetryCount());
    }

    private NotifyEventConsumeServiceImpl createService(FakeNotifyEventService eventService,
                                                        FakeNotifyMessageService messageService,
                                                        FakeNotifyMessageLogService logService,
                                                        FakeNotifyTemplateService templateService,
                                                        FakeNotifyDispatchService dispatchService,
                                                        SysUserMapper userMapper) throws Exception {
        NotifyEventConsumeServiceImpl service = new NotifyEventConsumeServiceImpl();
        setField(service, "notifyEventService", eventService);
        setField(service, "notifyMessageService", messageService);
        setField(service, "notifyMessageLogService", logService);
        setField(service, "notifyTemplateService", templateService);
        setField(service, "notifyDispatchService", dispatchService);
        setField(service, "sysUserMapper", userMapper);
        setField(service, "transactionTemplate", new TransactionTemplate(new NoopTransactionManager()));
        return service;
    }

    private SysNotifyEvent buildAssignedEvent(Long eventId, Long bizId, Long receiverId, Long operatorId,
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

    private SysNotifyEvent buildEvaluationEvent(Long eventId, Long bizId, Long customerId) {
        NotifyEvaluationInviteEventDTO payload = new NotifyEvaluationInviteEventDTO();
        payload.setWorkOrderId(bizId);
        payload.setOrderNo("WO-" + bizId);
        payload.setCustomerId(customerId);
        payload.setCustomerMobile("13800138000");
        payload.setCustomerOpenid("openid-" + customerId);
        payload.setCompanyId(3001L);
        payload.setCompanyName("深圳南山服务网点");
        payload.setClosedTime(LocalDateTime.of(2026, 4, 21, 18, 0, 0));

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode());
        event.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        event.setBizId(bizId);
        event.setBizNo("WO-" + bizId);
        event.setReceiverId(customerId);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        event.setRetryCount(0);
        return event;
    }

    private NotifyTemplateChannelVO buildEvaluationChannel(String templateId) {
        NotifyTemplateChannelVO channel = new NotifyTemplateChannelVO();
        channel.setTemplateCode(NotifyConstants.TEMPLATE_CODE_WORK_ORDER_EVALUATION_INVITE);
        channel.setChannelType(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode());
        channel.setChannelEnabled(1);
        channel.setChannelScene("C");
        channel.setTemplateId(templateId);
        channel.setPagePathTemplate("pages/order/evaluate?workOrderId=${workOrderId}");
        List<com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO> fieldMappings = new ArrayList<>();
        com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO mapping = new com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO();
        mapping.setField("thing1");
        mapping.setValue("${orderNo}");
        fieldMappings.add(mapping);
        channel.setFieldMapping(fieldMappings);
        return channel;
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
        Field field = target.getClass().getDeclaredField(fieldName);
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
        public com.jasic.aftersales.common.core.domain.PageResult<com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO> listPage(
                com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery query) {
            return null;
        }

        @Override
        public Long countTodo(Long receiverId) {
            return 0L;
        }
    }

    private static class FakeNotifyTemplateService implements NotifyTemplateService {
        private boolean notifyEnabled = true;
        private final List<NotifyTemplateChannelVO> channelConfigs = new ArrayList<>();

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
        public boolean isNotifyEnabled(String templateCode) {
            return notifyEnabled;
        }

        @Override
        public List<NotifyTemplateChannelVO> listChannelConfigs(String templateCode) {
            return channelConfigs;
        }

        @Override
        public void saveChannelConfigs(String templateCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        }

        @Override
        public void refreshCache() {
        }
    }

    private static class FakeNotifyDispatchService implements NotifyDispatchService {
        private final List<SysNotifyDispatch> createdDispatches = new ArrayList<>();

        @Override
        public Long createDispatch(SysNotifyDispatch dispatch) {
            if (dispatch.getId() == null) {
                dispatch.setId((long) (createdDispatches.size() + 1));
            }
            createdDispatches.add(dispatch);
            return dispatch.getId();
        }

        @Override
        public SysNotifyDispatch getById(Long id) {
            return null;
        }

        @Override
        public List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit) {
            return Collections.emptyList();
        }

        @Override
        public boolean markProcessing(Long dispatchId) {
            return false;
        }

        @Override
        public void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        }

        @Override
        public void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime, String resultCode,
                               String resultMessage, String channelResponseJson) {
        }

        @Override
        public void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        }

        @Override
        public int consumePendingDispatches() {
            return 0;
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
